package backupservice.protocols.processors;

import java.util.Random;
import java.util.TimerTask;

import filesystem.metadata.ChunkBackupInfo;
import backupservice.BackupService;
import backupservice.protocols.ProtocolHeader;
import backupservice.protocols.ProtocolInstance;
import backupservice.protocols.Protocols;

public class ReclaimPeer implements ProtocolProcessor {
	
	public static final int MAX_DELAY = 400;
	
	private String file_hash;
	private int chunk_num;
	
	private BackupService service;
	
	private Boolean backup_initiated = false;
	private Boolean active = false;
	private Random random = new Random();
	private int delay = -1;
	private int chunk_replication = 0;
	
	public ReclaimPeer(BackupService service, String file_hash, int chunk_num) {
		this.service = service;
		this.file_hash = file_hash;
		this.chunk_num = chunk_num;
	}

	@Override
	public Boolean handle(ProtocolInstance message) {
		
		ProtocolHeader header = message.getHeader();
		
		if(header.getMessage_type() == Protocols.MessageType.PUTCHUNK) {
			if(header.getFile_id().equals(file_hash) && header.getChunk_no() == chunk_num && header.getSender_id() != service.getIdentifier()) {
				backup_initiated = true;
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
		active = true;
		service.logAndShow("Notified removal of chunk #" + chunk_num + " of file " + file_hash + " for RECLAIM protocol. Updating metadata.");
		
		if(service.getMetadata().decreasePeerChunkReplication(file_hash, chunk_num)) {
			// successful
			service.logAndShow("Actual replication of chunk #" + chunk_num + " of file " + file_hash + " decreased.");
		} else {
			// unsuccessful
			service.logAndShow("Unable to decrease actual replication of chunk #" + chunk_num + " of file " + file_hash + ". File is not present in the system.");
		}
		
		ChunkBackupInfo chunk = service.getMetadata().peerChunkBackupInfo(file_hash, chunk_num);
		
		if(chunk != null && chunk.getActualReplication() < chunk.getMinReplication()) {
			delay = random.nextInt(MAX_DELAY);
			chunk_replication = chunk.getMinReplication();
			
			service.getTimer().schedule(new TimerTask() {
				@Override
				public void run() {
					eval();
				}
			}, delay);
		} else {
			terminate();
		}
		
	}
	
	public void eval() {
		if(backup_initiated) {
			terminate();
			return;
		}

		service.logAndShow("Actual replication of chunk #" + chunk_num + " of file " + file_hash + " below minimmum. Starting Backup protocol");
		
		BackupInitiatorChunk init = new BackupInitiatorChunk(service, file_hash, chunk_num, chunk_replication);
		service.addProcessor(init);
		init.initiate();
		terminate();
	}

	@Override
	public void terminate() {
		active = false;
		service.removeProcessor(this);
	}

}
