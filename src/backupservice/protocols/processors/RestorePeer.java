package backupservice.protocols.processors;

import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

import network.SocketWrapper;
import filesystem.metadata.ChunkBackupInfo;
import backupservice.BackupService;
import backupservice.protocols.ProtocolHeader;
import backupservice.protocols.ProtocolInstance;
import backupservice.protocols.Protocols;

public class RestorePeer implements ProtocolProcessor {
	
	public final static int MAX_DELAY = 400;
	
	private int sender_id;
	private String file_hash;
	private int chunk_no;
	private int sender_version_major = Protocols.PROTOCOL_VERSION_MAJOR;
	private int sender_version_minor = Protocols.PROTOCOL_VERSION_MINOR;
	private String sender_address = null;
	
	private byte[] chunk_content;
	
	private BackupService service;
	
	private Boolean active = false;
	private Random random;
	private int delay;
	
	private ProtocolInstance instance = null;
	
	public RestorePeer(BackupService service, int sender, String file_hash, int chunk_no, int sender_v_major, int sender_v_minor, String sender_addr) {
		this.service = service;
		this.sender_id = sender;
		this.file_hash = file_hash;
		this.chunk_no = chunk_no;
		this.random = new Random();
		this.delay = 0;
		sender_version_major = sender_v_major;
		sender_version_minor = sender_v_minor;
		this.sender_address = sender_addr;
	}

	@Override
	public Boolean handle(ProtocolInstance message) {
		if(!active)
			return false;
		
		ProtocolHeader header = message.getHeader();
		
		if(header.getMessage_type() == Protocols.MessageType.CHUNK) {
			if(header.getFile_id() == file_hash && header.getChunk_no() == chunk_no) {
				service.log("Contents of chunk #" + chunk_no + " of file " + file_hash + "requested by peer " + sender_id + " already sent by another peer. Terminating.");
				terminate();
				return true;
			}
		}

		return false;
	}

	@Override
	public Boolean active() {
		return active;
	}

	@Override
	public void initiate() {
		
		// TODO enhancement if sender is 2.3, send to private socket immediately
		
		ChunkBackupInfo chunk = service.getMetadata().peerChunkBackupInfo(file_hash, chunk_no);
		if(chunk == null) {
			service.logAndShow("Chunk #" + chunk_no + " of file " + file_hash + "requested by peer " + sender_id + " is not present in this peer. Terminating.");
			terminate();
		}
		
		String path = BackupService.BACKUP_FILE_PATH + service.getIdentifier() + "/" + file_hash + "_" + chunk_no;

		try {
			Path path_obj = Paths.get(path);
			chunk_content = Files.readAllBytes(path_obj);

			instance = Protocols.chunkProtocolInstance(Protocols.versionMajor(), Protocols.versionMinor(),
					service.getIdentifier(), file_hash, chunk_no, chunk_content);
		} catch (IOException e) {
			e.printStackTrace();
			service.logAndShowError("Unable to retrieve contents of chunk #" + chunk_no + " of file " + file_hash + "requested by peer " + sender_id + ". Terminating.");
			terminate();
			return;
		}

		active = true;
		if(BackupService.lastVersionActive() && sender_address != null) {
			try {
				Socket socket = new Socket(sender_address.substring(1, sender_address.length()), BackupService.START_SOCKET_NO_PRIVATE_DATA + sender_id);
				SocketWrapper.sendTCP(socket, instance.toBytes());
				socket.close();
				instance = Protocols.chunkProtocolInstance(Protocols.versionMajor(), Protocols.versionMinor(),
						service.getIdentifier(), file_hash, chunk_no, new byte[0]);
				sendChunk();
				terminate();
			} catch (IOException e) {
				service.logAndShowError("Unable to send RESTORE chunk via TCP. Continuing with UDP.");
				startDelayedResponse();	
			}
			// TODO ligar ao TCP
		} else {
			startDelayedResponse();			
		}
	}
	
	private void generateDelay() {
		delay = random.nextInt(MAX_DELAY);
	}

	private void startDelayedResponse() {
		generateDelay();

		service.getTimer().schedule( 
				new java.util.TimerTask() {
					@Override
					public void run() {
						eval();
					}
				}, 
				delay
			);
	}
	
	public void eval() {
		if(!active)
			return;
		
		sendChunk();
	}

	private void sendChunk() {
		if(instance != null) {
			try {
				service.sendRestoreSocket(instance.toString());
			} catch (IOException e) {
				e.printStackTrace();
				service.logAndShowError("Unable to send CHUNK instance of chunk #" + chunk_no + " of file " + file_hash + "requested by peer " + sender_id + ". Terminating.");
				terminate();
			}
		}
	}

	@Override
	public void terminate() {
		active = false;
		service.removeProcessor(this);
	}

}
