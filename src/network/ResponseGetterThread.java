package network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;

import backupservice.log.LoggerInterface;

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
	private LoggerInterface logger;
	
	private Boolean single_usage = true;
	private Boolean enabled = false;
	private Boolean line_mode = true;
	
	private Type type = null;
	
	public ResponseGetterThread(ResponseHandler handler, LoggerInterface logger, DatagramSocket socket, int max_length) {
		this(handler, logger, socket, max_length, true);
	}

	public ResponseGetterThread(ResponseHandler handler, LoggerInterface logger, DatagramSocket socket, int max_length, Boolean single_usage) {
		type = Type.DATAGRAM;
		
		this.logger = logger;
		this.datagram_handler = handler;
		this.datagram_socket = socket;
		buf_len = max_length;
		this.single_usage = single_usage;
	}

	public ResponseGetterThread(TCPResponseHandler handler, LoggerInterface logger, ServerSocket socket) {
		this(handler, logger, socket, true, true);
	}

	public ResponseGetterThread(TCPResponseHandler handler, LoggerInterface logger, ServerSocket socket, Boolean single_usage, Boolean line_mode) {
		type = Type.TCP;
		this.logger = logger;
		this.tcp_handler = handler;
		this.tcp_socket = socket;
		this.single_usage = single_usage;
		this.line_mode = line_mode;
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
			
			datagram_handler.handle(this, packet);
		} else {
			while(enabled) {
				byte[] buf = new byte[buf_len];
				DatagramPacket packet = new DatagramPacket(buf, buf_len);
				
				try {
					datagram_socket.receive(packet);
				} catch (IOException e) {
					//e.printStackTrace();
					logger.logAndShow("UDP port listening interrupted.");
					return;
				}
				
				if(enabled) {
					// Dispatches a new thread to avoid blocking upcoming messages
					final ResponseGetterThread t = this;
					new Thread( new Runnable() {
					    @Override
					    public void run() {
					    	datagram_handler.handle(t, packet);
					    }
					}).start();
				}
			}
		}
		
		logger.logAndShow("Not listening to UDP port " + datagram_socket.getLocalPort() + " anymore.");
		
		enabled = false;
	}
	
	private void run_tcp() {
		if(tcp_socket == null || tcp_handler == null || enabled) {
			return;
		}
		
		enabled = true;
		
		try {
			if(single_usage) {
				Socket connectionSocket = tcp_socket.accept();
				BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
				
				if(enabled) {
					String request = inFromClient.readLine();
					
					tcp_handler.handle(this, request, connectionSocket);
				}				
			} else {
				while(enabled) {
					try {
						Socket connectionSocket = tcp_socket.accept();
						BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
						
						String request = "";
						
						if(this.line_mode) {
							request = inFromClient.readLine();

							if(!enabled) {
								connectionSocket.close();
								break;
							}
						} else {
							char[] cbuf = new char[buf_len];
							int read_bytes = inFromClient.read(cbuf);
							if(read_bytes == -1) {
								connectionSocket.close();
								continue;
							}
							request = new String(cbuf, 0, read_bytes);

							if(!enabled) {
								connectionSocket.close();
								break;
							}							
						}
						
						final String sent = request;

						// Dispatches a new thread to avoid blocking upcoming messages
						final ResponseGetterThread t = this;
						new Thread( new Runnable() {
							@Override
							public void run() {
								tcp_handler.handle(t, sent, connectionSocket);
							}
						}).start();
					} catch (Exception e) {
						logger.logAndShow("TCP port " + tcp_socket.getLocalPort() + " listening interrupted.");
						return;
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		logger.logAndShow("Not listening to TCP port " + tcp_socket.getLocalPort() + " anymore.");
		
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
