package backupservice.protocols.processors;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import network.MulticastSocketWrapper;
import network.SocketWrapper;
import filesystem.FileChunk;
import filesystem.FileManager;
import filesystem.SplitFile;
import backupservice.BackupService;
import backupservice.protocols.ProtocolInstance;
import backupservice.protocols.Protocols;

public class BackupInitiator implements ProtocolProcessor {
	
	private BackupService service = null;
	private String file_path = null;
	private int replication_deg = -1;
	
	private Boolean active = false;
	
	private Socket response_socket = null;
	
	private ArrayList<ChunkSender> senders = null;
	
	private class ChunkSender extends Thread {
		private SplitFile split_file = null;
		private FileChunk chunk = null;
		private int replication_deg = -1;
		private BackupService service = null;
		private MulticastSocketWrapper outgoing_socket = null;
		
		public ChunkSender(SplitFile file, BackupService service, FileChunk chunk, int replication_deg) {
			this.split_file = file;
			this.chunk = chunk;
			this.replication_deg = replication_deg;
			this.service = service;
			this.outgoing_socket = this.service.getBackupSocket();
		}
		
		private void sendChunk() {
			System.out.println("Backing up chunk #" + chunk.getchunkNum() + " from file " + split_file.getFileId() + " (replcation degree " + this.replication_deg + ")");
			ProtocolInstance instance = Protocols.chunkProtocolInstance(Protocols.PROTOCOL_VERSION_MAJOR, Protocols.PROTOCOL_VERSION_MINOR, 
					service.getIdentifier(), split_file.getFileId(), chunk.getchunkNum(), chunk.getchunkContent());
			
			byte[] packet_bytes = instance.toBytes();
			try {
				outgoing_socket.send(packet_bytes, packet_bytes.length);
			} catch (IOException e1) {
				e1.printStackTrace();
				try {
					if(response_socket != null)
						SocketWrapper.sendTCP(response_socket, "2");
				} catch (IOException e) {
					e.printStackTrace();
				}
				return;
			}
		}
		
		public void run() {
			sendChunk();
		}
		
		public void handle(ProtocolInstance message) {
			// TODO store actual replication deg
			// TODO repetir até atingir replicação pretendida
			// TODO guardar quem respondeu
			// TODO registar tudo em metadata
		}
		
		public Boolean interested(ProtocolInstance message) {
			// TODO true se mensagem for do chunk correspondente
			return false;
		}
		
		public FileChunk chunk() {
			return chunk;
		}
		
		public int chunkNum() {
			return chunk.getchunkNum();
		}
	}

	public BackupInitiator(BackupService service, String file_path, int replication_deg) {
		this(service, file_path, replication_deg, null);
	}

	public BackupInitiator(BackupService service, String file_path, int replication_deg, Socket response_socket) {
		this.service = service;
		this.file_path = file_path;
		this.replication_deg = replication_deg;
		this.response_socket = response_socket;
		this.senders = new ArrayList<ChunkSender>();
	}
	
	@Override
	public Boolean handle(ProtocolInstance message) {
		if(!active)
			return false;
		
		for(int i = 0; i < senders.size(); ++i) {
			if(senders.get(i).interested(message)) {
				final ChunkSender sender = senders.get(i);
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
	
	public void initiate() {

		SplitFile split_file = null;
		
		try {
			try {
				split_file = FileManager.splitFile(file_path, service.getIdentifier() ,replication_deg, Protocols.MAX_PACKET_LENGTH);
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
			try {
				if(response_socket != null)
					SocketWrapper.sendTCP(response_socket, "1");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			terminate();
			return;
		}
		
		ArrayList<FileChunk> chunks = split_file.getChunkList();
		for(int i = 0; i < chunks.size(); ++i) {
			senders.add(new ChunkSender(split_file, service, chunks.get(i), replication_deg));
		}
		
		for(int i = 0; i < senders.size(); ++i) {
			senders.get(i).start();
		}
		
		active = true;
	}
	
	public void terminate() {
		active = false;
		// TODO remove itself from backupservice
	}

	@Override
	public Boolean active() {
		return active;
	}

}
