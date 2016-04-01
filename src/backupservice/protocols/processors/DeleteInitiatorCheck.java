package backupservice.protocols.processors;

import java.net.Socket;
import java.util.ArrayList;

import filesystem.metadata.MetadataManager;
import backupservice.BackupService;
import backupservice.protocols.ProtocolInstance;

public class DeleteInitiatorCheck implements ProtocolProcessor {

	private boolean active = false;
	private BackupService service = null;
	
	private Socket responseSocket = null;
	
	MetadataManager mg;
	
	public DeleteInitiatorCheck(BackupService service) {
		this(service, null);
	}
	
	public DeleteInitiatorCheck(BackupService service, Socket response_socket) {
		this.service = service;
		this.responseSocket = response_socket;
	}

	@Override
	public void initiate() {
		
		service.logAndShow("Starting DeleteInitiatorCheck...");
		
		mg = service.getMetadata();
		ArrayList<String> deletedFiles = mg.getDeletedPeerFiles();
		
		service.logAndShow("Prepare to send message to each file...");
	}
	
	public void checkFileHash(ArrayList<String> deleted_files)
	{	}
	
	@Override
	public Boolean active() {
		return active;
	}

	@Override
	public void terminate() {
		active = false;
		service.removeProcessor(this);		
	}

	@Override
	public Boolean handle(ProtocolInstance message) {
		return false;
	}

}
