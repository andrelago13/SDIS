package backupservice.protocols.processors;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import network.SocketWrapper;
import filesystem.metadata.ChunkBackupInfo;
import filesystem.metadata.FileBackupInfo;
import filesystem.metadata.MetadataManager;
import backupservice.BackupService;
import backupservice.protocols.ProtocolInstance;
import backupservice.protocols.Protocols;

public class ReclaimInitiator implements ProtocolProcessor {
	
	public static enum EndCondition {
		SUCCESS,
		MULTICAST_UNREACHABLE,
		UNABLE_TO_DELETE_CHUNK,
		NO_CHUNK_FOUND_TO_REMOVE
	}
	
	private BackupService service;
	private int max_bytes;
	private int bytes_reclaimed = 0;
	
	private Boolean active = false;
	
	private Socket response_socket;
	
	private MetadataManager metadata;
	
	public ReclaimInitiator(BackupService service, int max_bytes) {
		this(service, max_bytes, null);
	}
	
	public ReclaimInitiator(BackupService service, int max_bytes, Socket response_socket) {
		this.service = service;
		this.max_bytes = max_bytes;
		this.response_socket = response_socket;
		this.metadata = service.getMetadata();
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
		service.logAndShow("Initiating RECLAIM to ensure max of " + max_bytes + " Bytes.");
		active = true;
		Boolean successful = true;
		
		while(metadata.getBackupSize() > max_bytes) {
			successful = false;
			ChunkBackupInfo chunk = metadata.bestChunkToRemove();
			if(chunk == null) {
				service.logAndShowError("Unable to find chunk to remove at RECLAIM protocol.");
				if(response_socket != null) {
					try {
						SocketWrapper.sendTCP(response_socket, "" + EndCondition.NO_CHUNK_FOUND_TO_REMOVE.ordinal());
					} catch (IOException e1) {
						service.logAndShowError("Unable to notify TCP command caller.");
					}
				}
				break;
			}

			try {
				notifyRemoval(chunk);
			} catch (Exception e) {
				e.printStackTrace();
				service.logAndShowError("Unable to notify peers of chunk space RECLAIM.");
				if(response_socket != null) {
					try {
						SocketWrapper.sendTCP(response_socket, "" + EndCondition.MULTICAST_UNREACHABLE.ordinal());
					} catch (IOException e1) {
						service.logAndShowError("Unable to notify TCP command caller.");
					}
				}
				break;
			}
			try {
				removeChunk(chunk);
			} catch (Exception e) {
				e.printStackTrace();
				service.logAndShowError("Unable to remove chunk for space RECLAIM.");
				if(response_socket != null) {
					try {
						SocketWrapper.sendTCP(response_socket, "" + EndCondition.UNABLE_TO_DELETE_CHUNK.ordinal());
					} catch (IOException e1) {
						service.logAndShowError("Unable to notify TCP command caller.");
					}
				}
				break;
			}
				
				service.logAndShow("Removed chunk #" + chunk.getNum() + " from file " + chunk.getFileHash());
				successful = true;
		}
		
		if(successful) {
			service.logAndShow("Terminating RECLAIM. Reclaimed " + bytes_reclaimed + " bytes. Current total: " + metadata.getBackupSize() + " Bytes.");
			if(response_socket != null) {
				try {
					SocketWrapper.sendTCP(response_socket, "" + EndCondition.SUCCESS.ordinal());
				} catch (IOException e1) {
					service.logAndShowError("Unable to notify TCP command caller.");
				}
			}
		} else {
			service.logAndShow("Terminating unsuccessful RECLAIM. Reclaimed " + bytes_reclaimed + " bytes. Current total: " + metadata.getBackupSize() + "/" + max_bytes + " Bytes.");
		}
		terminate();
	}
	
	public void notifyRemoval(ChunkBackupInfo chunk) throws IOException {
		ProtocolInstance instance = Protocols.removedProtocolInstance(Protocols.PROTOCOL_VERSION_MAJOR, Protocols.PROTOCOL_VERSION_MINOR,
				service.getIdentifier(), chunk.getFileHash(), chunk.getNum());
		service.getControlSocket().send(instance.toString());
	}
	
	public void removeChunk(ChunkBackupInfo chunk) {
		metadata.removeChunk(chunk);
		String path = BackupService.BACKUP_FILE_PATH + service.getIdentifier() + "/" + chunk.getFileHash() + "_" + chunk.getNum();
		File file = new File(path);
		file.delete();
	}

	@Override
	public void terminate() {
		active = false;
		service.removeProcessor(this);
	}

}
