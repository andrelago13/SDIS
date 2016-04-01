package filesystem.metadata;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;

@SuppressWarnings("serial")
public class MetadataManager implements Serializable {
	
	private static final String METADATA_PATH = "resources/metadata/backup_metadata";
	private int id;
	
	private ArrayList<FileBackupInfo> own_files = null;
	private ArrayList<FileBackupInfo> peer_files = null;
	private ArrayList<String> deleted_peer_files = null;

	public void getFromFile() {
		try {
			FileInputStream fin = new FileInputStream(getFullPath());
			ObjectInputStream ois = new ObjectInputStream(fin);
			MetadataManager file = (MetadataManager) ois.readObject();
			ois.close();
			
			this.own_files = file.own_files;
			this.peer_files = file.peer_files;
		} catch (IOException | ClassNotFoundException e) {
			// do nothing
		}
	}

	public String toFileFormat() {
		String result = "" + own_files.size() + " " + peer_files.size() + '\n';
		
		for(int i = 0; i < own_files.size(); ++i) {
			result += own_files.get(i).toFileFormat() + '\n';
		}
		result += "============" + '\n';
		for(int i = 0; i < peer_files.size(); ++i) {
			result += peer_files.get(i).toFileFormat() + '\n';
		}
		result += "============" + '\n';
		result += "" + deleted_peer_files.size() + '\n';
		for(int i = 0; i < deleted_peer_files.size(); ++i) {
			result += deleted_peer_files.get(i) + '\n';
		}
		
		return result;
	}

	public Boolean fromFile() {
		try (BufferedReader br = new BufferedReader(new FileReader(getFullPath()))) {
		    String line;
		    
		    if((line = br.readLine()) == null)
		    	return false;
		    
		    String[] lengths = line.split(" ");
		    int own_files_len = Integer.parseInt(lengths[0]);
		    int peer_files_len = Integer.parseInt(lengths[1]);
		    
		    for(int i = 0; i < own_files_len; ++i) {
		    	String file_path = br.readLine();
		    	if(file_path.equals(""))
		    		file_path = null;
		    	
		    	String file_hash = br.readLine();
		    	String number_chunks_str = br.readLine();
		    	int number_chunks = Integer.parseInt(number_chunks_str);
		    	
		    	FileBackupInfo file = new FileBackupInfo(file_path, file_hash);
		    	
		    	for(int j = 0; j < number_chunks; ++j) {
		    		line = br.readLine();
		    		String[] chunk_infos = line.split(" ");
		    		int chunk_no = Integer.parseInt(chunk_infos[0]);
		    		int chunk_actual_replication = Integer.parseInt(chunk_infos[1]);
		    		int chunk_desired_replication = Integer.parseInt(chunk_infos[2]);
		    		int chunk_size = Integer.parseInt(chunk_infos[3]);
		    		
		    		file.updateChunk(chunk_no, chunk_size, chunk_desired_replication, chunk_actual_replication);
		    	}
		    	
		    	own_files.add(file);
		    }
		    
		    line = br.readLine();

		    
		    for(int i = 0; i < peer_files_len; ++i) {
		    	String file_path = br.readLine();
		    	if(file_path.equals(""))
		    		file_path = null;
		    	
		    	String file_hash = br.readLine();
		    	String number_chunks_str = br.readLine();
		    	int number_chunks = Integer.parseInt(number_chunks_str);
		    	
		    	FileBackupInfo file = new FileBackupInfo(file_path, file_hash);
		    	
		    	for(int j = 0; j < number_chunks; ++j) {
		    		line = br.readLine();
		    		String[] chunk_infos = line.split(" ");
		    		int chunk_no = Integer.parseInt(chunk_infos[0]);
		    		int chunk_actual_replication = Integer.parseInt(chunk_infos[1]);
		    		int chunk_desired_replication = Integer.parseInt(chunk_infos[2]);
		    		int chunk_size = Integer.parseInt(chunk_infos[3]);
		    		
		    		file.updateChunk(chunk_no, chunk_size, chunk_desired_replication, chunk_actual_replication);
		    	}
		    	
		    	peer_files.add(file);
		    }
		    
		    line = br.readLine();
		    line = br.readLine();
		    
		    int deletedFilesLen = Integer.parseInt(line);
		    
		    for(int i = 0; i < deletedFilesLen; ++i) {
		    	deleted_peer_files.add(br.readLine());
		    }
		    
		} catch (Exception e) {
			return false;
		}
		
		return true;
	}
	
	public MetadataManager(int id) {
		this.id = id;
		own_files = new ArrayList<FileBackupInfo>();
		peer_files = new ArrayList<FileBackupInfo>();
		deleted_peer_files = new ArrayList<String>();
		fromFile();
	}
	
	public void backup() throws IOException {
		/*File f = new File(getFullPath());
		f.getParentFile().mkdirs();
		
		FileOutputStream fout = new FileOutputStream(getFullPath());
		ObjectOutputStream oos = new ObjectOutputStream(fout);   
		oos.writeObject(this);
		oos.close();*/
		
		String path = getFullPath();
		
		File f = new File(path);
		f.getParentFile().mkdirs();
		
		PrintWriter out = new PrintWriter(path);
		out.print(toFileFormat());
		out.close();
	}
	
	public String getFullPath() {
		return METADATA_PATH + "_" + id;
	}
	
	public ArrayList<FileBackupInfo> ownFilesInfo() {
		return own_files;
	}
	
	public ArrayList<FileBackupInfo> peerFilesInfo() {
		return peer_files;
	}

	public void updateOwnFile(String file_path, String file_hash, int chunk_num, int chunk_min_replication, int chunk_replication, int chunk_size) {
		updateFile(own_files, file_path, file_hash, chunk_num, chunk_min_replication, chunk_replication, chunk_size);
	}
	
	public void updatePeerFile(String file_hash, int chunk_num, int chunk_min_replication, int chunk_replication, int chunk_size) {
		updateFile(peer_files, null, file_hash, chunk_num, chunk_min_replication, chunk_replication, chunk_size);
	}
	
	private void updateFile(ArrayList<FileBackupInfo> file_list, String file_path, String file_hash, int chunk_num, int chunk_min_replication, int chunk_replication, int chunk_size) {
		for(int i = 0; i < file_list.size(); ++i) {
			if(file_list.get(i).getHash().equals(file_hash)) {
				file_list.get(i).setPath(file_path);
				file_list.get(i).updateChunk(chunk_num, chunk_size, chunk_min_replication, chunk_replication);
				return;
			}
		}
		
		FileBackupInfo file = new FileBackupInfo(file_path, file_hash);
		file.updateChunk(chunk_num, chunk_size, chunk_min_replication, chunk_replication);
		file_list.add(file);
	}

	public String toString() {
		String result = "";
		
		// Own files
		result += "==> Own files metadata" + '\n';
		for(int i = 0; i < own_files.size(); ++i) {
			result += own_files.get(i).toString() + '\n';
		}
		result += "### End of own files metadata" + '\n' + '\n';
		
		// Peer files
		result += "==> Peer files metadata" + '\n';
		for(int i = 0; i < peer_files.size(); ++i) {
			result += peer_files.get(i).toString() + '\n';
		}
		result += "### End of peer files metadata" + '\n' + '\n';
		
		return result;
	}

	public FileBackupInfo ownFileBackupInfo_hash(String file_hash) {
		for(int i = 0; i < own_files.size(); ++i) {
			FileBackupInfo info = own_files.get(i);
			if(info.getHash() != null && info.getHash().equals(file_hash)) {
				return info;
			}
		}
		return null;
	}

	public FileBackupInfo ownFileBackupInfo_path(String file_path) {
		for(int i = 0; i < own_files.size(); ++i) {
			FileBackupInfo info = own_files.get(i);
			if(info.getFilePath() != null && info.getFilePath().equals(file_path)) {
				return info;
			}
		}
		return null;
	}
	
	public FileBackupInfo peerFileBackupInfo(String file_hash) {
		for(int i = 0; i < peer_files.size(); ++i) {
			if(peer_files.get(i).getHash().equals(file_hash))
				return peer_files.get(i);
		}
		return null;
	}
	
	public ChunkBackupInfo peerChunkBackupInfo(String file_hash, int chunk_no) {
		FileBackupInfo file = peerFileBackupInfo(file_hash);
		if(file == null)
			return null;
		
		ArrayList<ChunkBackupInfo> chunks = file.getChunks();
		
		for(int i = 0; i < chunks.size(); ++i) {
			if(chunks.get(i).getNum() == chunk_no)
				return chunks.get(i);
		}
		
		return null;
	}

	public int getBackupSize() {
		int total_size = 0;
		
		for(int i = 0; i < peer_files.size(); ++i) {
			total_size += peer_files.get(i).totalSize();
		}
		
		return total_size;
	}

	public ChunkBackupInfo bestChunkToRemove() {
		ChunkBackupInfo best_chunk = null;
		
		for(int i = 0; i < peer_files.size(); ++i) {
			ChunkBackupInfo chunk = peer_files.get(i).bestChunkToRemove();
			if(chunk != null) {
				if((best_chunk == null) || ((chunk.getActualReplication() - chunk.getMinReplication()) > (best_chunk.getActualReplication() - best_chunk.getMinReplication()))) {
					best_chunk = chunk;
				}
			}
		}
		
		return best_chunk;
	}
	
	public ChunkBackupInfo bestChunkToRemove(ArrayList<ChunkBackupInfo> invalid_chunks) {
		ChunkBackupInfo best_chunk = null;
		
		for(int i = 0; i < peer_files.size(); ++i) {
			ChunkBackupInfo chunk = peer_files.get(i).bestChunkToRemove();
			if(chunk != null) {
				if((best_chunk == null) || ((chunk.getActualReplication() - chunk.getMinReplication()) > (best_chunk.getActualReplication() - best_chunk.getMinReplication()))) {
					if(invalid_chunks == null || !invalid_chunks.contains(chunk))
						best_chunk = chunk;
				}
			}
		}
		
		return best_chunk;		
	}

	public void removeChunk(ChunkBackupInfo chunk) {
		for(int i = 0; i < peer_files.size(); ++i) {
			FileBackupInfo file = peer_files.get(i);
			if(file.getHash().equals(chunk.getFileHash())) {
				file.removeChunk(chunk);
				if(file.getChunks().size() == 0) {
					peer_files.remove(i);
				}
				return;
			}
		}
	}

	/*
	 * returns true if chunk existed
	 */
	public Boolean decreasePeerChunkReplication(String file_hash, int chunk_num) {
		for(int i = 0; i < peer_files.size(); ++i) {
			if(peer_files.get(i).getHash().equals(file_hash)) {
				return peer_files.get(i).decreaseChunkReplication(chunk_num);
			}
		}
		
		return false;
	}

	public ArrayList<String> getDeletedPeerFiles() {
		return deleted_peer_files;
	}

	public void addDeletedFile(String hash) {
		if(!deleted_peer_files.contains(hash)) {
			deleted_peer_files.add(hash);
		}
	}
	
}
