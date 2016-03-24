package backupservice.protocols.processors;

import java.util.Random;

import backupservice.BackupService;
import backupservice.protocols.ProtocolHeader;
import backupservice.protocols.ProtocolInstance;

public class BackupPeer implements ProtocolProcessor {
	
	public static final int MAX_DELAY = 400;
	
	private BackupService service;
	private int sender_id;
	private String file_id;
	private int chunk_no;
	private int chunk_desired_replication;
	
	private Random rand = new Random();
	private int answer_delay;
	
	private Boolean active = false;
	
	public BackupPeer(ProtocolInstance starter_message, BackupService service) {
		this.service = service;
		ProtocolHeader message_header = starter_message.getHeader();
		this.sender_id = message_header.getSender_id();
		this.file_id = message_header.getFile_id();
		this.chunk_no = message_header.getChunk_no();
		this.chunk_desired_replication = message_header.getReplication_deg();
		
		generateDelay();
	}
	
	public int generateDelay() {
		answer_delay = rand.nextInt(MAX_DELAY + 1);
		return answer_delay;
	}

	@Override
	public Boolean handle(ProtocolInstance message) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean active() {
		return active;
	}

	@Override
	public void initiate() {
		// TODO Ativar "escuta" por outros STORED (espera tempo máximo de delay e, se receber algum stored, volta a esperar) - sistema semelhante ao backupinitiator
		// TODO aguardar random (0-400ms) e responder STORED
		// TODO Responder a todos os PUTCHUNK
		active = true;
	}

	@Override
	public void terminate() {
		// TODO Auto-generated method stub
		active = false;
	}

}
