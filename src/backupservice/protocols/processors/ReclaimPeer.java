package backupservice.protocols.processors;

import backupservice.BackupService;
import backupservice.protocols.ProtocolInstance;

public class ReclaimPeer implements ProtocolProcessor {
	
	private String file_hash;
	private int chunk_num;
	
	private BackupService service;
	
	private Boolean active = false;
	
	public ReclaimPeer(BackupService service, String file_hash, int chunk_num) {
		this.service = service;
		this.file_hash = file_hash;
		this.chunk_num = chunk_num;
	}

	@Override
	public Boolean handle(ProtocolInstance message) {
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
		
		terminate();
	}

	@Override
	public void terminate() {
		active = false;
		service.removeProcessor(this);
	}

}
