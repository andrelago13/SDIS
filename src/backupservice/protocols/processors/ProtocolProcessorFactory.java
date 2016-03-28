package backupservice.protocols.processors;

import java.net.Socket;

import backupservice.BackupService;
import backupservice.cli.CLIProtocolInstance;
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
	
	public static ProtocolProcessor getProcessor(CLIProtocolInstance tcp_message, BackupService service) {
		return getProcessor(tcp_message, service, null);
	}
	
	public static ProtocolProcessor getProcessor(CLIProtocolInstance tcp_message, BackupService service, Socket response_socket) {
		
		switch(tcp_message.type()) {
		case BACKUP:
			return new BackupInitiator(service, tcp_message.filePath(), tcp_message.replicationDegree(), response_socket);
		case RESTORE:
			return new RestoreInitiator(service, tcp_message.filePath(), response_socket);
		case DELETE:
			// TODO delete initiator
			break;
		case RECLAIM:
			// TODO reclaim initiator
			break;
		}
		
		return null;
	}
	
}
