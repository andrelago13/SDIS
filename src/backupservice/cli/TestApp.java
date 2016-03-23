package backupservice.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class TestApp {

	// TODO Local ip and port of tcp client

	public static String LocalIp;
	public static int LocalPort;

	public static void main(String[] args) throws UnknownHostException, IOException {

		String[] commandInfo = new String[args.length - 2];
		String command = checkCommand(args, commandInfo);

		Socket socket = new Socket(LocalIp, LocalPort);
		PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
		BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));

		String request = "";

		switch(command)
		{
		case "BACKUP":
			// Execute backup protocol with commandInfo
			request += "BACKUP " + commandInfo[0] + " " + commandInfo[1];
			break;
		case "RESTORE":
			// Execute restore protocol with commandInfo
			request += "RESTORE " + commandInfo[0];
			break;
		case "RECLAIM":
			// Execute reclaim protocol with commandInfo
			request += "RECLAIM " + commandInfo[0];
			break;
		case "DELETE":
			// Execute delete protocol with commandInfo
			request += "DELETE " + commandInfo[0];
			break;
		case "ERROR":
			System.out.println("Invalid operation!!! Type -> java TestApp HELP\n for more help");
			break;
		case "EXIT":
			break;
		case "HELP":
			System.out.println("Protocols availables:\n\n" + "Backup file:\n Press:BACKUP peer_ap file replicationDegree\n\n"
					+ "Restore file:\n Press:RESTORE peer_ap file\n\n" 
					+ "Reclaim file:\n Press:RECLAIM peer_ap space\n\n"
					+ "Delete file:\n Press:DELETE peer_ap file\n\n"
					+ "exit:\n Press:EXIT\n\n");
			break;
		}
		
		output.println(request);
		System.out.println("Request sent -> " + request);
		
		String resp = input.readLine();
		System.out.println("Response received -> " + resp);
		
		if(resp.equals("0"))
			System.out.println("Command successful!");
		else
			// resp has the error code
			System.err.println("Command unsuccessful (" + resp + ")");

		output.close();
		input.close();
	}

	public static String checkCommand (String[] args, String[] inputInfo)
	{
		String[] inputParts = new String[args.length];

		for(int i = 0; i < args.length; i++)
			inputParts[i] = args[i];

		if(inputParts[0] != null)
		{
			if(inputParts[0].contains(":"))
			{
				String parts[] = inputParts[0].split(":");
				LocalIp = parts[0];
				LocalPort = Integer.parseInt(parts[1]);
			}
			else
			{
				LocalIp = "localhost";
				LocalPort = Integer.parseInt(inputParts[0]);
			}
		}
		else
			System.err.println("You have to indicate the ip and port or just the port of the Tcp Client!");

		String response = "";

		// TODO Change min and max replication degree of the file 
		if(inputParts[1].equals("BACKUP") && inputParts.length == 4  && Integer.parseInt(inputParts[3]) > 1 && Integer.parseInt(inputParts[3]) < 6)
		{
			response = "BACKUP";
			// file name to backup
			inputInfo[0] = inputParts[2];
			// replication degree
			inputInfo[1] = inputParts[3];
		}
		else if(inputParts[1].equals("RESTORE") && inputParts.length == 3)
		{
			response = "RESTORE";
			// file name to be restored
			inputInfo[0] = inputParts[2];
		}
		else if(inputParts[1].equals("RECLAIM") && inputParts.length == 3)
		{
			response = "RECLAIM";
			// space reclaimed
			inputInfo[0] = inputParts[2];
		}
		else if(inputParts[1].equals("DELETE") && inputParts.length == 3)
		{
			response = "DELETE";
			// file to be deleted
			inputInfo[0] = inputParts[2];
		}
		else if(inputParts[1].equals("HELP"))
			response = "HELP";
		else if(inputParts[1].equals("EXIT"))
			response = "EXIT";
		else
			response = "ERROR";

		return response;
	}
}
