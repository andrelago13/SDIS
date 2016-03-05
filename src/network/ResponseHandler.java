package network;

import java.net.DatagramPacket;

public interface ResponseHandler {
	public void handle(DatagramPacket response);
}
