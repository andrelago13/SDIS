package backupservice.protocols.processors;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.TimerTask;

import filesystem.metadata.ChunkBackupInfo;
import filesystem.metadata.MetadataManager;
import backupservice.BackupService;
import backupservice.protocols.ProtocolHeader;
import backupservice.protocols.ProtocolInstance;
import backupservice.protocols.Protocols;

public class DeleteInitiatorCheck implements ProtocolProcessor {
	
	public final static int BETWEEN_FILE_DELAY = 100;

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

		service.logAndShow("Starting check for deleted files.");

		mg = service.getMetadata();
		ArrayList<String> peerFiles = mg.getPeerFilesHashes(mg.peerFilesInfo());

		checkFileHash(peerFiles);

		int num_peer_files = peerFiles.size();
		int delay = generateTerminateDelay(num_peer_files);
		
		service.getTimer().schedule(new TimerTask() {
			@Override
			public void run() {
				terminate();
			}
			
		}, delay);
	}
	
	private static int generateTerminateDelay(int number_files) {
		return number_files*BETWEEN_FILE_DELAY + 5000;
	}

	public void checkFileHash(ArrayList<String> deleted_files)
	{	
		for (int i = 0; i < deleted_files.size(); ++i)
		{
			try {
				hash = deleted_files.get(i);
				notifyDeleteCheck(hash);
				service.logAndShow("Message EXISTS was sent with file: " + hash);
				Thread.sleep(BETWEEN_FILE_DELAY);
			} catch (IOException | InterruptedException e) {
				service.logAndShow("Unable to notify peers with EXISTS message!");
			}
		}		
	}

	public void notifyDeleteCheck(String hash) throws IOException
	{
		ProtocolInstance instance = Protocols.existsProtocolInstance(Protocols.versionMajor(), Protocols.versionMinor(),
				service.getIdentifier(), hash);
		byte[] buffer = instance.toBytes();
		service.sendControlSocket(buffer, buffer.length);
	}

	@Override
	public Boolean active() {
		return active;
	}

	@Override
	public void terminate() {
		service.logAndShow("Terminating check for deleted files.");
		
		active = false;
		service.removeProcessor(this);		
	}

	@Override
	public Boolean handle(ProtocolInstance message) {

		ProtocolHeader header = message.getHeader();

		if(header.getMessage_type() == Protocols.MessageType.WASDELETED) {
			if(header.getSender_id() != service.getIdentifier()) {
				
				String file_hash = header.getFile_id();

				ArrayList<ChunkBackupInfo> chunks = mg.getpeerChunks(file_hash);

				for(int j = 0; j < chunks.size();++j)
				{
					String path = BackupService.BACKUP_FILE_PATH + service.getIdentifier() + "/" + file_hash + "_" + chunks.get(j).getNum();

					if(utils.Files.fileValid(path))
					{
						utils.Files.removeFile(path);
						service.getMetadata().deletePeerFile(file_hash);
						//mg.peerFilesInfo().remove(mg.peerChunkBackupInfo(file_hash, chunks.get(j).getNum()));
						//service.getMetadata().addDeletedFile(file_hash);
						service.backupMetadata();
						service.logAndShow("Chunk provided by hash " + file_hash + " was successfully deleted!");
					}
					else
					{
						service.logAndShowError("Chunk does not exist.");
					}
					service.getMetadata().addDeletedFile(file_hash);
					service.backupMetadata();
				}

				return true;
			}
		}
		return false;
	}

}
