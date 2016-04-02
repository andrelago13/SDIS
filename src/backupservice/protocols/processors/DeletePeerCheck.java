package backupservice.protocols.processors;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import filesystem.metadata.MetadataManager;
import backupservice.BackupService;
import backupservice.protocols.ProtocolHeader;
import backupservice.protocols.ProtocolInstance;
import backupservice.protocols.Protocols;

public class DeletePeerCheck implements ProtocolProcessor {

	private boolean active = false;
	private BackupService service = null;

	private Socket responseSocket = null;
	private String hash = null;

	MetadataManager mg;

	public DeletePeerCheck(BackupService service, String hash) {
		this(service, hash, null);
	}

	public DeletePeerCheck(BackupService service, String hash, Socket response_socket) {
		this.service = service;
		this.hash = hash;
		this.responseSocket = response_socket;
	}

	@Override
	public void initiate() {

		service.logAndShow("Starting DeletePeerCheck...");

		mg = service.getMetadata();
		ArrayList<String> deletedFiles = mg.getDeletedPeerFiles();

		if(deletedFiles.contains(hash))
		{
			try {
				notifyDeleteCheck(hash);
			} catch (IOException e) {
				service.logAndShow("Unable to notify peers with WASDELETED message!");
			}
		}
		terminate();
	}

	public void notifyDeleteCheck(String Hash) throws IOException
	{
		ProtocolInstance instance = Protocols.wasdeletedProtocolInstance(Protocols.versionMajor(), Protocols.versionMinor(),
				service.getIdentifier(), Hash);
		service.sendControlSocket(instance.toString());
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

	@Override
	public Boolean active() {
		return active;
	}

}
