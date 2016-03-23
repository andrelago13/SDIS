package network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class ResponseGetterThread extends Thread {
	
	private ResponseHandler handler = null;
	private DatagramSocket socket = null;
	private int buf_len = 0;
	private Boolean single_usage = true;
	private Boolean enabled = false;
	
	public ResponseGetterThread(ResponseHandler handler, DatagramSocket socket, int max_length) {
		this(handler, socket, max_length, true);
	}

	public ResponseGetterThread(ResponseHandler handler, DatagramSocket socket, int max_length, Boolean single_usage) {
		this.handler = handler;
		this.socket = socket;
		buf_len = max_length;
		this.single_usage = single_usage;
	}

	@Override
	public void run() {
		System.out.println("Hello");
		
		if(socket == null || handler == null || buf_len < 1 || enabled)
			return;
		
		enabled = true;
		
		if(single_usage) {
			byte[] buf = new byte[buf_len];
			DatagramPacket packet = new DatagramPacket(buf, buf_len);
			
			try {
				socket.receive(packet);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			
			handler.handle(packet);
		} else {
			while(enabled) {
				byte[] buf = new byte[buf_len];
				DatagramPacket packet = new DatagramPacket(buf, buf_len);
				
				try {
					socket.receive(packet);
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
				
				if(enabled)
					handler.handle(packet);
			}
		}
		
		enabled = false;
	}
	
	public void interrupt() {
		enabled = false;
	}
	
	public Boolean enabled() {
		return enabled;
	}

}
