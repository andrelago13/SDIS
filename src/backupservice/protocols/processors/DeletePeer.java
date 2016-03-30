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

		service.logAndShow("Starting DeleteInitiator...");

		mg = service.getMetadata();
		
		FileBackupInfo peerFile =  mg.peerFileBackupInfo(file_hash);
		checkpeerFile(peerFile);

		ArrayList<ChunkBackupInfo> chunks = peerFile.getChunks();
		eraseChuncksFromFile(chunks);
		
		terminate();
	}

	@Override
	public void terminate() {
		active = false;
		service.removeProcessor(this);
	}
	
	public void checkpeerFile(FileBackupInfo p)
	{
		if(p == null) {
			service.logAndShow("File with " + file_hash + "requested by peer is not present. Terminating.");
			terminate();
		}
	}
	
	public void eraseChuncksFromFile(ArrayList<ChunkBackupInfo> chunks)
	{
		for(int i = 0; i < chunks.size(); i++)
		{
			String path = BackupService.BACKUP_FILE_PATH + service.getIdentifier() + "/" + file_hash + "_" + chunks.get(i).getNum();
			
			if(utils.Files.fileValid(path))
			{
				utils.Files.removeFile(path);
				mg.peerFilesInfo().remove(mg.peerChunkBackupInfo(file_hash, chunks.get(i).getNum()));
			}
			else
			{
				service.logAndShowError("Unable to delete chunk #" + chunks.get(i).getNum() + " of file " + file_hash + "requested by peer. Terminating.");
				terminate();
				return;
			}
			active = true;
		}
	}

}
