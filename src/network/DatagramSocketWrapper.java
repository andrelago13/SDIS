package network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class DatagramSocketWrapper extends DatagramSocket {

	public DatagramSocketWrapper() throws SocketException {
		super();
	}
	
	public DatagramSocketWrapper(int port) throws SocketException {
		super(port);
	}
	
	public void send(byte[] msg, int len, String address_name, int port) throws IOException {
		InetAddress addr = InetAddress.getByName(address_name);
		DatagramPacket packet = new DatagramPacket(msg, len, addr, port);
		send(packet);
	}
	
	public void send(byte[] msg, int len, InetAddress address, int port) throws IOException {
		DatagramPacket packet = new DatagramPacket(msg, len, address, port);
		send(packet);
	}

	public ResponseGetterThread singleUsageResponseThread(ResponseHandler handler, int max_length) {
		return new ResponseGetterThread(handler, this, max_length, true);
	}

	public ResponseGetterThread multipleUsageResponseThread(ResponseHandler handler, int max_length) {
		return new ResponseGetterThread(handler, this, max_length, false);
	}
}
