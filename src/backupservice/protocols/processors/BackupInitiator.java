package backupservice.protocols.processors;

import java.io.IOException;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import network.SocketWrapper;
import filesystem.FileChunk;
import filesystem.FileManager;
import filesystem.SplitFile;
import backupservice.BackupService;
import backupservice.protocols.ProtocolHeader;
import backupservice.protocols.ProtocolInstance;
import backupservice.protocols.Protocols;

public class BackupInitiator implements ProtocolProcessor {
	
	public static enum EndCondition {
		SUCCESS,
		NOT_ENOUGH_REPLICATION,
		MULTICAST_NOT_REACHABLE,
		TCP_NOT_REACHABLE,
		FILE_NOT_REACHABLE,
		HASH_FAILURE
	}
	public static final String[] condition_codes = {"0", "1", "-1", "-2", "-3", "-4"};
	
	private BackupService service = null;
	private String file_path = null;
	private int replication_deg = -1;
	
	private Boolean active = false;
	
	private Socket response_socket = null;
	Boolean one_has_failed = false;
	private ArrayList<Integer> terminated_senders = new ArrayList<Integer>();
	
	private ArrayList<ChunkSender> senders = null;
	
	private class ChunkSender extends Thread {
		private SplitFile split_file = null;
		private FileChunk chunk = null;
		private int replication_deg = -1;
		private BackupService service = null;
		
		private int current_attempt = 0;
		private ArrayList<Integer> responded_peers = null;
		
		public ChunkSender(SplitFile file, BackupService service, FileChunk chunk, int replication_deg) {
			this.split_file = file;
			this.chunk = chunk;
			this.replication_deg = replication_deg;
			this.service = service;
			this.responded_peers = new ArrayList<Integer>();
		}
		
		private void sendChunk() {
			service.logAndShow("Backing up chunk #" + chunk.getchunkNum() + ", file " + split_file.getFileId() + " (rep deg desired " + this.replication_deg + ", attempt " + current_attempt + ")");
			ProtocolInstance instance = Protocols.putChunkProtocolInstance(Protocols.versionMajor(), Protocols.versionMinor(), 
					service.getIdentifier(), split_file.getFileId(), chunk.getchunkNum(), replication_deg, chunk.getchunkContent());
			
			byte[] packet_bytes = instance.toBytes();
			try {
				service.sendBackupSocket(new String(packet_bytes, 0, packet_bytes.length));
			} catch (IOException e1) {
				e1.printStackTrace();
				try {
					if(response_socket != null)
						SocketWrapper.sendTCP(response_socket, condition_codes[EndCondition.MULTICAST_NOT_REACHABLE.ordinal()]);
				} catch (IOException e) {
					e.printStackTrace();
				}
				return;
			}
		}
		
		public void run() {
			if(!active())
				return;
			
			sendChunk();
			service.getTimer().schedule( 
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
			if(!active())
				return;
			
			if(current_attempt == 4) {
				service.logAndShow("Ending backup chunk #" + chunk.getchunkNum() + ", file " + split_file.getFileId() + " (rep deg achieved " + responded_peers.size() + ", attempt " + current_attempt + ")");
				one_has_failed = true;
				terminated_senders.add(this.chunk.getchunkNum());
				senderEnded();
			} else {
				if(responded_peers.size() >= replication_deg) {
					service.logAndShow("Ending backup of chunk #" + chunk.getchunkNum() + ", file " + split_file.getFileId() + " (rep deg achieved " + responded_peers.size() + ", attempt " + current_attempt + ")");
					terminated_senders.add(this.chunk.getchunkNum());
					senderEnded();
				} else {
					++current_attempt;
					run();					
				}
			}
		}
		
		// Assumes it is interested in message
		public void handle(ProtocolInstance message) {
			if(!active())
				return;
			
			ProtocolHeader header = message.getHeader();
			int peer_id = header.getSender_id();
			if(!responded_peers.contains(peer_id)) {
				responded_peers.add(peer_id);
				service.getMetadata().updateOwnFile(file_path, split_file.getFileId(), chunk.getchunkNum(), chunk.getreplicationNumber(), responded_peers.size(), chunk.getchunkContent().length);
				try {
					service.logAndShow("Backing up metadata");
					service.getMetadata().backup();
				} catch (IOException e) {
					e.printStackTrace();
					service.logAndShowError("Unable to backup metadata");
				}
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
		if(!active())
			return false;
		
		for(int i = 0; i < senders.size(); ++i) {
			if(senders.get(i).interested(message)) {
				senders.get(i).handle(message);
				return true;
			}
		}
		
		return false;
	}
	
	public void senderEnded() {
		if(senders.size() == terminated_senders.size()) {
			if(one_has_failed) {
				service.logAndShowError("File backup error. Not enough replication achieved in at least one chunk.");
				if(response_socket != null) {
					try {
						SocketWrapper.sendTCP(response_socket, condition_codes[EndCondition.NOT_ENOUGH_REPLICATION.ordinal()]);
					} catch (IOException e) {
						e.printStackTrace();
						service.logAndShowError("Unable to confirm conditional success to TCP client.");
					}	
				}				
			} else {
				service.logAndShow("File \"" + file_path + "\" backup successful!");
				if(response_socket != null) {
					try {
						SocketWrapper.sendTCP(response_socket, condition_codes[EndCondition.SUCCESS.ordinal()]);
					} catch (IOException e) {
						e.printStackTrace();
						service.logAndShowError("Unable to confirm success to TCP client.");
					}
				}
			}
			terminate();
		}
	}
	
	public void initiate() {

		SplitFile split_file = null;
		
		try {
			split_file = FileManager.splitFile(file_path, service.getIdentifier() ,replication_deg, Protocols.MAX_PACKET_LENGTH);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			try {
				if(response_socket != null)
					SocketWrapper.sendTCP(response_socket, condition_codes[EndCondition.HASH_FAILURE.ordinal()]);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			terminate();
			return;
		} catch (IOException e) {
			// TODO mensagens
			e.printStackTrace();
			try {
				if(response_socket != null)
					SocketWrapper.sendTCP(response_socket, condition_codes[EndCondition.FILE_NOT_REACHABLE.ordinal()]);
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
		
		active = true;
		
		for(int i = 0; i < senders.size(); ++i) {
			if(!active)
				break;
			senders.get(i).start();
		}
	}
	
	public void terminate() {
		active = false;
		
		if(response_socket != null) {
			try {
				response_socket.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		
		for(int i = 0; i < senders.size(); ++i) {
			try {
				senders.get(i).interrupt();
			} catch (Exception e) {
				// do nothing
			}
		}
		
		service.removeProcessor(this);
	}

	@Override
	public Boolean active() {
		return active;
	}

}
