package backupservice.protocols;

import utils.ArrayUtils;

public class Protocols {

	// Multicast channels: MC (control), MDB (backup data), MDR (restore data)

	public static final char CR = 0xD;
	public static final char LF = 0xA;

	public static final String LINE_SEPARATOR = "" + CR + LF;
	public static final char FIELD_SEPARATOR =  ' ';
	
	public static final int PROTOCOL_VERSION_MAJOR = 1;
	public static final int PROTOCOL_VERSION_MINOR = 0;


	// <MessageType> <Version> <SenderId> <FileId> <ChunkNo> <ReplicationDeg> <CRLF>
	/*
	 * <MessageType> 	- type of the message
	 * <Version> 		- protocol version, like <n>'.'<m> where <n> and <m> are the ASCII characters (this one is 1.0)
	 * <SenderId> 		- sender's id as ASCII digits 12 -> '1''2'
	 * <FileId> 		- 256bits (32bytes) encoded as 64 ASCII chars. It's encoded transforming each byte into it's hex value (like '6E') and then 
	 * 						replacing it by the corresponding chars ('6' and 'E'). Big-endian.
	 * <ChunkNo> 		- ASCII chars representing the decimal value (starts in zero). No máx. 6 chars. Limita o ficheiro a 64GB
	 * <ReplicationDeg>	- one ASCII char representing the decimal number, so the max is 9
	 */

	public static enum MessageType {

		// Chunk Backup Protocol
		PUTCHUNK,
		STORED,

		// Chunk Restore Protocol
		GETCHUNK,
		CHUNK,

		// File Deletion Protocol
		DELETE,

		// Space Reclaiming Protocol
		REMOVED

	}
	
	public static ProtocolInstance putChunkProtocolInstance(int version_major, int version_minor, int sender_id, String file_id, int chunk_no, int replication_deg, byte[] content) throws IllegalArgumentException {
		ProtocolHeader header = new ProtocolHeader(MessageType.PUTCHUNK, version_major, version_minor, sender_id, file_id, chunk_no, replication_deg);
		ProtocolBody body = new ProtocolBody(content);
		return new ProtocolInstance(header, body);
	}
	
	public static ProtocolInstance storedProtocolInstance(int version_major, int version_minor, int sender_id, String file_id, int chunk_no) throws IllegalArgumentException {
		ProtocolHeader header = new ProtocolHeader(MessageType.STORED, version_major, version_minor, sender_id, file_id, chunk_no);
		return new ProtocolInstance(header);
	}
	
	public static ProtocolInstance getchunkProtocolInstance(int version_major, int version_minor, int sender_id, String file_id, int chunk_no) throws IllegalArgumentException {
		ProtocolHeader header = new ProtocolHeader(MessageType.GETCHUNK, version_major, version_minor, sender_id, file_id, chunk_no);
		return new ProtocolInstance(header);
	}
	
	public static ProtocolInstance chunkProtocolInstance(int version_major, int version_minor, int sender_id, String file_id, int chunk_no, byte[] content) throws IllegalArgumentException {
		ProtocolHeader header = new ProtocolHeader(MessageType.CHUNK, version_major, version_minor, sender_id, file_id, chunk_no);
		ProtocolBody body = new ProtocolBody(content);
		return new ProtocolInstance(header, body);
	}
	
	public static ProtocolInstance deleteProtocolInstance(int version_major, int version_minor, int sender_id, String file_id) throws IllegalArgumentException {
		ProtocolHeader header = new ProtocolHeader(MessageType.DELETE, version_major, version_minor, sender_id, file_id);
		return new ProtocolInstance(header);
	}
	
	public static ProtocolInstance removedProtocolInstance(int version_major, int version_minor, int sender_id, String file_id, int chunk_no) throws IllegalArgumentException {
		ProtocolHeader header = new ProtocolHeader(MessageType.REMOVED, version_major, version_minor, sender_id, file_id, chunk_no);
		return new ProtocolInstance(header);
	}

	public static ProtocolInstance parseMessage(byte[] message) throws IllegalArgumentException {
		String[] tokens = (new String(message)).split("\\s+");
		
		if(tokens.length == 0) {
			throw new IllegalArgumentException("Invalid message buffer (empty buffer).");
		}
		
		String message_type = tokens[0];
		
		if(message_type.equals(MessageType.PUTCHUNK.toString())) {
			if(tokens.length != 7)
				throw new IllegalArgumentException("Invalid message buffer (PUTCHUNK expects 6 aditional tokens).");
			
			String[] version_tokens = tokens[1].split(".");
			
			ProtocolHeader header = new ProtocolHeader(MessageType.PUTCHUNK, Integer.parseInt(version_tokens[0]), Integer.parseInt(version_tokens[1]), Integer.parseInt(tokens[2]), tokens[3], Integer.parseInt(tokens[4]), Integer.parseInt(tokens[5]));
			ProtocolBody body = new ProtocolBody(tokens[6].getBytes());
			
			return new ProtocolInstance(header, body);
		} else if(message_type.equals(MessageType.STORED.toString())) {
			if(tokens.length != 5)
				throw new IllegalArgumentException("Invalid message buffer (STORED expects 4 aditional tokens).");
			
			String[] version_tokens = tokens[1].split(".");
			
			ProtocolHeader header = new ProtocolHeader(MessageType.STORED, Integer.parseInt(version_tokens[0]), Integer.parseInt(version_tokens[1]), Integer.parseInt(tokens[2]), tokens[3], Integer.parseInt(tokens[4]));
			
			return new ProtocolInstance(header);
		} else if(message_type.equals(MessageType.GETCHUNK.toString())) {
			if(tokens.length != 5)
				throw new IllegalArgumentException("Invalid message buffer (GETCHUNK expects 4 aditional tokens).");
			
			String[] version_tokens = tokens[1].split(".");
			
			ProtocolHeader header = new ProtocolHeader(MessageType.GETCHUNK, Integer.parseInt(version_tokens[0]), Integer.parseInt(version_tokens[1]), Integer.parseInt(tokens[2]), tokens[3], Integer.parseInt(tokens[4]));			
		
			return new ProtocolInstance(header);
		} else if(message_type.equals(MessageType.CHUNK.toString())) {
			if(tokens.length != 6)
				throw new IllegalArgumentException("Invalid message buffer (CHUNK expects 5 aditional tokens).");
			
			String[] version_tokens = tokens[1].split(".");
			
			ProtocolHeader header = new ProtocolHeader(MessageType.CHUNK, Integer.parseInt(version_tokens[0]), Integer.parseInt(version_tokens[1]), Integer.parseInt(tokens[2]), tokens[3], Integer.parseInt(tokens[4]));
			ProtocolBody body = new ProtocolBody(tokens[5].getBytes());
			
			return new ProtocolInstance(header, body);			
		} else if(message_type.equals(MessageType.DELETE.toString())) {
			if(tokens.length != 4)
				throw new IllegalArgumentException("Invalid message buffer (DELETE expects 3 aditional tokens).");
			
			String[] version_tokens = tokens[1].split(".");
			
			ProtocolHeader header = new ProtocolHeader(MessageType.DELETE, Integer.parseInt(version_tokens[0]), Integer.parseInt(version_tokens[1]), Integer.parseInt(tokens[2]), tokens[3]);			
		
			return new ProtocolInstance(header);			
		} else if(message_type.equals(MessageType.REMOVED.toString())) {
			if(tokens.length != 5)
				throw new IllegalArgumentException("Invalid message buffer (REMOVED expects 4 aditional tokens).");
			
			String[] version_tokens = tokens[1].split(".");
			
			ProtocolHeader header = new ProtocolHeader(MessageType.REMOVED, Integer.parseInt(version_tokens[0]), Integer.parseInt(version_tokens[1]), Integer.parseInt(tokens[2]), tokens[3], Integer.parseInt(tokens[4]));			
		
			return new ProtocolInstance(header);			
		}
		
		return null;
	}
	
	
	
	
	
	
	
	
}
