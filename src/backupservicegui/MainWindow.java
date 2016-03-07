package backupservicegui;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import java.awt.BorderLayout;

import javax.swing.JButton;

import network.Communicator;
import network.DatagramSocketWrapper;
import network.MulticastSocketWrapper;
import network.ResponseHandler;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;

public class MainWindow implements ResponseHandler {

	private JFrame frame;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainWindow window = new MainWindow();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public MainWindow() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 271, 153);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		frame.setResizable(false);
		
		MulticastSocketWrapper socket = null;
		
		try {
			socket = Communicator.getMulticastSocket("224.0.0.1", 8888);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		JPanel panel = new JPanel();
		frame.getContentPane().add(panel, BorderLayout.CENTER);
		panel.setLayout(null);
		
		final MulticastSocketWrapper socket2 = socket;
		
		JButton btnNewButton = new JButton("Receiver");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//JOptionPane.showMessageDialog(null, "Button 1");
				byte[] buf = new byte[2000];
				DatagramPacket packet = new DatagramPacket(buf, 2000);
				try {
					socket2.receive(packet);
					System.out.println(new String(packet.getData(), 0, packet.getLength()));
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		btnNewButton.setBounds(10, 11, 235, 23);
		panel.add(btnNewButton);
		
		JButton btnButton = new JButton("Sender");
		btnButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					DatagramSocketWrapper socket3 = Communicator.getSocket();
					DatagramPacket packet2 = new DatagramPacket("abc".getBytes(), 3, InetAddress.getByName("224.0.0.1"), 8888);
					socket3.send(packet2);
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		btnButton.setBounds(10, 45, 235, 23);
		panel.add(btnButton);
		
		JButton btnButton_1 = new JButton("Button 3");
		btnButton_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(null, "Button 3");
			}
		});
		btnButton_1.setBounds(10, 79, 235, 23);
		panel.add(btnButton_1);
	}

	@Override
	public void handle(DatagramPacket response) {
		System.out.println(new String(response.getData(), 0, response.getLength()));		
	}
}
