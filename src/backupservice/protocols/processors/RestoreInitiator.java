package backupservice.protocols.processors;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.TimerTask;

import network.ResponseGetterThread;
import network.SocketWrapper;
import filesystem.FileChunk;
import filesystem.metadata.FileBackupInfo;
import backupservice.BackupService;
import backupservice.protocols.ProtocolHeader;
import backupservice.protocols.ProtocolInstance;
import backupservice.protocols.Protocols;

public class RestoreInitiator implements ProtocolProcessor {
	
	public final static int DELAY_TIMER = 15000;
	public final static int MAX_ATTEMPTS = 2;

	public static enum EndCondition {
		SUCCESS,
		FILE_DATA_NOT_FOUND,
		MULTICAST_NOT_REACHABLE,
		FILE_MERGE_ERROR,
		CHUNKS_NOT_RECEIVED
	}
	
	private BackupService service;
	private String file_path;
	
	private int number_of_chunks = -1;
	
	private Socket response_socket;
	
	private Boolean active = false;
	private int attempt = 0;
	
	private ArrayList<ChunkRestorer> restorers;
	private ArrayList<FileChunk> received_chunks;
	
	private class ChunkRestorer extends Thread {
		
		private String file_hash;
		private int chunk_no;
		
		private Boolean got_chunk = false;
		private ProtocolInstance reply;
		
		public ChunkRestorer(String file_hash, int chunk_no) {
			this.file_hash = file_hash;
			this.chunk_no = chunk_no;
			reply = Protocols.getchunkProtocolInstance(Protocols.versionMajor(), Protocols.versionMinor(),
					service.getIdentifier(), file_hash, chunk_no);
		}
		
		public void run() {
			sendCommand();
		}
		
		public void sendCommand() {
			service.logAndShow("RESTORE chunk #" + chunk_no + ", file " + file_hash + ".");
			try {
				service.sendControlSocket(reply.toString());
			} catch (IOException e) {
				e.printStackTrace();
				service.logAndShowError("Unable to send GETCHUNK, CONTROL channel not reachable.");
			}
			
		}
		
		private Boolean interested(ProtocolInstance message) {
			if(message == null)
				return false;
			
			ProtocolHeader header = message.getHeader();
			if(header.getMessage_type() == Protocols.MessageType.CHUNK) {
				if(header.getFile_id().equals(file_hash) && header.getChunk_no() == chunk_no) {
					return true;
				} else {
					return false;
				}
			}
			
			return false;
		}
		
		private void handle(ProtocolInstance message) {
			if(got_chunk)
				return;
			
			got_chunk = true;
			received_chunks.add(new FileChunk(chunk_no, message.getBody().getContent(), -1));

			notifyReception(chunk_no);			
		}
		
		private void handleTCP(ProtocolInstance message) {
			if(got_chunk)
				return;
			
			got_chunk = true;
			received_chunks.add(new FileChunk(chunk_no, message.getBody().getContent(), -1));

			notifyReception(chunk_no);
		}
		
		public Boolean gotChunk() {
			return got_chunk;
		}
	}
	
	public RestoreInitiator(BackupService service, String file_path) {
		this(service, file_path, null);
	}
	
	public RestoreInitiator(BackupService service, String file_path, Socket response_socket) {
		this.service = service;
		this.file_path = file_path;
		this.restorers = new ArrayList<ChunkRestorer>();
		this.response_socket = response_socket;
		this.received_chunks = new ArrayList<FileChunk>();
	}

	@Override
	public Boolean handle(ProtocolInstance message) {
		if(!active())
			return false;
		
		for(int i = 0; i < restorers.size(); ++i) {
			if(restorers.get(i).interested(message)) {
				restorers.get(i).handle(message);
				return true;
			}
		}
		
		return false;
	}

	private void handleTCP(String message) {
		ProtocolInstance instance_tcp = Protocols.parseMessage(message);
		if(instance_tcp != null) {
			for(int i = 0; i < restorers.size(); ++i) {
				if(restorers.get(i).interested(instance_tcp)) {
					restorers.get(i).handleTCP(instance_tcp);
					return;
				}
			}
		}
		// TODO
		//System.out.println(message);
	}
	
	@Override
	public Boolean active() {
		return active;
	}

	@Override
	public void initiate() {
		FileBackupInfo file_info = service.getMetadata().ownFileBackupInfo_path(file_path);
		if(file_info == null) {
			file_info = service.getMetadata().ownFileBackupInfo_hash(file_path);
			if(file_info == null) {
				service.logAndShowError("Unable to retrieve data of file to RESTORE. Has the file been backed up earlier?");
				if(response_socket != null) {
					try {
						SocketWrapper.sendTCP(response_socket, "" + EndCondition.FILE_DATA_NOT_FOUND.ordinal());
					} catch (IOException e) {
						e.printStackTrace();
						service.logAndShowError("Unable to notify initiator of RESTORE protocol");
					}
				}
				terminate();
			}
		}
		
		this.number_of_chunks = file_info.getChunks().size();
		String file_hash = file_info.getHash();

		service.logAndShow("Restoring file \"" + file_path + "\" : attempt no. " + attempt + ".");
		for(int i = 0; i < this.number_of_chunks; ++i) {
			restorers.add(new ChunkRestorer(file_hash, i));
			restorers.get(i).start();
		}
		
		active = true;
		
		initiateDelayedCheck();
	}

	private void initiateDelayedCheck() {		
		service.getTimer().schedule(new TimerTask() {
			@Override
            public void run() {
				if(!active)
					return;
				
				makeCheck();
            }
		}, DELAY_TIMER);
	}
	
	public void makeCheck() {
		if(++attempt > MAX_ATTEMPTS) {
			service.logAndShowError("Restoring of file \"" + file_path + "\" failed. Not all chunks were received.");
			if(response_socket != null) {
				try {
					SocketWrapper.sendTCP(response_socket, "" + EndCondition.CHUNKS_NOT_RECEIVED.ordinal());
				} catch (IOException e) {
					e.printStackTrace();
					service.logAndShowError("Unable to confirm error " + EndCondition.CHUNKS_NOT_RECEIVED.ordinal() + " to TCP command sender.");
				}
			}
			terminate();
		} else {
			service.logAndShow("Restoring file \"" + file_path + "\" : attempt no. " + attempt + ".");
			for(int i = 0; i < restorers.size(); ++i) {
				if(!restorers.get(i).gotChunk())
					restorers.get(i).sendCommand();
			}
			initiateDelayedCheck();
		}
	}

	@Override
	public void terminate() {
		active = false;
		
		for(int i = 0; i < restorers.size(); ++i) {
			try {
				restorers.get(i).interrupt();
			} catch (Exception e) {
				// do nothing
			}
		}
		
		service.removeProcessor(this);
	}
	
	public void notifyReception(int chunk) {
		if(received_chunks.size() == number_of_chunks) {
			
			Collections.sort(received_chunks);

			try {

				File file = new File(file_path);
				if(file.getParentFile() != null) {
					file.getParentFile().mkdirs();
				}
				file.createNewFile();

				PrintWriter out_writer = new PrintWriter(new BufferedWriter(new FileWriter(file_path,false)));
				
				for(int i = 0; i < received_chunks.size(); ++i) {
					byte[] content = received_chunks.get(i).getchunkContent();
					out_writer.print(new String(content, 0, content.length));
				}
				out_writer.close();
			} catch (IOException e) {
				e.printStackTrace();
				service.logAndShowError("Unable to merge file chunks received.");
				if(response_socket != null) {
					try {
						SocketWrapper.sendTCP(response_socket, "" + EndCondition.FILE_MERGE_ERROR.ordinal());
					} catch (IOException e2) {
						e2.printStackTrace();
						service.showError("Unable to return error to TCP command sender");
					}
				}
			}
			
			if(response_socket != null) {
				try {
					SocketWrapper.sendTCP(response_socket, "" + EndCondition.SUCCESS.ordinal());
				} catch (IOException e) {
					e.printStackTrace();
					service.showError("Unable to confirm success to TCP command sender");
				}
			}
			
			service.logAndShow("File \"" + file_path + "\" successfully restored.");
			
			terminate();
		}
	}

}
