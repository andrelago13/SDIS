package backupservice.protocols.processors;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;

import network.MulticastSocketWrapper;
import network.SocketWrapper;
import filesystem.metadata.ChunkBackupInfo;
import filesystem.metadata.FileBackupInfo;
import filesystem.metadata.MetadataManager;
import backupservice.BackupService;
import backupservice.protocols.ProtocolHeader;
import backupservice.protocols.ProtocolInstance;
import backupservice.protocols.Protocols;
import backupservice.protocols.processors.BackupInitiator.EndCondition;

public class DeleteInitiator implements ProtocolProcessor {

	public static enum EndCondition {
		MULTICAST_UNREACHABLE,
		CANNOT_DELETE_FILE,
		FILE_WAS_NOT_BACKED_UP
	}

	private BackupService service = null;
	private String filePath = null;

	private Socket responseSocket = null;
	private Boolean active = false;
	
	private int currentAttempt = 0;
	
	public DeleteInitiator(BackupService service, String filePath) {
		this(service, filePath, null);
	}

	public DeleteInitiator(BackupService service, String filePath, Socket response_socket) {
		this.service = service;
		this.filePath = filePath;
		this.responseSocket = response_socket;
	}

	@Override
	public void initiate() {
		
		// TODO ver se ficheiro foi backed up (ver metadata) ok
		// TODO		se não estiver, concluir com mensagem apropriada ok
		// TODO		se estiver, apagar o ficheiro, apagas a metadata e envias DELETE (3x, 2s) ?
		// TODO		terminas ok

		service.logAndShow("Starting DeleteInitiator...");
		
		MetadataManager mg = service.getMetadata();
		
		deleteFile(mg, filePath);
		
		try {
			notifyDelete(mg);
		} catch (IOException e1) {
			e1.printStackTrace();
			
			if(responseSocket != null) {
				try {
					SocketWrapper.sendTCP(responseSocket, "" + EndCondition.FILE_WAS_NOT_BACKED_UP.ordinal());
				} catch (IOException e) {
					e.printStackTrace();
					service.logAndShowError("Unable to notify initiator of DELETE protocol");
				}
			}
		}
		startDelete();
	}
	
	public void notifyDelete(MetadataManager mg) throws IOException
	{
			ProtocolInstance instance = Protocols.deleteProtocolInstance(Protocols.PROTOCOL_VERSION_MAJOR, Protocols.PROTOCOL_VERSION_MINOR,
					service.getIdentifier(), mg.ownFileBackupInfo_path(filePath).getHash());
				service.getControlSocket().send(instance.toString());
	}
	

	private void startDelete() {

		service.getTimer().schedule( 
				new java.util.TimerTask() {
					@Override
					public void run() {
						eval();
					}
				}, 
		        (long) (2000)
			);
	}

	public void eval() {
		if(!active)
			return;
		
		if(currentAttempt == 2) {
			service.logAndShow("Removing file with" + filePath + ", attempt " + currentAttempt + ")");
			if(responseSocket != null) {
				try {
					SocketWrapper.sendTCP(responseSocket, ""+ EndCondition.CANNOT_DELETE_FILE.ordinal());
				} catch (IOException e) {
					e.printStackTrace();
					service.logAndShowError("Unable to confirm conditional success to TCP client.");
				}
			}
			terminate();
		}
		else {
			++currentAttempt;
			startDelete();					
		}
	}

	@Override
	public Boolean handle(ProtocolInstance message) {
		return false;
	}

	public Boolean active() {
		return active;
	}

	public void terminate() {
		active = false;
		service.removeProcessor(this);
	}
	
	public void deleteFile(MetadataManager mg, String pathFile)
	{
		if(mg.ownFileBackupInfo_path(pathFile) != null)
				if(utils.Files.fileValid(pathFile))
				{
					// Remove file
					utils.Files.removeFile(pathFile);
					// Remove file from metadata
					mg.ownFilesInfo().remove(mg.ownFileBackupInfo_path(pathFile));
					service.logAndShow("File provided by filePath was successfully deleted!");
				}
				else
					service.logAndShow("File provided by filePath doesn't exist!");
					
	}
}
