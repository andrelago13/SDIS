package backupservice.cli;

import java.util.Scanner;

/*
 * This Class (TestApp old version) is here to be used (maybe) later.
 */
public class CliTemp {
	
	public static void main(String[] args) {

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
				clearConsole();
				break;
			case "RESTORE":
				// Execute restore protocol with commandInfo
				clearConsole();
				break;
			case "RECLAIM":
				// Execute reclaim protocol with commandInfo
				clearConsole();
				break;
			case "DELETE":
				// Execute delete protocol with commandInfo
				clearConsole();
				break;
			case "ERROR":
				System.out.println("Invalid operation!!! Type -> java TestApp HELP\n for more help");
				clearConsole();
				break;
			case "EXIT":
				run = false;
				clearConsole();
				break;
			case "HELP":
				System.out.println("Protocols availables:\n\n" + "Backup file:\n Press:BACKUP file replicationDegree\n\n"
						+ "Restore file:\n Press:RESTORE file\n\n" 
						+ "Reclaim file:\n Press:RECLAIM space\n\n"
						+ "Delete file:\n Press:DELETE file\n\n"
						+ "exit:\n Press:EXIT\n\n");
				clearConsole();
				break;
			}	
		}
		sc.close();
	}

	public static String checkCommand (String input, String[] inputInfo)
	{
		String[] inputParts = input.split(" ");
		String response = "";


		if(inputParts[0].equals("BACKUP") && inputParts.length == 3 && utils.Files.fileValid(inputParts[1]) && Integer.parseInt(inputParts[2]) > 0 && Integer.parseInt(inputParts[2]) < 10)
		{
			response = "BACKUP";
			// file name to backup
			inputInfo[0] = inputParts[1];
			// replication degree
			inputInfo[1] = inputParts[2];
		}
		else if(inputParts[0].equals("RESTORE") && inputParts.length == 2)
		{
			response = "RESTORE";
			// file name to be restored
			inputInfo[0] = inputParts[1];
		}
		else if(inputParts[0].equals("RECLAIM") && inputParts.length == 2)
		{
			response = "RECLAIM";
			// space reclaimed
			inputInfo[0] = inputParts[1];
		}
		else if(inputParts[0].equals("DELETE") && inputParts.length == 2)
		{
			response = "DELETE";
			// file to be deleted
			inputInfo[0] = inputParts[1];
		}
		else if(inputParts[0].equals("HELP"))
			response = "HELP";
		else if(inputParts[0].equals("EXIT"))
			response = "EXIT";
		else
			response = "ERROR";

		return response;
	}
	
	public static void clearConsole()
	{
		int i = 0;
		for(;i< 30; i++)
			System.out.println();
	}

}
