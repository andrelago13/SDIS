package backupservice;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.ServerSocket;

import network.Communicator;
import network.MulticastSocketWrapper;
import network.ResponseGetterThread;
import network.ResponseHandler;

public class BackupService implements ResponseHandler {
	
	private final static int START_SOCKET_NO = 8080;
	
	private int identifier;
	
	private MulticastSocketWrapper socket_control = null;
	private MulticastSocketWrapper socket_backup = null;
	private MulticastSocketWrapper socket_restore = null;
	private ServerSocket own_socket = null;
	
	public BackupService(int identifier, InetAddress control_address, int control_port, InetAddress backup_address, int backup_port, InetAddress restore_address, int restore_port) throws IllegalArgumentException, IOException {
		if(control_address == null || backup_address == null || restore_address == null || identifier < 0 || restore_port < 0) {
			throw new IllegalArgumentException("Invalid argument values.");
		}
		
		this.identifier = identifier;
		
		socket_control = Communicator.getMulticastSocket(control_address, control_port);
		socket_backup = Communicator.getMulticastSocket(backup_address, backup_port);
		socket_restore = Communicator.getMulticastSocket(restore_address, restore_port);
	}
	
	public BackupService(int identifier, String control_address, int control_port, String backup_address, int backup_port, String restore_address, int restore_port) throws IllegalArgumentException, IOException {
		if(control_address == null || backup_address == null || restore_address == null || identifier < 0 || restore_port < 0) {
			throw new IllegalArgumentException("Invalid argument values.");
		}
		
		this.identifier = identifier;
		
		socket_control = Communicator.getMulticastSocket(control_address, control_port);
		socket_backup = Communicator.getMulticastSocket(backup_address, backup_port);
		socket_restore = Communicator.getMulticastSocket(restore_address, restore_port);
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
		ResponseGetterThread t1 = socket_control.multipleUsageResponseThread(this, 1000);
		t1.start();
	}

	public void terminate() {
		try {
			socket_control.dispose();
			socket_backup.dispose();
			socket_restore.dispose();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void handle(DatagramPacket response) {
		// TODO Auto-generated method stub
		System.out.println("Handle");
		System.out.println(new String(response.getData(), 0, response.getLength()));
	}

}
