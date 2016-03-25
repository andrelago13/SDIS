package filesystem.metadata;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

@SuppressWarnings("serial")
public class MetadataManager implements Serializable {
	
	private static final String METADATA_PATH = "resources/metadata/backup_metadata";
	private static MetadataManager instance = null;
	
	private ArrayList<FileBackupInfo> own_files = null;
	private ArrayList<FileBackupInfo> peer_files = null;

	public static MetadataManager getInstance() {
		if(instance == null) {
			try {
				FileInputStream fin = new FileInputStream(METADATA_PATH);
				ObjectInputStream ois = new ObjectInputStream(fin);
				instance = (MetadataManager) ois.readObject();
				ois.close();
			} catch (IOException | ClassNotFoundException e) {
				instance = new MetadataManager();
			}
		}
		
		return instance;
	}
	
	public static MetadataManager updateFromFile() throws IOException, ClassNotFoundException {
		FileInputStream fin = new FileInputStream(METADATA_PATH);
		ObjectInputStream ois = new ObjectInputStream(fin);
		instance = (MetadataManager) ois.readObject();
		ois.close();
		return instance;
	}
	
	public static MetadataManager resetInstance() {
		instance = new MetadataManager();
		return instance;
	}
	
	private MetadataManager() {
		own_files = new ArrayList<FileBackupInfo>();
		peer_files = new ArrayList<FileBackupInfo>();
	}
	
	public void backup() throws IOException {
		System.out.println(own_files.size());
		FileOutputStream fout = new FileOutputStream(METADATA_PATH);
		ObjectOutputStream oos = new ObjectOutputStream(fout);   
		oos.writeObject(instance);
		oos.close();
	}
	
	public ArrayList<FileBackupInfo> ownFilesInfo() {
		return own_files;
	}
	
	public ArrayList<FileBackupInfo> peerFilesInfo() {
		return peer_files;
	}

	public void updateOwnFile(String file_hash, int chunk_num, int chunk_min_replication, int chunk_replication, int chunk_size) {
		updateFile(own_files, file_hash, chunk_num, chunk_min_replication, chunk_replication, chunk_size);
	}
	
	public void updatePeerFile(String file_hash, int chunk_num, int chunk_min_replication, int chunk_replication, int chunk_size) {
		updateFile(peer_files, file_hash, chunk_num, chunk_min_replication, chunk_replication, chunk_size);
	}
	
	public void updateFile(ArrayList<FileBackupInfo> file_list, String file_hash, int chunk_num, int chunk_min_replication, int chunk_replication, int chunk_size) {
		for(int i = 0; i < file_list.size(); ++i) {
			if(file_list.get(i).getHash().equals(file_hash)) {
				file_list.get(i).updateChunk(chunk_num, chunk_size, chunk_min_replication, chunk_replication);
				return;
			}
		}
		
		FileBackupInfo file = new FileBackupInfo(file_hash);
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
		
		// TODO adicionar peer files
		
		return result;
	}
}
