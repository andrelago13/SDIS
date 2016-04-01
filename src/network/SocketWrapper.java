package network;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class SocketWrapper {
	
	public static void sendTCP(Socket socket, String message) throws IOException {
		sendTCP(socket, message, true);
	}
	
	public static void sendTCP(Socket socket, String message, Boolean close) throws IOException {
		DataOutputStream outToClient = new DataOutputStream(socket.getOutputStream());
		outToClient.writeBytes(message);
		if(close)
			outToClient.close();		
	}
	
	public static void sendTCP(Socket socket, byte[] message) throws IOException {
		sendTCP(socket, message, true);
	}
	
	public static void sendTCP(Socket socket, byte[] message, Boolean close) throws IOException {
		DataOutputStream outToClient = new DataOutputStream(socket.getOutputStream());
		outToClient.write(message);
		if(close)
			outToClient.close();
		
	}
	
}
