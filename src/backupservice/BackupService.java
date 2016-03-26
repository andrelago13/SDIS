package backupservice;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import filesystem.metadata.MetadataManager;
import backupservice.cli.CLIProtocolInstance;
import backupservice.log.Logger;
import backupservice.protocols.ProtocolInstance;
import backupservice.protocols.Protocols;
import backupservice.protocols.processors.ProtocolProcessor;
import backupservice.protocols.processors.ProtocolProcessorFactory;
import network.Communicator;
import network.MulticastSocketWrapper;
import network.ResponseGetterThread;
import network.ResponseHandler;
import network.TCPResponseHandler;

public class BackupService implements ResponseHandler, TCPResponseHandler {
	
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
	}
	
	private void initiateLogger() {
		try {
			logger = new Logger(identifier);
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Unable to initialize logger");
		}
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
		// INITIALIZE THREAD OBJECTS
		control_receiver_thread = socket_control.multipleUsageResponseThread(this, Protocols.MAX_PACKET_LENGTH);
		backup_receiver_thread = socket_backup.multipleUsageResponseThread(this, Protocols.MAX_PACKET_LENGTH);
		restore_receiver_thread = socket_restore.multipleUsageResponseThread(this, Protocols.MAX_PACKET_LENGTH);
		command_receiver_thread = new ResponseGetterThread(this, own_socket, false);
		
		// START RUNNING THREADS
		control_receiver_thread.start();
		backup_receiver_thread.start();
		restore_receiver_thread.start();
		command_receiver_thread.start();
		
		try {
			System.out.println("Listening for commands at " + InetAddress.getLocalHost().getHostAddress() + ":" + own_socket.getLocalPort());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		System.out.println("CONTROL CHANNEL: " + socket_control.getGroup().getHostAddress() + ":" + socket_control.getLocalPort());
		System.out.println("BACKUP CHANNEL: " + socket_backup.getGroup().getHostAddress() + ":" + socket_control.getLocalPort());
		System.out.println("RESTORE CHANNEL: " + socket_restore.getGroup().getHostAddress() + ":" + socket_control.getLocalPort());
	}

	public void terminate() {
		if(control_receiver_thread != null)
			control_receiver_thread.interrupt();
		if(backup_receiver_thread != null)
			backup_receiver_thread.interrupt();
		if(restore_receiver_thread != null)
			restore_receiver_thread.interrupt();
		if(command_receiver_thread != null)
			command_receiver_thread.interrupt();
		
		try {
			socket_control.dispose();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Unable to dispose of CONTROL channel socket.");
		}
		try {
			socket_backup.dispose();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Unable to dispose of BACKUP channel socket.");
		}
		try {
			socket_restore.dispose();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Unable to dispose of RESTORE channel socket.");
		}
		try {
			own_socket.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Unable to dispose of COMMAND channel socket.");
		}
		
		for(int i = 0; i < processors.size(); ++i) {
			processors.get(i).terminate();
		}
		
		try {
			metadata.backup();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Unable to backup metadata.");
		}
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
			logAndShow("Unknown channel received \"" + new String(response.getData(), 0, response.getLength()) + "\".");			
		}
		
		Boolean handled = false;
		ProtocolInstance response_instance = Protocols.parseMessage(new String(response.getData(), 0, response.getLength()));
		if(response_instance == null) {
			logAndShow("Message received was not a valid Protocol message.");
			return;
		}
		
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
				logAndShow("Message received does not trigger any processor");				
			}
		}
	}

	@Override
	public void handle(ResponseGetterThread sender, String response, Socket connection_socket) {
		// TODO logs
		System.out.println("Handle TCP");
		System.out.println(response);
		
		ProtocolProcessor processor = null;
		try {
			processor = ProtocolProcessorFactory.getProcessor(new CLIProtocolInstance(response), this, connection_socket);
		} catch (Exception e) {
			System.err.println("Invalid TCP command received =>" + response);
			return;
		}
		if(processor != null) {
			processors.add(processor);
			processor.initiate();
		} else {
			System.err.println("Invalid TCP command received =>" + response);	
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
}
