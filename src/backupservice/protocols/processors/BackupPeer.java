package backupservice.protocols.processors;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Random;

import backupservice.BackupService;
import backupservice.protocols.ProtocolHeader;
import backupservice.protocols.ProtocolInstance;
import backupservice.protocols.Protocols;

public class BackupPeer implements ProtocolProcessor {
	
	public static final int MAX_DELAY = 400;
	public static final int WAIT_FOR_STORED_DELAY = 16000;
	public static final int MAX_ATTEMPT = 1;
	
	private BackupService service;
	private int sender_id;
	private String file_id;
	private int chunk_no;
	private int chunk_desired_replication;
	private byte[] chunk_content;
	
	private Random rand = new Random();
	private int answer_delay;
	private ProtocolInstance reply;
	
	private Boolean active = false;
	private int attempt = 0;
	private int prev_replies = 0;
	private ArrayList<Integer> responded_peers;
	
	public BackupPeer(ProtocolInstance starter_message, BackupService service) {
		this.service = service;
		ProtocolHeader message_header = starter_message.getHeader();
		chunk_content = starter_message.getBody().getContent();
		this.sender_id = message_header.getSender_id();
		this.file_id = message_header.getFile_id();
		this.chunk_no = message_header.getChunk_no();
		this.chunk_desired_replication = message_header.getReplication_deg();

		generateDelay();
		generateProtocolInstance();
		responded_peers = new ArrayList<Integer>();
	}
	
	private void replyWithDelay() {
		replyWithDelay(answer_delay);
	}
	
	private void replyWithDelay(int delay) {
		new java.util.Timer().schedule( 
		        new java.util.TimerTask() {
		            @Override
		            public void run() {
		                try {
							service.getControlSocket().send(reply.toString());
						} catch (IOException e) {
							e.printStackTrace();
							System.err.println("Unable to reply STORED");
						}
		            }
		        }, 
		        delay 
		);
	}
	
	public int generateDelay() {
		answer_delay = rand.nextInt(MAX_DELAY + 1);
		return answer_delay;
	}

	public ProtocolInstance generateProtocolInstance() {
		reply = Protocols.storedProtocolInstance(Protocols.PROTOCOL_VERSION_MAJOR, Protocols.PROTOCOL_VERSION_MINOR, sender_id, file_id, chunk_no);
		return reply;
	}
	
	@Override
	public Boolean handle(ProtocolInstance message) {
		// TODO Responder a todos os PUTCHUNK (aguardar random (0-400ms) e responder STORED)
		// TODO Ativar "escuta" por outros STORED (espera tempo máximo de delay e, se receber algum stored, volta a esperar) - sistema semelhante ao backupinitiator
		// TODO guardar metadata
		// TODO guardar só se houver espaço
		return null;
	}

	@Override
	public Boolean active() {
		return active;
	}

	private void eval() {
		++attempt;
		if(attempt > MAX_ATTEMPT) {
			terminate();
		} else {
			if(responded_peers.size() == prev_replies) {
				terminate();
			} else {
				prev_replies = responded_peers.size();
				evalDelay(WAIT_FOR_STORED_DELAY);
			}
		}
	}
	
	private void evalDelay(int delay) {
		new java.util.Timer().schedule( 
		        new java.util.TimerTask() {
		            @Override
		            public void run() {
		                eval();
		            }
		        }, 
		        delay 
		);
	}
	
	@Override
	public void initiate() {
		active = true;
		try {
			storeChunk();
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		generateDelay();
		service.getMetadata().updatePeerFile(file_id, chunk_no, chunk_desired_replication, 1, chunk_content.length);
		replyWithDelay();
		
		evalDelay(WAIT_FOR_STORED_DELAY);
		
	}

	@Override
	public void terminate() {
		active = false;
		service.removeProcessor(this);
	}
	
	private void storeChunk() throws FileNotFoundException, UnsupportedEncodingException {
		// TODO acabar
		PrintWriter writer = new PrintWriter("the-file-name.txt", "UTF-8");
		writer.print(chunk_content);
		writer.close();
	}

}
