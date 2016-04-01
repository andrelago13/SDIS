package backupservice.protocols.processors;

import java.io.IOException;
import java.net.Socket;

import network.SocketWrapper;
import filesystem.metadata.MetadataManager;
import backupservice.BackupService;
import backupservice.protocols.ProtocolInstance;
import backupservice.protocols.Protocols;

public class DeleteInitiator implements ProtocolProcessor {

	public static enum EndCondition {
		SUCCESS,
		MULTICAST_UNREACHABLE,
		CANNOT_DELETE_FILE,
		FILE_WAS_NOT_BACKED_UP
	}

	private BackupService service = null;
	private String filePath = null;

	private Socket responseSocket = null;
	private Boolean active = false;

	private int currentAttempt = 0;

	MetadataManager mg;

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

		service.logAndShow("Starting DeleteInitiator...");

		mg = service.getMetadata();

		if(!deleteFile(mg, filePath))
		{
			service.logAndShow("File provided by filePath doesn't exist!");

			if(responseSocket != null) {
				try {
					SocketWrapper.sendTCP(responseSocket, "" + EndCondition.CANNOT_DELETE_FILE.ordinal());
				} catch (IOException e) {
					e.printStackTrace();
					service.logAndShowError("Unable to notify initiator of DELETE protocol");
				}
			}
			terminate();
			return;
		}

		try {
			notifyDelete(mg);
		} catch (IOException e) {
			e.printStackTrace();
			if(responseSocket != null) {
				try {
					SocketWrapper.sendTCP(responseSocket, "" + EndCondition.MULTICAST_UNREACHABLE.ordinal());
				} catch (IOException e2) {
					e2.printStackTrace();
					service.logAndShowError("Unable to notify initiator of DELETE protocol");
				}
			}
		}
		startDelete();
	}

	public void notifyDelete(MetadataManager mg) throws IOException
	{
		ProtocolInstance instance = Protocols.deleteProtocolInstance(Protocols.versionMajor(), Protocols.versionMinor(),
				service.getIdentifier(), mg.ownFileBackupInfo_path(filePath).getHash());
		service.sendControlSocket(instance.toString());
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
					SocketWrapper.sendTCP(responseSocket, ""+ EndCondition.SUCCESS.ordinal());
				} catch (IOException e) {
					e.printStackTrace();
					service.logAndShowError("Unable to confirm conditional success to TCP client.");
				}
			}
			terminate();
		}
		else {
			try {
				notifyDelete(mg);
			} catch (IOException e) {
				e.printStackTrace();
				if(responseSocket != null) {
					try {
						SocketWrapper.sendTCP(responseSocket, "" + EndCondition.MULTICAST_UNREACHABLE.ordinal());
					} catch (IOException e2) {
						e2.printStackTrace();
						service.logAndShowError("Unable to notify initiator of DELETE protocol");
					}
				}
			}
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

	public Boolean deleteFile(MetadataManager mg, String pathFile)
	{
		if(mg.ownFileBackupInfo_path(pathFile) != null)
			if(utils.Files.fileValid(pathFile))
			{
				// Remove file
				utils.Files.removeFile(pathFile);
				// Remove file from metadata
				mg.ownFilesInfo().remove(mg.ownFileBackupInfo_path(pathFile));
				service.logAndShow("File provided by filePath was successfully deleted!");
				return true;
			}

		return false;			
	}
}
