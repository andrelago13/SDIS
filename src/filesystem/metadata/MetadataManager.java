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
	
	private static final String METADATA_PATH = "resources/metadata";
	private static MetadataManager instance = null;
	
	private ArrayList<FileBackupInfo> own_files = null;

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

	public void updateOwnFile(String file_hash, int chunk_num, int chunk_min_replication, int chunk_replication, int chunk_size) {
		for(int i = 0; i < own_files.size(); ++i) {
			if(own_files.get(i).getHash().equals(file_hash)) {
				own_files.get(i).updateChunk(chunk_num, chunk_size, chunk_min_replication, chunk_replication);
				return;
			}
		}
		
		FileBackupInfo file = new FileBackupInfo(file_hash);
		file.updateChunk(chunk_num, chunk_size, chunk_min_replication, chunk_replication);
		own_files.add(file);
	}

	public String toString() {
		String result = "";
		
		// Own files
		result += "==> Own files metadata" + '\n';
		for(int i = 0; i < own_files.size(); ++i) {
			result += own_files.get(i).toString() + '\n';
		}
		result += "### End of own files metadata" + '\n' + '\n';
		
		return result;
	}
}
