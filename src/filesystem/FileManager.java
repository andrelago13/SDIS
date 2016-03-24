package filesystem;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;

public class FileManager 
{
	@SuppressWarnings("unchecked") @Deprecated
	public static SplitFile splitFile(String filename, int replicationNum, int chunkSize) throws IOException, NoSuchAlgorithmException
	{
		// TODO Falta gerar o fileId da SplitFile com base no filename. Alterar teste
		String fileIdHashed = utils.Hash.hashFile(filename, 0, replicationNum);
		SplitFile splitFile = new SplitFile(fileIdHashed);
		
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
		
		readStream.close();
		
		Collections.sort(splitFile.getChunkList());
		return splitFile;	
	}
	
	@SuppressWarnings("unchecked") 
	public static SplitFile splitFile(String filename, int owner, int replicationNum, int chunkSize) throws IOException, NoSuchAlgorithmException
	{
		// TODO Falta gerar o fileId da SplitFile com base no filename. Alterar teste
		String fileIdHashed = utils.Hash.hashFile(filename, owner, replicationNum);
		SplitFile splitFile = new SplitFile(fileIdHashed);
		
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
		
		readStream.close();
		
		Collections.sort(splitFile.getChunkList());
		return splitFile;	
	}
	
	public void joinChunksToFile(ArrayList<FileChunk> chunks, String filePath) throws IOException
	{	
		FileOutputStream fos = new FileOutputStream(filePath);
		try {
			for(int i = 0; i < chunks.size(); i++)
			fos.write(chunks.get(i).getchunkContent());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

}
