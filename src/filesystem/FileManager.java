package filesystem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class FileManager 
{
	
	//public static SplitFILE splitFile(String filename, int replicationNum, int chunkSize)
	
	public static SplitFile splitFile(String filename, int replicationNum, int chunkSize) throws IOException
	{
		// TODO Falta gerar o fileId da SplitFile com base no filename. Alterar teste
		SplitFile splitFile = new SplitFile("teste");
		
		File file = new File(filename);
		
		FileInputStream readStream = new FileInputStream(file);
		BufferedReader br = new BufferedReader(new InputStreamReader(readStream));
		int fileSize = (int)file.length();
		
		int chunkPart = 0;
		Boolean fileSizeMultiple = true;
		
		while(fileSize > 0)
		{
			char[] chunkContentPart = new char[chunkSize];
			int readBytes = br.read(chunkContentPart, 0, chunkSize);
			System.out.println(chunkContentPart);
			
			if(fileSize < chunkSize) {
				fileSizeMultiple = false;
			}
			
			fileSize -= readBytes;
			
			FileChunk chunk = new FileChunk(chunkPart, new String(chunkContentPart, 0, readBytes).getBytes(), replicationNum);
			splitFile.getChunkList().add(chunk);
			chunkPart++;
		}
		
		if(fileSizeMultiple) {
			splitFile.getChunkList().add(new FileChunk(chunkPart, new byte[0], replicationNum));
		}
		
		System.out.println(splitFile.getChunkList().size());
		
		readStream.close();
		
		return splitFile;	
	}

}
