package backupservice.protocols.processors;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import network.MulticastSocketWrapper;
import network.SocketWrapper;
import filesystem.FileChunk;
import filesystem.FileManager;
import filesystem.SplitFile;
import backupservice.BackupService;
import backupservice.protocols.ProtocolHeader;
import backupservice.protocols.ProtocolInstance;
import backupservice.protocols.Protocols;

public class BackupInitiator implements ProtocolProcessor {
	
	private BackupService service = null;
	private String file_path = null;
	private int replication_deg = -1;
	
	private Boolean active = false;
	
	private Socket response_socket = null;
	
	private ArrayList<ChunkSender> senders = null;
	
	private class ChunkSender extends Thread {
		private SplitFile split_file = null;
		private FileChunk chunk = null;
		private int replication_deg = -1;
		private BackupService service = null;
		private MulticastSocketWrapper outgoing_socket = null;
		
		private int current_attempt = 0;
		private ArrayList<Integer> responded_peers = null;
		
		public ChunkSender(SplitFile file, BackupService service, FileChunk chunk, int replication_deg) {
			this.split_file = file;
			this.chunk = chunk;
			this.replication_deg = replication_deg;
			this.service = service;
			this.outgoing_socket = this.service.getBackupSocket();
			this.responded_peers = new ArrayList<Integer>();
		}
		
		private void sendChunk() {
			System.out.println("Backing up chunk #" + chunk.getchunkNum() + " from file " + split_file.getFileId() + " (replcation degree " + this.replication_deg + ", attempt " + current_attempt + ")");
			ProtocolInstance instance = Protocols.putChunkProtocolInstance(Protocols.PROTOCOL_VERSION_MAJOR, Protocols.PROTOCOL_VERSION_MINOR, 
					service.getIdentifier(), split_file.getFileId(), chunk.getchunkNum(), replication_deg, chunk.getchunkContent());
			
			byte[] packet_bytes = instance.toBytes();
			try {
				outgoing_socket.send(packet_bytes, packet_bytes.length);
			} catch (IOException e1) {
				e1.printStackTrace();
				try {
					if(response_socket != null)
						SocketWrapper.sendTCP(response_socket, "2");
				} catch (IOException e) {
					e.printStackTrace();
				}
				return;
			}
		}
		
		public void run() {
			sendChunk();
			new java.util.Timer().schedule( 
			        new java.util.TimerTask() {
			            @Override
			            public void run() {
			                eval();
			            }
			        }, 
			        (long) (1000 * Math.pow(2, current_attempt))
			);
		}
		
		private void eval() {
			if(current_attempt == 4) {
				// TODO has no more attempts, respond with error and terminate
			} else {
				if(responded_peers.size() >= replication_deg) {
					// TODO successful, respond success and terminate
				} else {
					++current_attempt;
					run();					
				}
			}
		}
		
		// Assumes it is interested in message
		public void handle(ProtocolInstance message) {
			ProtocolHeader header = message.getHeader();
			int peer_id = header.getSender_id();
			if(!responded_peers.contains(peer_id)) {
				responded_peers.add(peer_id);
				// TODO update registry
			}
		}
		
		public Boolean interested(ProtocolInstance message) {
			if(message == null)
				return false;
			
			ProtocolHeader header = message.getHeader();
			if(header != null && header.getMessage_type() == Protocols.MessageType.STORED) {
				String file_id = header.getFile_id();
				int chunk_no = header.getChunk_no();
				if(file_id != null && file_id.equals(split_file.getFileId()) && chunk_no == chunk.getchunkNum()) {
					return true;
				}
			}
			
			return false;
		}
	}

	public BackupInitiator(BackupService service, String file_path, int replication_deg) {
		this(service, file_path, replication_deg, null);
	}

	public BackupInitiator(BackupService service, String file_path, int replication_deg, Socket response_socket) {
		this.service = service;
		this.file_path = file_path;
		this.replication_deg = replication_deg;
		this.response_socket = response_socket;
		this.senders = new ArrayList<ChunkSender>();
	}
	
	@Override
	public Boolean handle(ProtocolInstance message) {
		if(!active)
			return false;
		
		for(int i = 0; i < senders.size(); ++i) {
			if(senders.get(i).interested(message)) {
				final ChunkSender sender = senders.get(i);
				new Thread( new Runnable() {
				    @Override
				    public void run() {
				    	sender.handle(message);
				    }
				}).start();
				return true;
			}
		}
		
		return false;
	}
	
	public void initiate() {

		SplitFile split_file = null;
		
		try {
			split_file = FileManager.splitFile(file_path, service.getIdentifier() ,replication_deg, Protocols.MAX_PACKET_LENGTH);
		} catch (IOException e) {
			e.printStackTrace();
			try {
				if(response_socket != null)
					SocketWrapper.sendTCP(response_socket, "1");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			terminate();
			return;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			try {
				if(response_socket != null)
					SocketWrapper.sendTCP(response_socket, "3");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			terminate();
			return;
		}
		
		ArrayList<FileChunk> chunks = split_file.getChunkList();
		for(int i = 0; i < chunks.size(); ++i) {
			senders.add(new ChunkSender(split_file, service, chunks.get(i), replication_deg));
		}
		
		for(int i = 0; i < senders.size(); ++i) {
			senders.get(i).start();
		}
		
		active = true;
	}
	
	public void terminate() {
		active = false;
		// TODO remove itself from backupservice
	}

	@Override
	public Boolean active() {
		return active;
	}

}
