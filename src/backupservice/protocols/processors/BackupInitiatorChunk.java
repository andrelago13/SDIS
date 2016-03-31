package backupservice.protocols.processors;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import network.MulticastSocketWrapper;
import filesystem.FileChunk;
import backupservice.BackupService;
import backupservice.protocols.ProtocolHeader;
import backupservice.protocols.ProtocolInstance;
import backupservice.protocols.Protocols;

public class BackupInitiatorChunk implements ProtocolProcessor {
	
	private BackupService service = null;
	private String file_hash = null;
	private int chunk_num = -1;
	private int replication_deg = -1;
	
	private Boolean active = false;
	
	private ChunkSender sender = null;
	
	private class ChunkSender extends Thread {
		private String file_hash = null;
		private FileChunk chunk = null;
		private int replication_deg = -1;
		private BackupService service = null;
		private MulticastSocketWrapper outgoing_socket = null;
		
		private int current_attempt = 0;
		private ArrayList<Integer> responded_peers = null;
		
		public ChunkSender(String file_hash, BackupService service, FileChunk chunk, int replication_deg) {
			this.file_hash = file_hash;
			this.chunk = chunk;
			this.replication_deg = replication_deg;
			this.service = service;
			this.outgoing_socket = this.service.getBackupSocket();
			this.responded_peers = new ArrayList<Integer>();
		}
		
		private void sendChunk() {
			service.logAndShow("Backing up chunk #" + chunk.getchunkNum() + ", file " + file_hash + " (rep deg desired " + this.replication_deg + ", attempt " + current_attempt + ")");
			ProtocolInstance instance = Protocols.putChunkProtocolInstance(Protocols.versionMajor(), Protocols.versionMinor(), 
					service.getIdentifier(), this.file_hash, chunk.getchunkNum(), replication_deg, chunk.getchunkContent());
			
			byte[] packet_bytes = instance.toBytes();
			try {
				outgoing_socket.send(packet_bytes, packet_bytes.length);
			} catch (IOException e1) {
				service.logAndShowError("Unable to reach multicast");
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
				service.logAndShow("Ending backup chunk #" + chunk.getchunkNum() + ", file " + file_hash + " (rep deg achieved " + responded_peers.size() + ", attempt " + current_attempt + ")");
				terminate();
			} else {
				if(responded_peers.size() >= replication_deg) {
					service.logAndShow("Ending backup of chunk #" + chunk.getchunkNum() + ", file " + file_hash + " (rep deg achieved " + responded_peers.size() + ", attempt " + current_attempt + ")");
					terminate();
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
				service.getMetadata().updatePeerFile(file_hash, chunk.getchunkNum(), chunk.getreplicationNumber(), responded_peers.size(), chunk.getchunkContent().length);
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
				if(file_id != null && file_id.equals(file_hash) && chunk_no == chunk.getchunkNum()) {
					return true;
				}
			}
			
			return false;
		}
	}

	public BackupInitiatorChunk(BackupService service, String file_hash, int chunk_num, int replication_deg) {
		this.service = service;
		this.file_hash = file_hash;
		this.replication_deg = replication_deg;
		this.chunk_num = chunk_num;
	}
	
	@Override
	public Boolean handle(ProtocolInstance message) {
		if(!active())
			return false;
		
		if(sender != null) {
			if(sender.interested(message)) {
				sender.handle(message);
				return true;
			}
		}
		
		return false;
	}
	
	public void initiate() {
		
		service.logAndShow("Starting \"emergency\" backup of chunk #" + chunk_num + " of file " + file_hash + ".");
		
		byte[] chunkContent = getChunkContent();
		if(chunkContent == null) {
			service.logAndShowError("Unable to retrieve chunk to backup");
			terminate();
			return;
		}
		
		FileChunk chunk = new FileChunk(chunk_num, chunkContent, replication_deg);
		sender = new ChunkSender(file_hash, service, chunk, replication_deg);
		
		active = true;
		
		sender.start();
	}
	
	private byte[] getChunkContent() {
		String path = BackupService.BACKUP_FILE_PATH + service.getIdentifier() + "/" + file_hash + "_" + chunk_num;
		byte[] result = null;

		try {
			Path path_obj = Paths.get(path);
			result = Files.readAllBytes(path_obj);
			
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		
		return result;
	}

	public void terminate() {
		active = false;
		
		try {
			if(sender != null)
				sender.interrupt();
		} catch (Exception e) {
			// do nothing
		}
		
		service.removeProcessor(this);
	}

	@Override
	public Boolean active() {
		return active;
	}

}
