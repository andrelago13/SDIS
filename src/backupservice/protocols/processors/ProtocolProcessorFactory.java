package backupservice.protocols.processors;

import java.net.Socket;

import backupservice.BackupService;
import backupservice.cli.CLIProtocolInstance;
import backupservice.protocols.ProtocolHeader;
import backupservice.protocols.ProtocolInstance;

public abstract class ProtocolProcessorFactory {

	// Returns null if no processor should be launched
	public static ProtocolProcessor getProcessor(ProtocolInstance message, BackupService service, String sender_address, int sender_port) {
		
		ProtocolHeader header = message.getHeader();
		 
		switch(header.getMessage_type()) {
		case PUTCHUNK:
			if(header.getSender_id() != service.getIdentifier()) {
				return new BackupPeer(message, service);
			}
			break;
		case GETCHUNK:
			if(header.getSender_id() != service.getIdentifier()) {
				return new RestorePeer(service, header.getSender_id(), header.getFile_id(), header.getChunk_no(), header.getVersion_major(), header.getVersion_minor(), sender_address, sender_port);
			}
			break;
		case DELETE:
			if(header.getSender_id() != service.getIdentifier()) {
				return new DeletePeer(service, header.getFile_id());
			}
			break;
		case REMOVED:
			if(header.getSender_id() != service.getIdentifier()) {
				return new ReclaimPeer(service, header.getFile_id(), header.getChunk_no(), header.getVersion_major(), header.getVersion_minor());
			}
			break;
		case EXISTS:
			if(header.getSender_id() != service.getIdentifier() && BackupService.lastVersionActive()) {
				return new DeletePeerCheck(service, header.getFile_id());
			}
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
			return new DeleteInitiator(service, tcp_message.filePath(), response_socket);
		case RECLAIM:
			return new ReclaimInitiator(service, tcp_message.maxDiskSpace(), response_socket);
		}
		
		return null;
	}
	
}
