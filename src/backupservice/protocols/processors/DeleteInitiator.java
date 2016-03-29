package backupservice.protocols.processors;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import network.MulticastSocketWrapper;
import network.SocketWrapper;
import filesystem.metadata.ChunkBackupInfo;
import filesystem.metadata.FileBackupInfo;
import filesystem.metadata.MetadataManager;
import backupservice.BackupService;
import backupservice.protocols.ProtocolHeader;
import backupservice.protocols.ProtocolInstance;
import backupservice.protocols.Protocols;

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
		
		// TODO ver se ficheiro foi backed up (ver metadata)
		// TODO		se não estiver, concluir com mensagem apropriada
		// TODO		se estiver, apagar o ficheiro, apagas a metadata e envias DELETE (3x, 2s)
		// TODO		terminas

		/*MetadataManager mg = service.getMetadata();

		// Testou-se com um ficheiro já criado
		//	mg.ownFilesInfo().add(new FileBackupInfo("resources/test_read.txt", "12345"));


		int i = 0;
		for(; i < mg.ownFilesInfo().size(); i++)
			if(mg.ownFilesInfo().get(i).getFilePath().equals(filePath))
			{
				if(utils.Files.fileValid(filePath))
				{
					utils.Files.removeFile(filePath);
					break;
				}
			}
		// falta apagar metadata

		if(i == mg.ownFilesInfo().size())
			service.logAndShow("File provided by filePath doesn't exist!");

		if(responseSocket != null)
			try {
				SocketWrapper.sendTCP(responseSocket, condition_codes[EndConditionD.CANNOT_DELETE_FILE.ordinal()]);
			} catch (IOException e) {
				e.printStackTrace();
			}

		ArrayList<FileBackupInfo> peerFiles = mg.peerFilesInfo();
		//		peerFiles.add(new FileBackupInfo("resources/test_join", "12945"));

		for(int j = 0; j < peerFiles.size(); j++)
			if(peerFiles.get(j).getFilePath().equals(filePath))
				removers.add(new ChunkRemove(service, peerFiles.get(j).getChunks().get(j), peerFiles.get(j).getHash()));

		if(removers.size() != 0)
		{
			active = true;

			for(int l = 0; l < removers.size(); l++) {
				removers.get(l).start();
			}
		}*/
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
}
