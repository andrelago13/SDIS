package backupservice.protocols.processors;

import backupservice.BackupService;
import backupservice.protocols.ProtocolInstance;

public class BackupInitiator implements ProtocolProcessor {
	
	private BackupService service = null;
	private String file_path = null;
	private int replication_deg = -1;
	
	private Boolean active = false;

	public BackupInitiator(BackupService service, String file_path, int replication_deg) {
		this.service = service;
		this.file_path = file_path;
		this.replication_deg = replication_deg;
	}
	
	@Override
	public Boolean handle(ProtocolInstance message) {
		// TODO ver se mensagem é para nós e tratar
		return null;
	}
	
	public void initiate() {
		// TODO split file into chunks
		// TODO send chunks and store actual replication deg
		// TODO repetir até atingir replicação pretendida
		// TODO guardar quem respondeu
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
