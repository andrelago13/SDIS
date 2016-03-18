package backupservice.cli;

import java.util.Scanner;

public class CommandLineInterface {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		Scanner sc = new Scanner(System.in);
		boolean run = true;
		while(run)
		{
			String in = sc.nextLine();
			String[] commandInfo = null;
			String command = checkCommand(in, commandInfo);

			switch(command)
			{
			case "BACKUP":
				// Execute backup protocol with commandInfo
				break;
			case "RESTORE":
				// Execute restore protocol with commandInfo
				break;
			case "RECLAIM":
				// Execute reclaim protocol with commandInfo
				break;
			case "DELETE":
				// Execute delete protocol with commandInfo
				break;
			case "ERROR":
				System.out.println("Invalid operation!!! Type -> java TestApp HELP\n for more help");
				break;
			case "EXIT":
				run = false;
				break;
			case "HELP":
				System.out.println("Protocols availables:\n\n" + "Backup file:\n Press:java TestApp local_accessPoint BACKUP file replicationDegree\n\n"
						+ "Restore file:\n Press:java TestApp local_accessPoint RESTORE file\n\n" 
						+ "Reclaim file:\n Press:java TestApp local_accessPoint RECLAIM space\n\n"
						+ "Delete file:\n Press:java TestApp local_accessPoint DELETE file\n\n"
						+ "exit:\n Press:java TestApp EXIT\n\n");
				break;
			}	
		}
        sc.close();
	}

	public static String checkCommand (String input, String[] inputInfo)
	{
		String[] inputParts = input.split(" ");
		String response = "";

		if(inputParts[0].equals("java") && inputParts[1].equals("TestApp"))
		{
			// TODO Change min and max replication degree of the file 
			if(inputParts[2].equals("BACKUP") && inputParts.length == 6 && utils.Files.fileValid(inputParts[5]) && Integer.parseInt(inputParts[6]) > 1 && Integer.parseInt(inputParts[6]) < 6)
			{
				response = "BACKUP";
				// file name to backup
				inputInfo[0] = inputParts[5];
				// replication degree
				inputInfo[1] = inputParts[6];
			}
			else if(inputParts[2].equals("RESTORE") && inputParts.length == 5)
			{
				response = "RESTORE";
				// file name to be restored
				inputInfo[0] = inputParts[3];
			}
			else if(inputParts[2].equals("RECLAIM") && inputParts.length == 5)
			{
				response = "RECLAIM";
				// space reclaimed
				inputInfo[0] = inputParts[3];
			}
			else if(inputParts[2].equals("DELETE") && inputParts.length == 5)
			{
				response = "DELETE";
				// file to be deleted
				inputInfo[0] = inputParts[3];
			}
			else if(inputParts[2].equals("HELP"))
				response = "HELP";
			else if(inputParts[2].equals("EXIT"))
				response = "EXIT";
		}
		else
		{
			response = "ERROR";
		}

		return response;
	}

}
