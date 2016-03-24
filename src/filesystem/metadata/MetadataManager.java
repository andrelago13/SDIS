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

	public static MetadataManager getInstance() throws IOException, ClassNotFoundException {
		if(instance == null) {
			FileInputStream fin = new FileInputStream(METADATA_PATH);
			ObjectInputStream ois = new ObjectInputStream(fin);
			instance = (MetadataManager) ois.readObject();
			ois.close();
		}
		
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
		FileOutputStream fout = new FileOutputStream(METADATA_PATH);
		ObjectOutputStream oos = new ObjectOutputStream(fout);   
		oos.writeObject(instance);
		oos.close();
	}
	
	public ArrayList<FileBackupInfo> ownFilesInfo() {
		return own_files;
	}
	
}
