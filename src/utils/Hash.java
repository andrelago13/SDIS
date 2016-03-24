package utils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.DatatypeConverter;

public class Hash {
	
	public static String hashFile (String filename, int owner, int replicationLevel) throws NoSuchAlgorithmException, UnsupportedEncodingException
	{
		String fileID = filename + ":" + Integer.toString(owner) + ":" + System.currentTimeMillis() + ":" + Integer.toString(replicationLevel);
		
		MessageDigest mD = MessageDigest.getInstance("SHA-256");
		mD.update(fileID.getBytes("UTF-8"));
		
		byte[] fileIDhashed = mD.digest();
		
		// printHexBinary converts a byte[] into string
		return new String(DatatypeConverter.printHexBinary(fileIDhashed));
		
	}

}
