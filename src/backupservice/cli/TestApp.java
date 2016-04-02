package backupservice.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import backupservice.BackupService;
import backupservice.protocols.processors.BackupInitiator;
import backupservice.protocols.processors.DeleteInitiator;
import backupservice.protocols.processors.ReclaimInitiator;
import backupservice.protocols.processors.RestoreInitiator;

public class TestApp {

	public static String LocalIp;
	public static int LocalPort;

	public static void main(String[] args){

		String[] commandInfo = new String[args.length - 2];
		String command = checkCommand(args, commandInfo);

		Socket socket = null;
		try {
			socket = new Socket(LocalIp, LocalPort);
		} catch (IOException e) {
			System.out.println("Cannot create socket!!");
			e.printStackTrace();
			return;
		}
		PrintWriter output = null;
		try {
			output = new PrintWriter(socket.getOutputStream(), true);
		} catch (IOException e) {
			System.out.println("Cannot create PrintWriter output!!");
			e.printStackTrace();
			return;
		}
		BufferedReader input = null;
		try {
			input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} catch (IOException e) {
			System.out.println("Cannot create BufferedReader input!!");
			e.printStackTrace();
			return;
		}

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

		System.out.println(request);
		String request_backup = new String(request.getBytes());
		
		if(!request.equals(""))
		{
			output.println(request_backup);
			System.out.println("Request sent -> " + request);

			String resp = "";
			try {
				resp = input.readLine();
			} catch (IOException e) {
				System.out.println("Cannot read resp from BufferedReader input!!");
				e.printStackTrace();
			}
			System.out.println("Response received -> " + resp);

			if(!resp.equals(""))
			{
				int pos = Integer.parseInt(resp);
				switch(command)
				{
				case "BACKUP":
					if(resp.equals("0"))
						System.out.println("Command successful!");
					else if(resp.equals("1"))
						System.out.println("Command partially successful! Not enough replication");
					else if(resp.equals("2"))
						System.out.println("Command unsuccessful (error: " + BackupInitiator.EndCondition.values()[pos] + ")");
					else if(resp.equals("3"))
						System.out.println("Command unsuccessful (error: " + BackupInitiator.EndCondition.values()[pos] + ")");
					else if(resp.equals("4"))
						System.out.println("Command unsuccessful (error: " + BackupInitiator.EndCondition.values()[pos] + ")");
					else if(resp.equals("5"))
						System.out.println("Command unsuccessful (error: " + BackupInitiator.EndCondition.values()[pos] + ")");
					break;
				case "RESTORE":
					if(resp.equals("0"))
						System.out.println("Command successful!");
					else if(resp.equals("1"))
						System.out.println("Command unsuccessful (error: " + RestoreInitiator.EndCondition.values()[pos] + ")");
					else if(resp.equals("2"))
						System.out.println("Command unsuccessful (error: " + RestoreInitiator.EndCondition.values()[pos] + ")");
					else if(resp.equals("3"))
						System.out.println("Command unsuccessful (error: " + RestoreInitiator.EndCondition.values()[pos] + ")");
					else if(resp.equals("4"))
						System.out.println("Command unsuccessful (error: " + RestoreInitiator.EndCondition.values()[pos] + ")");
					break;
				case "RECLAIM":
					if(resp.equals("0"))
						System.out.println("Command successful!");
					else if(resp.equals("1"))
						System.out.println("Command unsuccessful (error: " + ReclaimInitiator.EndCondition.values()[pos] + ")");
					else if(resp.equals("2"))
						System.out.println("Command unsuccessful (error: " + ReclaimInitiator.EndCondition.values()[pos] + ")");
					else if(resp.equals("3"))
						System.out.println("Command unsuccessful (error: " + ReclaimInitiator.EndCondition.values()[pos] + ")");
					break;
				case "DELETE":
					if(resp.equals("0"))
						System.out.println("Command successful!");
					else if(resp.equals("1"))
						System.out.println("Command unsuccessful (error: " + DeleteInitiator.EndCondition.values()[pos] + ")");
					else if(resp.equals("2"))
						System.out.println("Command unsuccessful (error: " + DeleteInitiator.EndCondition.values()[pos] + ")");
					else if(resp.equals("3"))
						System.out.println("Command unsuccessful (error: " + DeleteInitiator.EndCondition.values()[pos] + ")");
					break;
				}
			}
			else
				System.out.println("Didn't receive any response from server!");
		}
		else
			System.out.println("You didn't select any operating command!");

		output.close();
		try {
			input.close();
		} catch (IOException e) {			
			System.out.println("Cannot close BufferedReader input!!");
			e.printStackTrace();
		}
		try {
			socket.close();
		} catch (IOException e) {
			System.out.println("Cannot close socket!!");
			e.printStackTrace();
		}

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
			System.out.println("You have to indicate the ip and port or just the port of the Tcp Client!");

		String response = "";

		if(inputParts[1].equals("BACKUP") && inputParts.length == 4  && Integer.parseInt(inputParts[3]) >= 1 && Integer.parseInt(inputParts[3]) <= 9)
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
