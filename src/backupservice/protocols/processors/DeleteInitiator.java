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
					// FIXME se esta exceção for lançada não é porque o ficheiro não foi backed up (vê o fixme da função deleteFile) mas sim porque não
					// conseguiste escrever no multicast. Tens de mudar o EndCondition aqui
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
		
		// FIXME o objetivo é enviar 3 DELETE com 2 segundos de intervalo. Tu envias um DELETE na chamada a notifyDelete mas não voltas a chamar essa função.
		// o eval também vai ter de enviar o delete porque o eval é que é chamado a cada 2 segundos
		
		if(currentAttempt == 2) {
			// FIXME se atingires a tentativa 2 não significa que não conseguiste fazer DELETE, significa que conseguiste e que
			// já acabaste, logo tens de mudar a resposta que mandas para o tcp
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
			// FIXME provavelmente aqui é que envias o notifyDelete
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
		

		// FIXME não devias ter um else neste if e não devias ter um retorno na função?
		// repara que, se alguma destas exceções for lançada é porque não conseguiste apagar o ficheiro, logo também não vais enviar o delete. Se isto
		// acontecer terminas, mostras uma mensagem no ecrã e respondes ao tcp
		// para além disso, se o mg.ownFileBackupInfo_path(pathFile) for null, é porque o chunk nunca foi backed up, logo tmb não fazes o delete
					
	}
}
