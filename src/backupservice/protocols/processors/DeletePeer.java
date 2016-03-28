package backupservice.protocols.processors;

import java.util.ArrayList;
import java.util.Random;

import filesystem.metadata.ChunkBackupInfo;
import filesystem.metadata.FileBackupInfo;
import filesystem.metadata.MetadataManager;
import backupservice.BackupService;
import backupservice.protocols.ProtocolHeader;
import backupservice.protocols.ProtocolInstance;
import backupservice.protocols.Protocols;

public class DeletePeer implements ProtocolProcessor {

	public final static int MAX_DELAY = 400;

	private String file_hash;

	private BackupService service;

	private Boolean active = false;
	
	private Random random;
	private int delay;

	public DeletePeer(BackupService service, String file_hash) {
		this.service = service;
		this.file_hash = file_hash;
	}

	@Override
	public Boolean handle(ProtocolInstance message) {
		if(!active)
			return false;

		ProtocolHeader header = message.getHeader();

		if(header.getMessage_type() == Protocols.MessageType.DELETE) {
			if(header.getFile_id() == file_hash) {
				service.log("File with " + file_hash + " already deleted by peer. Terminating.");
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

	@Override
	public void initiate() {

		MetadataManager mg = service.getMetadata();
		FileBackupInfo peerFile =  mg.peerFileBackupInfo(file_hash);

		if(peerFile == null) {
			service.logAndShow("File with " + file_hash + "requested by peer is not present. Terminating.");
			terminate();
		}

		ArrayList<ChunkBackupInfo> chunks = peerFile.getChunks();

		for(int i = 0; i < chunks.size(); i++)
		{
			String path = BackupService.BACKUP_FILE_PATH + service.getIdentifier() + "/" + file_hash + "_" + chunks.get(i).getNum();
			
			if(utils.Files.fileValid(path))
				utils.Files.removeFile(path);
			else
			{
				service.logAndShowError("Unable to delete chunk #" + chunks.get(i).getNum() + " of file " + file_hash + "requested by peer. Terminating.");
				terminate();
				return;
			}

			active = true;
			startDelayedResponse();

		}
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
	}


	@Override
	public void terminate() {
		active = false;
		service.removeProcessor(this);
	}

}
