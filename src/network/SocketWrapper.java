package network;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class SocketWrapper extends Socket {
	
	public void send(String message) throws IOException {
		DataOutputStream outToClient = new DataOutputStream(this.getOutputStream());
		outToClient.writeBytes(message);
	}
	
}
