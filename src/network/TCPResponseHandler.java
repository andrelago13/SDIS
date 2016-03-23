package network;

import java.net.Socket;

public interface TCPResponseHandler {
	public void handle(String response, Socket connection_socket);
}
