package backupservice.protocols.processors;

import java.net.Socket;

import backupservice.BackupService;
import backupservice.protocols.ProtocolInstance;

public class RestorePeer implements ProtocolProcessor {
	
	private int sender_id;
	private String file_hash;
	private int chunk_no;
	
	private BackupService service;
	
	private Boolean active = false;
	
	public RestorePeer(BackupService service, int sender, String file_hash, int chunk_no) {
		this.service = service;
		this.sender_id = sender;
		this.file_hash = file_hash;
		this.chunk_no = chunk_no;
	}

	@Override
	public Boolean handle(ProtocolInstance message) {
		if(!active)
			return false;
		
		// TODO se receber CHUNK do mesmo chunk, termina
		return null;
	}

	@Override
	public Boolean active() {
		return active;
	}

	@Override
	public void initiate() {
		// TODO ver se há backup do ficheiro e chunk pretendido
		// TODO se não houver, terminar
		// TODO esperar 0-400ms para ver se outros respondem
		// TODO se nenhum responder, enviar chunk
		active = true;
	}

	@Override
	public void terminate() {
		// TODO Auto-generated method stub
		active = false;
	}

}
