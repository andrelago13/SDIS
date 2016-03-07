package filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

public class FileManager 
{
	private int ChunkSize;
	private ArrayList<ChunkFile> chunkFiles;
	
	public FileManager()
	{
		ChunkSize = 64000;
		chunkFiles = new ArrayList<ChunkFile>();
	}
	
	public int getChunkSize()
	{
		return ChunkSize;
	}
	public ArrayList<ChunkFile> getChunkFileList()
	{
		return chunkFiles;
	}
	
	public void setChunkSize(int ChunkSize)
	{
		this.ChunkSize = ChunkSize;
	}
	
	public void fileToChunk(String filename, int replicationNum) throws IOException
	{
		FileManager fileMan = new FileManager();
		
		File file = new File(filename);
		
		FileInputStream readStream;
		int fileSize = (int)file.length();
		
		int fileSizeChunked = 0;
		int chunkPart = 0;
		byte[] chunkContentPart;
		
		readStream = new FileInputStream(file);
		
		while(fileSize > 0)
		{
			if(fileSize < fileMan.getChunkSize())
				fileMan.setChunkSize(fileSize);
			
			chunkContentPart = new byte[fileMan.getChunkSize()];
			fileSizeChunked = readStream.read(chunkContentPart, 0, fileMan.getChunkSize());
			
			fileSize -= fileSizeChunked;
			chunkPart += 1;
			
			ChunkFile chunk = new ChunkFile(chunkPart, chunkContentPart, replicationNum);
			fileMan.getChunkFileList().add(chunk);
		}
		readStream.close();
		
	}

}
