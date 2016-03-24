package filesystem.metadata;

import java.io.Serializable;

@SuppressWarnings("serial")
public class ChunkBackupInfo implements Serializable {
	
	private int num = -1;
	private int actual_replication = -1;
	private int min_replication = -1;
	private int size = -1;
	
	public ChunkBackupInfo(int num, int size, int min_replication, int actual_replication) {
		this.num = num;
		this.size = size;
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
	
	public int getSize() {
		return size;
	}
	
	public void setSize(int size) {
		this.size = size;
	}
	
	public String toString() {
		return "" + '\t' + '\t' + "Chunk #" + num + " size:" + size + " min_replication:" + min_replication + " replication:" + actual_replication;
	}
	
}
