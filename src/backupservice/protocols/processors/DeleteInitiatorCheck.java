package backupservice.protocols.processors;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import filesystem.metadata.ChunkBackupInfo;
import filesystem.metadata.MetadataManager;
import backupservice.BackupService;
import backupservice.protocols.ProtocolHeader;
import backupservice.protocols.ProtocolInstance;
import backupservice.protocols.Protocols;

public class DeleteInitiatorCheck implements ProtocolProcessor {

	private boolean active = false;
	private String hash = null;
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
		ArrayList<String> peerFiles = mg.getPeerFilesHashes(mg.peerFilesInfo());

		checkFileHash(peerFiles);

		service.logAndShow("Prepare to send message to each file...");
	}

	public void checkFileHash(ArrayList<String> deleted_files)
	{	
		for (int i = 0; i < deleted_files.size(); ++i)
		{
			try {
				hash = deleted_files.get(i);
				notifyDeleteCheck(hash);
				service.logAndShow("Message EXISTS was sent with file: " + hash);
				Thread.sleep(500);
			} catch (IOException | InterruptedException e) {
				service.logAndShow("Unable to notify peers with EXISTS message!");
			}
		}		
	}

	public void notifyDeleteCheck(String Hash) throws IOException
	{
		ProtocolInstance instance = Protocols.existsProtocolInstance(Protocols.versionMajor(), Protocols.versionMinor(),
				service.getIdentifier(), Hash);
		service.sendControlSocket(instance.toString());
	}

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

		ProtocolHeader header = message.getHeader();

		if(header.getMessage_type() == Protocols.MessageType.WASDELETED) {
			if(header.getSender_id() != service.getIdentifier()) {

				ArrayList<ChunkBackupInfo> chunks = mg.getpeerChunks(header.getFile_id());

				for(int j = 0; j < chunks.size();++j)
				{
					String path = BackupService.BACKUP_FILE_PATH + service.getIdentifier() + "/" + header.getFile_id() + "_" + chunks.get(j).getNum();

					if(utils.Files.fileValid(path))
					{
						utils.Files.removeFile(path);
						mg.peerFilesInfo().remove(mg.peerChunkBackupInfo(header.getFile_id(), chunks.get(j).getNum()));
						mg.addDeletedFile(header.getFile_id()+chunks.get(j).getNum());
						// TODO service.bakupMetadata();
						service.logAndShow("Chunk provided by hash was successfully deleted!");
					}
					else
					{
						service.logAndShowError("Chunk does not exist. Terminating.");
						terminate();
					}
				}

				return true;
			}
		}
		return false;
	}

}
