package backupservice;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Timer;

import filesystem.metadata.MetadataManager;
import backupservice.cli.CLIProtocolInstance;
import backupservice.log.Logger;
import backupservice.log.LoggerInterface;
import backupservice.protocols.ProtocolInstance;
import backupservice.protocols.Protocols;
import backupservice.protocols.processors.ProtocolProcessor;
import backupservice.protocols.processors.ProtocolProcessorFactory;
import network.Communicator;
import network.MulticastSocketWrapper;
import network.ResponseGetterThread;
import network.ResponseHandler;
import network.TCPResponseHandler;

public class BackupService implements ResponseHandler, TCPResponseHandler, LoggerInterface {
	
	private final static int START_SOCKET_NO = 8080;
	
	public static final String BACKUP_FILE_PATH = "resources/backups/";
	
	private int identifier;
	
	private MetadataManager metadata = null;
	private Logger logger = null;
	
	private MulticastSocketWrapper socket_control = null;
	private MulticastSocketWrapper socket_backup = null;
	private MulticastSocketWrapper socket_restore = null;
	private ServerSocket own_socket = null;
	
	private ResponseGetterThread control_receiver_thread = null;
	private ResponseGetterThread backup_receiver_thread = null;
	private ResponseGetterThread restore_receiver_thread = null;
	private ResponseGetterThread command_receiver_thread = null;
	private Timer timer = null;
	
	private ArrayList<ProtocolProcessor> processors = null;
	
	public BackupService(int identifier, InetAddress control_address, int control_port, InetAddress backup_address, int backup_port, InetAddress restore_address, int restore_port) throws IllegalArgumentException, IOException {
		if(control_address == null || backup_address == null || restore_address == null || identifier < 0 || restore_port < 0) {
			throw new IllegalArgumentException("Invalid argument values.");
		}
		
		this.identifier = identifier;
		
		socket_control = Communicator.getMulticastSocket(control_address, control_port);
		socket_backup = Communicator.getMulticastSocket(backup_address, backup_port);
		socket_restore = Communicator.getMulticastSocket(restore_address, restore_port);
		
		initiateOwnSocket();
		processors = new ArrayList<ProtocolProcessor>();
		initiateMetadata();
		initiateLogger();
		initiateTimer();
	}
	
	public BackupService(int identifier, String control_address, int control_port, String backup_address, int backup_port, String restore_address, int restore_port) throws IllegalArgumentException, IOException {
		if(control_address == null || backup_address == null || restore_address == null || identifier < 0 || restore_port < 0) {
			throw new IllegalArgumentException("Invalid argument values.");
		}
		
		this.identifier = identifier;
		
		socket_control = Communicator.getMulticastSocket(control_address, control_port);
		socket_backup = Communicator.getMulticastSocket(backup_address, backup_port);
		socket_restore = Communicator.getMulticastSocket(restore_address, restore_port);
		
		initiateOwnSocket();
		processors = new ArrayList<ProtocolProcessor>();
		initiateMetadata();
		initiateLogger();
		initiateTimer();
	}
	
	private void initiateLogger() {
		try {
			logger = new Logger(identifier);
		} catch (IOException e) {
			e.printStackTrace();
			logAndShowError("Unable to initialize logger.");
		}
	}
	
	private void initiateTimer() {
		timer = new Timer();
	}
	
	private void initiateMetadata() {
		metadata = new MetadataManager(identifier);		
	}
	
	private void initiateOwnSocket() throws IOException {
		own_socket = new ServerSocket(START_SOCKET_NO + identifier);
	}
	
	public int getIdentifier() {
		return identifier;
	}
	
	public Timer getTimer() {
		return timer;
	}
	
	public MulticastSocketWrapper getControlSocket() {
		return socket_control;
	}
	
	public MulticastSocketWrapper getBackupSocket() {
		return socket_backup;
	}
	
	public MulticastSocketWrapper getRestoreSocket() {
		return socket_restore;
	}

	public void initiate() {
		logAndShow("Backup Service initializing...");
		
		// INITIALIZE THREAD OBJECTS
		control_receiver_thread = socket_control.multipleUsageResponseThread(this, this, Protocols.MAX_PACKET_LENGTH);
		backup_receiver_thread = socket_backup.multipleUsageResponseThread(this, this, Protocols.MAX_PACKET_LENGTH);
		restore_receiver_thread = socket_restore.multipleUsageResponseThread(this, this, Protocols.MAX_PACKET_LENGTH);
		command_receiver_thread = new ResponseGetterThread(this, this, own_socket, false);
		
		// START RUNNING THREADS
		control_receiver_thread.start();
		backup_receiver_thread.start();
		restore_receiver_thread.start();
		command_receiver_thread.start();
		
		try {
			logAndShow("Listening for commands at " + InetAddress.getLocalHost().getHostAddress() + ":" + own_socket.getLocalPort());
		} catch (UnknownHostException e) {
			logAndShow("Listening for commands at localhost:" + own_socket.getLocalPort());
		}
		logAndShow("CONTROL channel started at " + socket_control.getGroup().getHostAddress() + ":" + socket_control.getLocalPort());
		logAndShow("BACKUP channel started at " + socket_backup.getGroup().getHostAddress() + ":" + socket_backup.getLocalPort());
		logAndShow("RESTORE channel started at " + socket_restore.getGroup().getHostAddress() + ":" + socket_restore.getLocalPort());

		logAndShow("Backup Service initialized");
	}

	public void terminate() {
		logAndShow("Backup Service terminating...");
		
		timer.cancel();
		
		if(control_receiver_thread != null) {
			try{
				control_receiver_thread.interrupt();				
			} catch (Exception e) {
				// do nothing
			}
		}
		if(backup_receiver_thread != null) {
			try{
				backup_receiver_thread.interrupt();				
			} catch (Exception e) {
				// do nothing
			}
		}
		if(restore_receiver_thread != null) {
			try{
				restore_receiver_thread.interrupt();				
			} catch (Exception e) {
				// do nothing
			}
		}
		if(command_receiver_thread != null) {
			try{
				command_receiver_thread.interrupt();				
			} catch (Exception e) {
				// do nothing
			}
		}
		
		try {
			socket_control.dispose();
		} catch (IOException e) {
			e.printStackTrace();
			logAndShowError("Unable to dispose of CONTROL channel socket.");
		}
		try {
			socket_backup.dispose();
		} catch (IOException e) {
			e.printStackTrace();
			logAndShowError("Unable to dispose of BACKUP channel socket.");
		}
		try {
			socket_restore.dispose();
		} catch (IOException e) {
			e.printStackTrace();
			logAndShowError("Unable to dispose of RESTORE channel socket.");
		}
		try {
			own_socket.close();
		} catch (IOException e) {
			e.printStackTrace();
			logAndShowError("Unable to dispose of COMMAND channel socket.");
		}
		
		System.out.println(processors.size());
		for(int i = 0; i < processors.size(); ++i) {
			processors.get(i).terminate();
		}
		
		try {
			metadata.backup();
		} catch (IOException e) {
			e.printStackTrace();
			logAndShowError("Unable to backup metadata.");
		}

		logAndShow("Backup Service terminated.");
	}

	@Override
	public void handle(ResponseGetterThread sender, DatagramPacket response) {		
		if(sender == control_receiver_thread) {
			logAndShow("CONTROL channel received \"" + new String(response.getData(), 0, response.getLength()) + "\".");
		} else if(sender == backup_receiver_thread) {
			logAndShow("BACKUP channel received \"" + new String(response.getData(), 0, response.getLength()) + "\".");
		} else if(sender == restore_receiver_thread) {
			logAndShow("RESTORE channel received \"" + new String(response.getData(), 0, response.getLength()) + "\".");
		} else {
			logAndShow("Unknown UDP channel received \"" + new String(response.getData(), 0, response.getLength()) + "\".");			
		}
		
		Boolean handled = false;
		ProtocolInstance response_instance = Protocols.parseMessage(new String(response.getData(), 0, response.getLength()));
		if(response_instance == null) {
			logAndShow("Message received was not a valid Protocol message.");
			return;
		}
		
		if(response_instance.getHeader().getSender_id() == identifier)
			return;
		
		for(int i = 0; i < processors.size(); ++i) {
			if(processors.get(i).handle(response_instance)) {
				handled = true;
				logAndShow("Message received was handled by existing processor.");
				break;
			}
		}
		if(!handled) {
			ProtocolProcessor processor = ProtocolProcessorFactory.getProcessor(response_instance, this);
			if(processor != null) {
				logAndShow("Message received will be handled by a new processor.");
				addProcessor(processor);
				processor.initiate();
			} else {
				logAndShow("Message received does not trigger any new processor");				
			}
		}
	}

	@Override
	public void handle(ResponseGetterThread sender, String response, Socket connection_socket) {
		// FIXME remove after testing
		if(response.equals("EXIT")) {
			terminate();
			return;
		}
		
		if(sender == command_receiver_thread) {
			logAndShow("COMMAND channel received \"" + response + "\".");			
		} else {
			logAndShow("Unknown TCP channel received \"" + response + "\".");	
		}
		
		ProtocolProcessor processor = null;
		try {
			processor = ProtocolProcessorFactory.getProcessor(new CLIProtocolInstance(response), this, connection_socket);
		} catch (Exception e) {
			logAndShowError("Command received was invalid.");
			return;
		}
		if(processor != null) {
			logAndShow("Command received triggered a new processor.");
			processors.add(processor);
			processor.initiate();
		} else {
			logAndShowError("Command received does not trigger any new processor.");
			return;
		}
	}

	public int identifier() {
		return identifier;
	}

	public void addProcessor(ProtocolProcessor processor) {
		processors.add(processor);
	}
	
	public void removeProcessor(ProtocolProcessor processor) {
		if(processors.contains(processor))
			processors.remove(processor);
	}

	public MetadataManager getMetadata() {
		return metadata;
	}

	public void log(String message) {
		if(logger != null) {
			logger.log(message);
		}
	}

	public void logAndShow(String message) {
		if(logger != null) {
			logger.logAndShow(message);
		}
	}

	public void show(String message) {
		if(logger != null) {
			logger.show(message);
		}
	}

	public void logError(String message) {
		if(logger != null) {
			logger.logError(message);
		}
	}
	
	public void logAndShowError(String message) {
		if(logger != null) {
			logger.logAndShowError(message);
		}		
	}
	
	public void showError(String message) {
		if(logger != null) {
			logger.showError(message);
		}		
	}

}
