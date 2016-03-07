package filesystem;

import java.util.ArrayList;

public class SplitFile 
{
	private String fileId;
	ArrayList<FileChunk> chunkList;
	
	public SplitFile(String fileId)
	{
		this.fileId = fileId;
		chunkList = new ArrayList<FileChunk>(); 
	}
	
	public ArrayList<FileChunk> getChunkList()
	{
		return chunkList;
	}
	public String getFileId()
	{
		return fileId;
	}
	
}
