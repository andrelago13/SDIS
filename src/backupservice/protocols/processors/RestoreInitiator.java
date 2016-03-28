package backupservice.protocols.processors;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import network.SocketWrapper;
import filesystem.FileChunk;
import filesystem.metadata.FileBackupInfo;
import backupservice.BackupService;
import backupservice.protocols.ProtocolHeader;
import backupservice.protocols.ProtocolInstance;
import backupservice.protocols.Protocols;

public class RestoreInitiator implements ProtocolProcessor {

	public static enum EndCondition {
		SUCCESS,
		FILE_DATA_NOT_FOUND
	}
	
	private BackupService service;
	private String file_path;
	
	private Socket response_socket;
	
	private Boolean active = false;
	
	private ArrayList<ChunkRestorer> restorers;
	private ArrayList<FileChunk> received_chunks;
	
	private class ChunkRestorer extends Thread {
		
		private String file_hash;
		private int chunk_no;
		
		private Boolean interested(ProtocolInstance message) {
			if(message == null)
				return false;
			
			ProtocolHeader header = message.getHeader();
			if(header.getMessage_type() == Protocols.MessageType.CHUNK) {
				if(header.getFile_id().equals(file_hash) && header.getChunk_no() == chunk_no) {
					return true;
				} else {
					return false;
				}
			}
			
			return false;
		}
		
		private void handle(ProtocolInstance message) {
			// TODO if chunk already added return
			// TODO else add chunk to restoreinitiator
			// TODO notify chunk reception
		}
	}
	
	public RestoreInitiator(BackupService service, String file_path) {
		this(service, file_path, null);
	}
	
	public RestoreInitiator(BackupService service, String file_path, Socket response_socket) {
		this.service = service;
		this.file_path = file_path;
		this.restorers = new ArrayList<ChunkRestorer>();
		this.response_socket = response_socket;
		this.received_chunks = new ArrayList<FileChunk>();
	}

	@Override
	public Boolean handle(ProtocolInstance message) {
		if(!active())
			return false;
		
		for(int i = 0; i < restorers.size(); ++i) {
			if(restorers.get(i).interested(message)) {
				restorers.get(i).handle(message);
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
		FileBackupInfo file_info = service.getMetadata().ownFileBackupInfo_path(file_path);
		if(file_info == null) {
			service.logAndShowError("Unable to retrieve data of file to RESTORE. Has the file been backed up earlier?");
			if(response_socket != null) {
				try {
					SocketWrapper.sendTCP(response_socket, "" + EndCondition.FILE_DATA_NOT_FOUND.ordinal());
				} catch (IOException e) {
					e.printStackTrace();
					service.logAndShowError("Unable to notify initiator of RESTORE protocol");
				}
			}
			terminate();
		}
		// TODO check metadata for number of chunks
		// TODO create restorer for each chunk
		// TODO initiate restorers
		active = true;
	}

	@Override
	public void terminate() {
		active = false;
		
		for(int i = 0; i < restorers.size(); ++i) {
			try {
				restorers.get(i).interrupt();
			} catch (Exception e) {
				// do nothing
			}
		}
		
		service.removeProcessor(this);
	}

}
