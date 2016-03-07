package test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import filesystem.FileChunk;
import filesystem.FileManager;
import filesystem.SplitFile;

public class Test {

	public static void main(String[] args) {
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
		
	}

}
