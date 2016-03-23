package backupservice.protocols.processors;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import network.MulticastSocketWrapper;
import filesystem.FileChunk;
import filesystem.FileManager;
import filesystem.SplitFile;
import backupservice.BackupService;
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
		private MulticastSocketWrapper outgoing_socket = null;
		private FileChunk chunk = null;
		private int replication_deg = -1;
		
		public ChunkSender(MulticastSocketWrapper socket, FileChunk chunk, int replication_deg) {
			this.outgoing_socket = socket;
			this.chunk = chunk;
			this.replication_deg = replication_deg;
		}
		
		public void run() {
			// TODO acabar
		}
		
		public FileChunk chunk() {
			return chunk;
		}
		
		public int chunkNum() {
			return chunk.getchunkNum();
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
		
		// TODO ver se mensagem é para nós e tratar
		
		return null;
	}
	
	public void initiate() {

		SplitFile split_file;
		
		// TODO split file into chunks
		try {
			split_file = FileManager.splitFile(file_path, replication_deg, Protocols.MAX_PACKET_LENGTH);
		} catch (IOException e) {
			e.printStackTrace();
			// TODO notify response_socket of failure due to file system
			return;
		}
		
		ArrayList<FileChunk> chunks = split_file.getChunkList();
		for(int i = 0; i < chunks.size(); ++i) {
			senders.add(new ChunkSender(service.getBackupSocket(), chunks.get(i), replication_deg));
		}
		
		// TODO send chunks and store actual replication deg
		// TODO repetir até atingir replicação pretendida
		// TODO guardar quem respondeu
		// TODO registar tudo em metadata
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
