package utils;

import java.io.File;

public class Files {

	public static boolean fileValid(String filename)
	{
		File file = new File(filename);		
		if(file.exists() && file.isFile())
			return true;
		else
			return false;
	}
	
	public static boolean folderValid(String filename)
	{
		File file = new File(filename);
		if(file.exists() && file.isDirectory())
			return true;
		else
			return false;
	}
	
	public static void removeFile(String path)
	{
		File file = new File(path);
		file.delete();
	}
}
