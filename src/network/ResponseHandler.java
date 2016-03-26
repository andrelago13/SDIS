package network;

import java.net.DatagramPacket;

public interface ResponseHandler {
	public void handle(ResponseGetterThread sender, DatagramPacket response);
}
