package filesystem.metadata;

import java.io.Serializable;
import java.util.ArrayList;

@SuppressWarnings("serial")
public class FileBackupInfo implements Serializable {

	private String hash = null;
	private ArrayList<ChunkBackupInfo> chunks = null;
	
	public FileBackupInfo(String hash) {
		this(hash, new ArrayList<ChunkBackupInfo>());
	}
	
	public FileBackupInfo(String hash, ArrayList<ChunkBackupInfo> chunks) {
		this.hash = hash;
		this.chunks = chunks;
	}
	
	public String getHash() {
		return hash;
	}
	
	public ArrayList<ChunkBackupInfo> getChunks() {
		return chunks;
	}
	
	public ChunkBackupInfo chunkByNum(int num) {
		for(int i = 0; i < chunks.size(); ++i) {
			if(chunks.get(i).getNum() == num)
				return chunks.get(i);
		}
		
		return null;
	}
	
	public int numChunks() {
		return chunks.size();
	}
	
	public void updateChunk(int chunk_num, int size, int min_replication, int replication) {
		for(int i = 0; i < chunks.size(); ++i) {
			if(chunks.get(i).getNum() == chunk_num) {
				ChunkBackupInfo chunk = chunks.get(i);
				chunk.setActualReplication(replication);
				chunk.setMinReplication(min_replication);
				chunk.setSize(size);
				
				return;
			}
		}
		
		chunks.add(new ChunkBackupInfo(chunk_num, size, min_replication, replication));
	}

	public String toString() {
		String result = "";
		result += '\t' + "File: " + hash + '\n' + '\t' + "Chunks:" + '\n';
		for(int i = 0; i < chunks.size(); ++i) {
			result += chunks.get(i).toString() + '\n';
		}
		return result;
	}

}
