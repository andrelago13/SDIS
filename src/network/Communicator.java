package network;

import java.io.IOException;
import java.net.InetAddress;

public abstract class Communicator {
	
	public static MulticastSocketWrapper getMulticastSocket(String multicast_addr, int multicast_port) throws IOException {
		MulticastSocketWrapper socket = new MulticastSocketWrapper(multicast_port, multicast_addr);
		return socket;
	}
	
	public static MulticastSocketWrapper getMulticastSocket(InetAddress multicast_addr, int multicast_port) throws IOException {
		MulticastSocketWrapper socket = new MulticastSocketWrapper(multicast_port, multicast_addr);
		return socket;
	}
	
	public static DatagramSocketWrapper getSocket() throws IOException {
		DatagramSocketWrapper socket = new DatagramSocketWrapper();
		return socket;
	}
	
	public static DatagramSocketWrapper getSocket(int port) throws IOException {
		DatagramSocketWrapper socket = new DatagramSocketWrapper(port);
		return socket;
	}
	
}
