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
		// TODO		se n�o estiver, concluir com mensagem apropriada ok
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
					// FIXME se esta exce��o for lan�ada n�o � porque o ficheiro n�o foi backed up (v� o fixme da fun��o deleteFile) mas sim porque n�o
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
		
		// FIXME o objetivo � enviar 3 DELETE com 2 segundos de intervalo. Tu envias um DELETE na chamada a notifyDelete mas n�o voltas a chamar essa fun��o.
		// o eval tamb�m vai ter de enviar o delete porque o eval � que � chamado a cada 2 segundos
		
		if(currentAttempt == 2) {
			// FIXME se atingires a tentativa 2 n�o significa que n�o conseguiste fazer DELETE, significa que conseguiste e que
			// j� acabaste, logo tens de mudar a resposta que mandas para o tcp
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
			// FIXME provavelmente aqui � que envias o notifyDelete
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
		

		// FIXME n�o devias ter um else neste if e n�o devias ter um retorno na fun��o?
		// repara que, se alguma destas exce��es for lan�ada � porque n�o conseguiste apagar o ficheiro, logo tamb�m n�o vais enviar o delete. Se isto
		// acontecer terminas, mostras uma mensagem no ecr� e respondes ao tcp
		// para al�m disso, se o mg.ownFileBackupInfo_path(pathFile) for null, � porque o chunk nunca foi backed up, logo tmb n�o fazes o delete
					
	}
}
