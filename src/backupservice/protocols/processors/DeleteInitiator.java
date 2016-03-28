package backupservice.protocols.processors;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import network.MulticastSocketWrapper;
import network.SocketWrapper;
import filesystem.metadata.ChunkBackupInfo;
import filesystem.metadata.FileBackupInfo;
import filesystem.metadata.MetadataManager;
import backupservice.BackupService;
import backupservice.protocols.ProtocolHeader;
import backupservice.protocols.ProtocolInstance;
import backupservice.protocols.Protocols;

public class DeleteInitiator implements ProtocolProcessor {

	public static enum EndConditionD {
		MULTICAST_UNREACHABLE,
		CANNOT_DELETE_FILE
	}

	public static final String[] condition_codes = {"-5", "-6"};

	private BackupService service = null;
	private String filePath = null;

	private Socket responseSocket = null;
	private Boolean active = false;

	private ArrayList<ChunkRemove> removers = null;

	private class ChunkRemove extends Thread {
		private ChunkBackupInfo chunk = null;
		private String fileID = null;
		private BackupService service = null;
		private MulticastSocketWrapper outgoing_socket = null;
		private int currentAttempt = 0;

		private ArrayList<Integer> respondedPeers = null;

		public ChunkRemove(BackupService service, ChunkBackupInfo chunk, String fileID) {
			this.chunk = chunk;
			this.service = service;
			this.outgoing_socket = this.service.getBackupSocket();
			this.respondedPeers = new ArrayList<Integer>();
			this.fileID = fileID;
		}

		private void removeChunk() {
			service.logAndShow("Removing  chunk #" + chunk.getNum() + ", file " + fileID + ", attempt " + currentAttempt + ")");
			ProtocolInstance instance = Protocols.deleteProtocolInstance(Protocols.PROTOCOL_VERSION_MAJOR, Protocols.PROTOCOL_VERSION_MINOR, service.getIdentifier(), fileID);

			byte[] packet_bytes = instance.toBytes();
			try {
				outgoing_socket.send(packet_bytes, packet_bytes.length);
			} catch (IOException e1) {
				e1.printStackTrace();
				try {
					if(responseSocket != null)
						SocketWrapper.sendTCP(responseSocket, condition_codes[EndConditionD.MULTICAST_UNREACHABLE.ordinal()]);
				} catch (IOException e) {
					e.printStackTrace();
				}
				return;
			}
		}

		public void run() {
			if(!active())
				return;

			removeChunk();
			service.getTimer().schedule( 
					new java.util.TimerTask() {
						@Override
						public void run() {
							eval();
						}
					}, 
					(long) (1000 * Math.pow(2, currentAttempt))
					);
		}

		private void eval() {
			if(!active())
				return;

			if(currentAttempt == 4) {
				service.logAndShow("Ending remove chunk #" + chunk.getNum() + ", file " + fileID + ", attempt " + currentAttempt + ")");
				terminate();
			} else {
				++currentAttempt;
				run();					
			}
		}

		public Boolean interested(ProtocolInstance message) {
			if(message == null)
				return false;

			ProtocolHeader header = message.getHeader();
			if(header != null && header.getMessage_type() == Protocols.MessageType.REMOVED) {
				String file_id = header.getFile_id();
				int chunk_no = header.getChunk_no();
				if(file_id != null && file_id.equals(fileID) && chunk_no == chunk.getNum()) {
					return true;
				}
			}

			return false;
		}

		public void handle(ProtocolInstance message) {
			if(!active())
				return;

			ProtocolHeader header = message.getHeader();
			int peer_id = header.getSender_id();
			if(!respondedPeers.contains(peer_id)) {
				respondedPeers.add(peer_id);
				for(int i = 0; i < service.getMetadata().peerFilesInfo().size(); i++)
					if(service.getMetadata().peerFilesInfo().get(i).getFilePath().equals(filePath))
						for(int j = 0; j < service.getMetadata().peerFilesInfo().get(i).getChunks().size();j++)
							service.getMetadata().peerFilesInfo().get(i).getChunks().remove(j);
			}
		}
	}

	public DeleteInitiator(BackupService service, String filePath) {
		this(service, filePath, null);
	}

	public DeleteInitiator(BackupService service, String filePath, Socket response_socket) {
		this.service = service;
		this.filePath = filePath;
		this.responseSocket = response_socket;
	}	

	@Override
	public void initiate() {

		MetadataManager mg = service.getMetadata();

		for(int i = 0; i < mg.ownFilesInfo().size(); i++)
			if(mg.ownFilesInfo().get(i).getFilePath().equals(filePath))
				if(utils.Files.fileValid(filePath))
					utils.Files.removeFile(filePath);

		if(responseSocket != null)
			try {
				SocketWrapper.sendTCP(responseSocket, condition_codes[EndConditionD.CANNOT_DELETE_FILE.ordinal()]);
			} catch (IOException e) {
				e.printStackTrace();
			}

		ArrayList<FileBackupInfo> peerFiles = mg.peerFilesInfo();
		for(int i = 0; i < peerFiles.size(); i++)
			if(peerFiles.get(i).getFilePath().equals(filePath))
				removers.add(new ChunkRemove(service, peerFiles.get(i).getChunks().get(i), peerFiles.get(i).getFilePath()));

		active = true;

		for(int i = 0; i < removers.size(); ++i) {
			removers.get(i).start();
		}
	}

	@Override
	public Boolean handle(ProtocolInstance message) {
		if(!active)
			return false;

		for(int i = 0; i < removers.size(); ++i) {
			if(removers.get(i).interested(message)) {
				final ChunkRemove sender = removers.get(i);
				new Thread( new Runnable() {
					@Override
					public void run() {
						sender.handle(message);
					}
				}).start();
				return true;
			}
		}
		return false;
	}

	public Boolean active() {
		return active;
	}

	public void terminate() {
		active = false;
		service.removeProcessor(this);
	}
}
