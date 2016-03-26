package network;

import java.net.Socket;

public interface TCPResponseHandler {
	public void handle(ResponseGetterThread sender, String response, Socket connection_socket);
}
