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
	
}
