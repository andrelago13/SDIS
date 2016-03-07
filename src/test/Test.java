package test;

import java.io.IOException;
import java.util.ArrayList;

import filesystem.FileChunk;
import filesystem.FileManager;
import filesystem.SplitFile;

public class Test {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			SplitFile f = FileManager.splitFile("resources/test_read.txt", 1, 2);
			ArrayList<FileChunk> fc = f.getChunkList();
			System.out.println("Read file");
			for(int i = 0; i < fc.size(); ++i) {
				System.out.println(new String(fc.get(i).getchunkContent()));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
