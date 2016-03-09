package backupservice;

import java.io.IOException;
import java.net.InetAddress;

import network.Communicator;
import network.MulticastSocketWrapper;

public class BackupService {
	// TODO criar isto e classe de Port Listener
	
	private int identifier;
	
	private MulticastSocketWrapper socket_control = null;
	private MulticastSocketWrapper socket_backup = null;
	private MulticastSocketWrapper socket_restore = null;
	
	public BackupService(int identifier, InetAddress control_address, int control_port, InetAddress backup_address, int backup_port, InetAddress restore_address, int restore_port) throws IllegalArgumentException, IOException {
		if(control_address == null || backup_address == null || restore_address == null || identifier < 0 || restore_port < 0) {
			throw new IllegalArgumentException("Invalid argument values.");
		}
		
		this.identifier = identifier;
		
		socket_control = Communicator.getMulticastSocket(control_address, control_port);
		socket_backup = Communicator.getMulticastSocket(backup_address, backup_port);
		socket_restore = Communicator.getMulticastSocket(restore_address, restore_port);
		
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
}
