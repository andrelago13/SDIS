package test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;

import filesystem.FileChunk;
import filesystem.FileManager;
import filesystem.SplitFile;

public class Test {

	public static void main(String[] args) throws IOException {
		
		// Teste ao splitFile
		
		ArrayList<FileChunk> fc = new ArrayList<FileChunk>();
		fc.add(new FileChunk(1, new byte[0], 0));
		fc.add(new FileChunk(4, new byte[0], 0));
		fc.add(new FileChunk(2, new byte[0], 0));
		fc.add(new FileChunk(3, new byte[0], 0));
		
		for(int i = 0; i < fc.size(); ++i) {
			System.out.println(fc.get(i).getchunkNum());
		}

		Collections.sort(fc);
		
		for(int i = 0; i < fc.size(); ++i) {
			System.out.println(fc.get(i).getchunkNum());
		}
		
		System.out.println();
		
		// Teste ao joinChunkFile
		
		byte[] b = ("teste 0 ").getBytes();
		System.out.println(new String(b, Charset.forName("UTF-8")));
		
		byte[] b1 = ("teste 1 ").getBytes();
		System.out.println(new String(b1, Charset.forName("UTF-8")));
		
		byte[] b2 = ("teste 2 ").getBytes();
		System.out.println(new String(b2, Charset.forName("UTF-8")));
		 
		byte[] b3 = ("teste 3").getBytes();
		System.out.println(new String(b3, Charset.forName("UTF-8")));
		
		
		ArrayList<FileChunk> fc2 = new ArrayList<FileChunk>();
		fc2.add(new FileChunk(1, b, 0));
		fc2.add(new FileChunk(4, b1, 0));
		fc2.add(new FileChunk(2, b2, 0));
		fc2.add(new FileChunk(3, b3, 0));
		
		String filePath = "./resources/test_join";
		
		FileManager fm = new FileManager();
		fm.joinChunksToFile(fc2, filePath);
		
	}

}
