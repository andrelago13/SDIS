package backupservice.protocols.processors;

import java.util.ArrayList;

import filesystem.metadata.ChunkBackupInfo;
import filesystem.metadata.FileBackupInfo;
import filesystem.metadata.MetadataManager;
import backupservice.BackupService;
import backupservice.protocols.ProtocolInstance;

public class DeletePeer implements ProtocolProcessor {

	private String file_hash;

	private BackupService service;

	private Boolean active = false;
	
	MetadataManager mg;

	public DeletePeer(BackupService service, String file_hash) {
		this.service = service;
		this.file_hash = file_hash;
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

		service.logAndShow("Starting DeletePeer...");

		mg = service.getMetadata();
		
		FileBackupInfo peerFile =  mg.peerFileBackupInfo(file_hash);
		
		if(checkpeerFile(peerFile)) {
			ArrayList<ChunkBackupInfo> chunks = peerFile.getChunks();
			eraseChuncksFromFile(chunks);
			service.logAndShow("Ending DeletePeer...");
			terminate();
		}
	}

	@Override
	public void terminate() {
		active = false;
		service.removeProcessor(this);
	}
	
	public Boolean checkpeerFile(FileBackupInfo p)
	{
		if(p == null) {
			service.logAndShow("File with " + file_hash + " requested by peer is not present. Terminating.");
			terminate();
			return false;
		}
		return true;
	}
	
	public void eraseChuncksFromFile(ArrayList<ChunkBackupInfo> chunks)
	{
		service.show("Prepare to remove chunk!");

		for(int i = 0; i < chunks.size(); i++)
		{
			String path = BackupService.BACKUP_FILE_PATH + service.getIdentifier() + "/" + file_hash + "_" + chunks.get(i).getNum();
			
			if(utils.Files.fileValid(path))
			{
				utils.Files.removeFile(path);
				mg.peerFilesInfo().remove(mg.peerChunkBackupInfo(file_hash, chunks.get(i).getNum()));
				service.backupMetadata();
				service.logAndShow("Chunk provided by path was successfully deleted!");
			}
			else
			{
				service.logAndShowError("Chunk does not exist. Terminating.");
				terminate();
				return;
			}
			active = true;
		}
	}

}
