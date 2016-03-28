package backupservice.protocols.processors;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

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
	
	private byte[] chunk_content;
	
	private BackupService service;
	
	private Boolean active = false;
	private Random random;
	private int delay;
	
	private ProtocolInstance instance = null;
	
	public RestorePeer(BackupService service, int sender, String file_hash, int chunk_no) {
		this.service = service;
		this.sender_id = sender;
		this.file_hash = file_hash;
		this.chunk_no = chunk_no;
		this.random = new Random();
		this.delay = 0;
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
			}
		}

		return null;
	}

	@Override
	public Boolean active() {
		return active;
	}

	@Override
	public void initiate() {
		ChunkBackupInfo chunk = service.getMetadata().peerChunkBackupInfo(file_hash, chunk_no);
		if(chunk == null) {
			service.logAndShow("Chunk #" + chunk_no + " of file " + file_hash + "requested by peer " + sender_id + " is not present in this peer. Terminating.");
			terminate();
		}
		
		String path = BackupService.BACKUP_FILE_PATH + service.getIdentifier() + "/" + file_hash + "_" + chunk_no;

		try {
			Path path_obj = Paths.get(path);
			chunk_content = Files.readAllBytes(path_obj);
			
			instance = Protocols.chunkProtocolInstance(Protocols.PROTOCOL_VERSION_MAJOR, Protocols.PROTOCOL_VERSION_MINOR,
					sender_id, file_hash, chunk_no, chunk_content);
		} catch (IOException e) {
			e.printStackTrace();
			service.logAndShowError("Unable to retrieve contents of chunk #" + chunk_no + " of file " + file_hash + "requested by peer " + sender_id + ". Terminating.");
			terminate();
			return;
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
				service.getRestoreSocket().send(instance.toString());
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
