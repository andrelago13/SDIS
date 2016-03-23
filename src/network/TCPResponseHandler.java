package network;

public interface TCPResponseHandler {
	public void handle(String response, SocketWrapper connection_socket);
}
