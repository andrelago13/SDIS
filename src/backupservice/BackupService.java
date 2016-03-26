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
import backupservice.protocols.ProtocolInstance;
import backupservice.protocols.Protocols;
import backupservice.protocols.processors.BackupInitiator;
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
	}
	
	public void initiateMetadata() {
		metadata = MetadataManager.getInstance();		
	}
	
	public void initiateOwnSocket() throws IOException {
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
		
		/*// FIXME remove this after testing
		BackupInitiator t = new BackupInitiator(this, "resources/test_read.txt", 1, null);
		processors.add(t);
		t.initiate();*/
	}

	public void terminate() {
		try {
			socket_control.dispose();
			socket_backup.dispose();
			socket_restore.dispose();
			own_socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void handle(DatagramPacket response) {
		//System.out.println("Handle UDP");
		//System.out.println(new String(response.getData(), 0, response.getLength()));
		
		Boolean handled = false;
		ProtocolInstance response_instance = Protocols.parseMessage(new String(response.getData(), 0, response.getLength()));
		if(response_instance == null)
			return;
		
		for(int i = 0; i < processors.size(); ++i) {
			if(processors.get(i).handle(response_instance)) {
				handled = true;
				break;
			}
		}
		if(!handled) {
			ProtocolProcessor processor = ProtocolProcessorFactory.getProcessor(response_instance, this);
			if(processor != null) {
				addProcessor(processor);
				processor.initiate();
			}
		}
	}

	@Override
	public void handle(String response, Socket connection_socket) {
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
}
