package filesystem;

public class FileChunk implements Comparable {

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

	@Override
	public int compareTo(Object o) {
		if(o instanceof FileChunk) {
			FileChunk fc = (FileChunk) o;
			if(chunkNum < fc.chunkNum)
				return -1;
			if(chunkNum > fc.chunkNum)
				return 1;
		}
		
		return 0;
	}
}
