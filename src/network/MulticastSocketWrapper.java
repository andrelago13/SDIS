package network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class MulticastSocketWrapper extends MulticastSocket  {
	
	private InetAddress group = null;
	private int port = -1;

	public MulticastSocketWrapper(int port, String addr) throws IOException {
		super(port);
		group = InetAddress.getByName(addr);
		this.port = port;
		joinGroup(group);
	}

	public MulticastSocketWrapper(int port, InetAddress addr) throws IOException {
		super(port);
		group = addr;
		joinGroup(group);
	}
	
	public void dispose() throws IOException {
		leaveGroup(group);
		close();
	}

	public ResponseGetterThread singleUsageResponseThread(ResponseHandler handler, int max_length) {
		return new ResponseGetterThread(handler, this, max_length, true);
	}

	public ResponseGetterThread multipleUsageResponseThread(ResponseHandler handler, int max_length) {
		return new ResponseGetterThread(handler, this, max_length, false);
	}

	public void send(byte[] message, int length) throws IOException {
		send(new DatagramPacket(message, length, group, port));
	}

	public void send(String message) throws IOException {
		send(message.getBytes(), message.length());
	}
}
