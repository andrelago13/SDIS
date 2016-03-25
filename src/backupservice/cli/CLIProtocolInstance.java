package backupservice.cli;

public class CLIProtocolInstance {
	
	public static enum Type {
		BACKUP,
		RESTORE,
		DELETE,
		RECLAIM
	}
	
	private Type type = null;
	private String file_path = null;
	private int replication_deg = -1;	// Só preciso para BACKUP
	private int max_space = -1;

	public CLIProtocolInstance(String message) throws IllegalArgumentException {

		String[] messageParts = message.split(" ");
		
		int i = 0;
		for (; i < Type.values().length;i++)
			if(Type.values()[i].toString().equals(messageParts[0]))
				break;
		
		if(i == Type.values().length)
			throw new IllegalArgumentException("Invalid protocol command!");
		
		if(messageParts[0].equals("BACKUP"))
		{
			file_path = messageParts[1];
			replication_deg = Integer.parseInt(messageParts[2]);
		}
		else if(messageParts[0].equals("RESTORE"))
			file_path = messageParts[1];
		else if(messageParts[0].equals("DELETE"))
			file_path = messageParts[1];
		else if(messageParts[0].equals("RECLAIM"))
			max_space = Integer.parseInt(messageParts[1]);	
	}
	
	public Type type() {
		return type;
	}
	
	public String filePath() {
		return file_path;
	}
	
	public int replicationDegree() {
		return replication_deg;
	}
	
	public int maxDiskSpace() {
		return max_space;
	}
	
}
