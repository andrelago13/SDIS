package backupservice.protocols.processors;

import java.net.Socket;
import java.util.ArrayList;

import network.MulticastSocketWrapper;
import filesystem.FileChunk;
import filesystem.SplitFile;
import backupservice.BackupService;
import backupservice.protocols.ProtocolHeader;
import backupservice.protocols.ProtocolInstance;
import backupservice.protocols.Protocols;

public class DeleteInitiator implements ProtocolProcessor {
	
	public static enum EndConditionD {
		SUCCESS,
		MULTICAST_UNREACHABLE,
		TCP_UNREACHABLE,
		FILE_UNREACHABLE
	}
	
	public static final String[] condition_codes = {"0", "-1", "-2", "-3"};

	private BackupService service = null;
	private String filePath = null;
	
	private Socket responseSocket = null;
	private Boolean active = false;
	
	// TODO Acabar a classe ChunKRemove
	private ArrayList<ChunKRemove> removers = null;
	
	private class ChunKRemove extends Thread {
		private FileChunk chunk = null;
		private BackupService service = null;
		private MulticastSocketWrapper outgoing_socket = null;
		private int currentAttempt = 0;

		private ArrayList<Integer> respondedPeers = null;
		
		public ChunKRemove(BackupService service, FileChunk chunk) {
			this.chunk = chunk;
			this.service = service;
			this.outgoing_socket = this.service.getBackupSocket();
			this.respondedPeers = new ArrayList<Integer>();
		}
		
		// TODO implementar run e eval() e acabar o handle
		
		public boolean interested(ProtocolInstance message) {
			if(message == null)
				return false;
			
			ProtocolHeader header = message.getHeader();
			if(header != null && header.getMessage_type() == Protocols.MessageType.DELETE) {
				String file_id = header.getFile_id();
				if(file_id != null)
					return true;
			}
			return false;
		}

		public void handle(ProtocolInstance message) {
			// TODO Auto-generated method stub
		}
	}
		
	public DeleteInitiator(BackupService service, String filePath) {
		this(service, filePath, null);
	}
	
	public DeleteInitiator(BackupService service, String filePath, Socket response_socket) {
		this.service = service;
		this.filePath = filePath;
		this.responseSocket = response_socket;
	}	
	
	// TODO implementar o initiate

	@Override
	public void initiate() {
		// TODO Auto-generated method stub
	}

	@Override
	public Boolean handle(ProtocolInstance message) {
		if(!active)
			return false;
		
		for(int i = 0; i < removers.size(); ++i) {
			if(removers.get(i).interested(message)) {
				final ChunKRemove sender = removers.get(i);
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

	public Boolean active() {
		return active;
	}

	public void terminate() {
		active = false;
		service.removeProcessor(this);
	}
}
