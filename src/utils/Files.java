package utils;

import java.io.File;

public class Files {

	public static boolean fileValid(String filename)
	{
		File file = new File(filename);		
		if(file.exists())
			return true;
		else
			return false;
	}
}
