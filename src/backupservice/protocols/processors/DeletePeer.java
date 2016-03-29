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

		// TODO ver se ficheiro existe na metadata
		// TODO 	se não existir, terminas c/mensagem no log
		// TODO 	se existir
		// TODO			vês na metadata quais os chunks que tens
		// TODO			vais ao path BackupService.BACKUP_FILE_PATH/ID_DESTE_PEER/HASH_NUM
		// TODO 		apagar ficheiros e metadata
		
		/*MetadataManager mg = service.getMetadata();
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

		}*/
	}

	@Override
	public void terminate() {
		active = false;
		service.removeProcessor(this);
	}

}
