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
	private int sender_port = -1;
	
	private byte[] chunk_content;
	
	private BackupService service;
	
	private Boolean active = false;
	private Random random;
	private int delay;
	
	private ProtocolInstance instance = null;
	
	public RestorePeer(BackupService service, int sender, String file_hash, int chunk_no, int sender_v_major, int sender_v_minor, String sender_addr, int sender_port) {
		this.service = service;
		this.sender_id = sender;
		this.file_hash = file_hash;
		this.chunk_no = chunk_no;
		this.random = new Random();
		this.delay = 0;
		sender_version_major = sender_v_major;
		sender_version_minor = sender_v_minor;
		this.sender_address = sender_addr.substring(1, sender_addr.length());
		this.sender_port = sender_port;
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

		service.logAndShow("Restoring chunk #" + chunk_no + " of file " + file_hash + "requested by peer " + sender_id + ".");
		
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
			
			if(BackupService.lastVersionActive() && BackupService.isLastVersion(sender_version_major, sender_version_minor)) {
				ProtocolInstance instance_temp = Protocols.chunkProtocolInstance(Protocols.versionMajor(), Protocols.versionMinor(),
					service.getIdentifier(), file_hash, chunk_no, new byte[1]);
				System.out.println("" + sender_address + " " + sender_port);
				byte[] buffer = instance.toBytes();
				service.sendPrivateData(buffer, buffer.length, sender_address, BackupService.START_SOCKET_NO_PRIVATE_DATA + sender_id);
				Thread.sleep(50);
				buffer = instance_temp.toBytes();
				service.sendRestoreSocket(buffer, buffer.length);

				service.logAndShow("Restored chunk #" + chunk_no + " of file " + file_hash + "requested by peer " + sender_id + ".");
				
				terminate();
				return;
			}
		} catch (IOException e) {
			e.printStackTrace();
			service.logAndShowError("Unable to retrieve contents of chunk #" + chunk_no + " of file " + file_hash + "requested by peer " + sender_id + ". Terminating.");
			terminate();
			return;
		} catch (InterruptedException e) {
			//e.printStackTrace();
			service.logAndShowError("Enhanced restore: multicast unreachable or invalid sender address");
		}

		active = true;
		startDelayedResponse();
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
				byte[] buffer = instance.toBytes();
				service.sendRestoreSocket(buffer, buffer.length);
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
