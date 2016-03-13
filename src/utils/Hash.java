package utils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.DatatypeConverter;

public class Hash {
	
	public static String hashFile (String filename) throws NoSuchAlgorithmException, UnsupportedEncodingException
	{
		MessageDigest mD = MessageDigest.getInstance("SHA-256");
		mD.update(filename.getBytes("UTF-8"));
		
		byte[] filenamehashed = mD.digest();
		
		// printHexBinary convertes a byte[] into string
		return new String(DatatypeConverter.printHexBinary(filenamehashed));
		
	}

}
