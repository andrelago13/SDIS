package filesystem.metadata;

import java.io.Serializable;

@SuppressWarnings("serial")
public class ChunkBackupInfo implements Serializable {
	
	private int num = -1;
	private int actual_replication = -1;
	private int min_replication = -1;
	
	public ChunkBackupInfo(int num, int min_replication, int actual_replication) {
		this.num = num;
		this.min_replication = min_replication;
		this.actual_replication = actual_replication;
	}

	public int getNum() {
		return num;
	}

	public void setNum(int num) {
		this.num = num;
	}

	public int getActualReplication() {
		return actual_replication;
	}

	public void setActualReplication(int actual_replication) {
		this.actual_replication = actual_replication;
	}

	public int getMinReplication() {
		return min_replication;
	}

	public void setMinReplication(int min_replication) {
		this.min_replication = min_replication;
	}
	
}
