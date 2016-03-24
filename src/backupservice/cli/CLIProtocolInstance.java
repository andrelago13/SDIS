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
		// TODO fazer parsing
		// TODO faz throw de um IllegalArgumentException se mensagem não for comando do protocolo
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
