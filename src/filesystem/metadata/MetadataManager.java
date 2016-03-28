package filesystem.metadata;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

@SuppressWarnings("serial")
public class MetadataManager implements Serializable {
	
	// TODO store metadata as text rather than object for easier reading and manipulation
	
	private static final String METADATA_PATH = "resources/metadata/backup_metadata";
	private int id;
	
	private ArrayList<FileBackupInfo> own_files = null;
	private ArrayList<FileBackupInfo> peer_files = null;

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

	
	public MetadataManager(int id) {
		this.id = id;
		own_files = new ArrayList<FileBackupInfo>();
		peer_files = new ArrayList<FileBackupInfo>();
		getFromFile();
	}
	
	public void backup() throws IOException {
		File f = new File(getFullPath());
		f.getParentFile().mkdirs();
		
		FileOutputStream fout = new FileOutputStream(getFullPath());
		ObjectOutputStream oos = new ObjectOutputStream(fout);   
		oos.writeObject(this);
		oos.close();
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
	
}
