package filesystem.metadata;

import java.io.Serializable;
import java.util.ArrayList;

@SuppressWarnings("serial")
public class FileBackupInfo implements Serializable {

	private String file_path = null;
	private String hash = null;
	private ArrayList<ChunkBackupInfo> chunks = null;
	
	public FileBackupInfo(String path, String hash) {
		this(path, hash, new ArrayList<ChunkBackupInfo>());
	}
	
	public FileBackupInfo(String path, String hash, ArrayList<ChunkBackupInfo> chunks) {
		this.file_path = path;
		this.hash = hash;
		this.chunks = chunks;
	}
	
	public String getHash() {
		return hash;
	}
	
	public String getFilePath() {
		return file_path;
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
				chunk.setFile(this);
				return;
			}
		}
		
		ChunkBackupInfo c = new ChunkBackupInfo(chunk_num, size, min_replication, replication, this);
		chunks.add(c);
	}

	public String toString() {
		String result = "";
		result += '\t' + "File: " + file_path + " [" + hash + "]" + '\n' + '\t' + "Chunks:" + '\n';
		for(int i = 0; i < chunks.size(); ++i) {
			result += chunks.get(i).toString() + '\n';
		}
		return result;
	}

	public int totalSize() {
		int total_size = 0;
		
		for(int i = 0; i < chunks.size(); ++i) {
			total_size += chunks.get(i).getSize();
		}
		
		return total_size;
	}

	public ChunkBackupInfo bestChunkToRemove() {
		ChunkBackupInfo best_chunk = null;
		
		for(int i = 0; i < chunks.size(); ++i) {
			ChunkBackupInfo chunk = chunks.get(i);
			if(chunk != null) {
				if((best_chunk == null) || ((best_chunk.getActualReplication() - best_chunk.getMinReplication()) > (chunk.getActualReplication() - chunk.getMinReplication()))) {
					best_chunk = chunk;
				}
			}
		}
		
		return best_chunk;
	}

	public void removeChunk(ChunkBackupInfo chunk) {
		for(int i = 0; i < chunks.size(); ++i) {
			ChunkBackupInfo chunk_t = chunks.get(i);
			if(chunk_t.getNum() == chunk.getNum()) {
				chunks.remove(i);
				return;
			}
		}
	}

	public Boolean decreaseChunkReplication(int chunk_num) {
		for(int i = 0; i < chunks.size(); ++i) {
			ChunkBackupInfo chunk = chunks.get(i);
			if(chunk.getNum() == chunk_num) {
				if(chunk.getActualReplication() > 0)
					chunk.setActualReplication(chunk.getActualReplication() - 1);
				return true;
			}
		}
		
		return false;
	}

	public String toFileFormat() {
		String result = "";
		
		if(file_path == null)
			result += '\n';
		else
			result += file_path + '\n';
		
		if(hash == null)
			result += '\n';
		else
			result += hash + '\n';
		
		result += "" + chunks.size() + '\n';
		
		for(int i = 0; i < chunks.size(); ++i) {
			result += chunks.get(i).toFileFormat();
			if(i < chunks.size()-1) {
				result += '\n';
			}
		}
			
		return result;
	}

	public void setPath(String file_path) {
		this.file_path = file_path;
	}
}
