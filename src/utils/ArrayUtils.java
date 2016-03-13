package utils;

public class ArrayUtils {
	
	public static byte[] appendArrays(byte[] a1, byte[] a2) {
		byte[] result = new byte[a1.length + a2.length];
		
		for(int i = 0; i < a1.length; ++i) {
			result[i] = a1[i];
		}
		
		int dif = a1.length;
		for(int i = 0; i < a2.length; ++i) {
			result[i+dif] = a2[i];
		}
		
		return result;
	}
}
