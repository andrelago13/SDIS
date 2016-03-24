package backupservice.protocols.processors;

import backupservice.BackupService;
import backupservice.protocols.ProtocolHeader;
import backupservice.protocols.ProtocolInstance;

public abstract class ProtocolProcessorFactory {

	// Returns null if no processor should be launched
	public static ProtocolProcessor getProcessor(ProtocolInstance message, BackupService service) {
		
		ProtocolHeader header = message.getHeader();
		
		switch(header.getMessage_type()) {
		case PUTCHUNK:
			if(header.getSender_id() != service.getIdentifier()) {
				return new BackupPeer(message, service);
			}
			break;
		case GETCHUNK:
			// TODO getchunk peer
			break;
		case DELETE:
			// TODO delete peer
			break;
		case REMOVED:
			// TODO removed peer
			break;
		default:
			break;
		}
		
		return null;
	}
	
	// TODO factory para TCP
	
}
