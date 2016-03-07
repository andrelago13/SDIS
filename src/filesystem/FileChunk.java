package filesystem;

public class FileChunk {

	private int chunkNum;
	private byte[] chunkContent;
	private int replicationNumber;
	
	public FileChunk(int chunkNum, byte[] chunkContent, int replicationNumber)
	{
		this.chunkNum = chunkNum;
		this.chunkContent = chunkContent;
		this.replicationNumber = replicationNumber;
	}
	
	public int getchunkNum()
	{
		return chunkNum;
	}
	
	public byte[] getchunkContent()
	{
		return chunkContent;
	}
	public int getreplicationNumber()
	{
		return replicationNumber;
	}
}
