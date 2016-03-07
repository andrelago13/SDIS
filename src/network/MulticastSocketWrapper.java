package network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class MulticastSocketWrapper extends MulticastSocket  {
	
	private InetAddress group = null;

	public MulticastSocketWrapper(int port, String addr) throws IOException {
		super(port);
		group = InetAddress.getByName(addr);
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

}
