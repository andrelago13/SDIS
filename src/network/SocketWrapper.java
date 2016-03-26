package network;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class SocketWrapper {
	
	public static void sendTCP(Socket socket, String message) throws IOException {
		DataOutputStream outToClient = new DataOutputStream(socket.getOutputStream());
		outToClient.writeBytes(message);
		outToClient.close();
	}
	
}
