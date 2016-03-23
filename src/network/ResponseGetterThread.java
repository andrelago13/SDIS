package network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;

public class ResponseGetterThread extends Thread {
	
	public static enum Type {
		DATAGRAM,
		TCP
	}
	
	private ResponseHandler datagram_handler = null;
	private DatagramSocket datagram_socket = null;
	private int buf_len = 0;

	private ServerSocket tcp_socket = null;
	private TCPResponseHandler tcp_handler = null;
	
	private Boolean single_usage = true;
	private Boolean enabled = false;
	
	private Type type = null;
	
	public ResponseGetterThread(ResponseHandler handler, DatagramSocket socket, int max_length) {
		this(handler, socket, max_length, true);
	}

	public ResponseGetterThread(ResponseHandler handler, DatagramSocket socket, int max_length, Boolean single_usage) {
		type = Type.DATAGRAM;
		
		this.datagram_handler = handler;
		this.datagram_socket = socket;
		buf_len = max_length;
		this.single_usage = single_usage;
	}

	public ResponseGetterThread(TCPResponseHandler handler, ServerSocket socket) {
		this(handler, socket, true);
	}

	public ResponseGetterThread(TCPResponseHandler handler, ServerSocket socket, Boolean single_usage) {
		type = Type.TCP;
		this.tcp_handler = handler;
		this.tcp_socket = socket;
		this.single_usage = single_usage;
	}
	
	@Override
	public void run() {
		if(type == Type.DATAGRAM)
			run_datagram();
		else if(type == Type.TCP)
			run_tcp();
	}
	
	private void run_datagram() {
		if(datagram_socket == null || datagram_handler == null || buf_len < 1 || enabled)
			return;
		
		System.out.println("Awaiting for datagram packets at port " + datagram_socket.getLocalPort());
		
		enabled = true;
		
		if(single_usage) {
			byte[] buf = new byte[buf_len];
			DatagramPacket packet = new DatagramPacket(buf, buf_len);
			
			try {
				datagram_socket.receive(packet);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			
			datagram_handler.handle(packet);
		} else {
			while(enabled) {
				byte[] buf = new byte[buf_len];
				DatagramPacket packet = new DatagramPacket(buf, buf_len);
				
				try {
					datagram_socket.receive(packet);
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
				
				if(enabled) {
					// Dispatches a new thread to avoid blocking upcoming messages
					new Thread( new Runnable() {
					    @Override
					    public void run() {
					    	datagram_handler.handle(packet);
					    }
					}).start();
				}
			}
		}
		
		System.out.println("Not listening to port " + datagram_socket.getLocalPort() + " anymore");
		
		enabled = false;
	}
	
	private void run_tcp() {
		if(tcp_socket == null || tcp_handler == null || enabled) {
			return;
		}
		
		System.out.println("Awaiting for TCP connections at port " + tcp_socket.getLocalPort());
		
		enabled = true;
		
		try {
			if(single_usage) {
				Socket connectionSocket = tcp_socket.accept();
				BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
				
				if(enabled) {
					String request = inFromClient.readLine();
					System.out.println("Received: \"" + request + "\"");
					
					tcp_handler.handle(request, connectionSocket);
				}				
			} else {
				while(enabled) {
					Socket connectionSocket = tcp_socket.accept();
					BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
					
					String request = inFromClient.readLine();
					
					if(!enabled) {
						connectionSocket.close();
						break;
					}

					System.out.println("Received: \"" + request + "\"");

					// Dispatches a new thread to avoid blocking upcoming messages
					new Thread( new Runnable() {
					    @Override
					    public void run() {
							tcp_handler.handle(request, connectionSocket);
					    }
					}).start();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("Not listening to port " + tcp_socket.getLocalPort() + " anymore");
		
		enabled = false;
	}
	
	public void interrupt() {
		enabled = false;
		super.interrupt();
	}
	
	public void disable() {
		enabled = false;
	}
	
	public Boolean enabled() {
		return enabled;
	}

	public Type type() {
		return type;
	}
	
}
