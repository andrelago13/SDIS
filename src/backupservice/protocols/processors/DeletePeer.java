package backupservice.protocols.processors;

import java.util.Random;

import backupservice.BackupService;
import backupservice.protocols.ProtocolHeader;
import backupservice.protocols.ProtocolInstance;
import backupservice.protocols.Protocols;

public class DeletePeer implements ProtocolProcessor {
	
	public final static int MAX_DELAY = 400;

	private String file_hash;
	
	private BackupService service;
	
	private Boolean active = false;
	
	public DeletePeer(BackupService service, String file_hash) {
		this.service = service;
		this.file_hash = file_hash;
	}
	

	@Override
	public Boolean handle(ProtocolInstance message) {
		if(!active)
			return false;
		
		ProtocolHeader header = message.getHeader();
		
		if(header.getMessage_type() == Protocols.MessageType.DELETE) {
			if(header.getFile_id() == file_hash) {
				service.log("Contents of file " + file_hash + " already deleted by peer. Terminating.");
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
		
		
	}

	@Override
	public void terminate() {
		active = false;
		service.removeProcessor(this);
	}

}
