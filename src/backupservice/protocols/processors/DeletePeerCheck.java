package backupservice.protocols.processors;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;

import filesystem.metadata.MetadataManager;
import backupservice.BackupService;
import backupservice.protocols.ProtocolHeader;
import backupservice.protocols.ProtocolInstance;
import backupservice.protocols.Protocols;

public class DeletePeerCheck implements ProtocolProcessor {

	public final static int MAX_DELAY = 400;

	private boolean active = false;
	private BackupService service = null;

	private Socket responseSocket = null;
	private String hash = null;

	MetadataManager mg;

	private Random random;
	private int delay;

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
			startDelayedResponse();			

		terminate();
	}

	private void generateDelay() {
		delay = random.nextInt(MAX_DELAY);
	}

	private void startDelayedResponse() {
		generateDelay();

		service.getTimer().schedule( 
				new java.util.TimerTask() {
					@Override
					public void run() {
						eval();
					}
				}, 
				delay
				);
	}

	public void eval() {
		if(!active)
			return;

		try {
			notifyDeleteCheck(hash);
		} catch (IOException e) {
			service.logAndShow("Unable to notify peers with WASDELETED message!");
		}	
	}

	public void notifyDeleteCheck(String Hash) throws IOException
	{
		ProtocolInstance instance = Protocols.wasdeletedProtocolInstance(Protocols.versionMajor(), Protocols.versionMinor(),
				service.getIdentifier(), Hash);
		byte[] buffer = instance.toBytes();
		service.sendControlSocket(buffer, buffer.length);
	}

	@Override
	public void terminate() {
		active = false;
		service.removeProcessor(this);	
	}
	@Override
	public Boolean handle(ProtocolInstance message) {
		if(!active)
			return false;

		ProtocolHeader header = message.getHeader();

		if(header.getMessage_type() == Protocols.MessageType.WASDELETED) {
			if(header.getFile_id() == hash) {
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

}
