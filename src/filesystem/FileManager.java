package filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FileManager 
{
	
	//public static SplitFILE splitFile(String filename, int replicationNum, int chunkSize)
	
	public static SplitFile splitFile(String filename, int replicationNum, int chunkSize) throws IOException
	{
		int chunkSizeNew = chunkSize;
		
		// Falta gerar o fileId da SplitFile com base no filename. Alterar teste
		SplitFile splitFile = new SplitFile("teste");
		
		File file = new File(filename);
		
		FileInputStream readStream;
		int fileSize = (int)file.length();
		
		int fileSizeChunked = 0;
		int chunkPart = 0;
		byte[] chunkContentPart;
		
		readStream = new FileInputStream(file);
		
		while(fileSize > 0)
		{
			if(fileSize < chunkSizeNew)
				chunkSizeNew = fileSize;
			
			chunkContentPart = new byte[chunkSizeNew];
			fileSizeChunked = readStream.read(chunkContentPart, 0, chunkSizeNew);
			
			fileSize -= fileSizeChunked;
			chunkPart += 1;
			
			FileChunk chunk = new FileChunk(chunkPart, chunkContentPart, replicationNum);
			splitFile.getChunkList().add(chunk);
		}
		readStream.close();
		
		return splitFile;	
	}

}
