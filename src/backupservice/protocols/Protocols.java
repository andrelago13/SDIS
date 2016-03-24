package backupservice.protocols;

public class Protocols {
	
	public static final int MAX_PACKET_LENGTH = 64000; //64KB

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

	public static ProtocolInstance parseMessage(String message) throws IllegalArgumentException {
		String message_str = new String(message);
		String[] temp = (message_str).split(LINE_SEPARATOR);
		
		if(temp.length == 0) {
			throw new IllegalArgumentException("Invalid message buffer (empty buffer).");
		}
		
		String header_str = temp[0];
		String[] header_split = header_str.split("\\s+");
		
		if(header_split.length == 0) {
			throw new IllegalArgumentException("Invalid message buffer (empty header).");
		}
		
		String message_type = header_split[0];
		
		if(message_type.equals(MessageType.PUTCHUNK.toString())) {
			if(header_split.length != 6)
				throw new IllegalArgumentException("Invalid message buffer (PUTCHUNK expects 5 aditional tokens).");
			
			String[] version_tokens = header_split[1].split("\\.");
			
			if(temp.length < 3) {
				throw new IllegalArgumentException("Invalid message buffer (no body found).");				
			}
			
			ProtocolHeader header = new ProtocolHeader(MessageType.PUTCHUNK, Integer.parseInt(version_tokens[0]), Integer.parseInt(version_tokens[1]), Integer.parseInt(header_split[2]), header_split[3], Integer.parseInt(header_split[4]), Integer.parseInt(header_split[5]));
			ProtocolBody body = new ProtocolBody(message_str.substring(message_str.indexOf(LINE_SEPARATOR) + 2*LINE_SEPARATOR.length(), message_str.length()-1).getBytes());
			
			return new ProtocolInstance(header, body);
		} else if(message_type.equals(MessageType.STORED.toString())) {
			if(header_split.length != 5)
				throw new IllegalArgumentException("Invalid message buffer (STORED expects 4 aditional tokens).");
			
			String[] version_tokens = header_split[1].split("\\.");
			
			ProtocolHeader header = new ProtocolHeader(MessageType.STORED, Integer.parseInt(version_tokens[0]), Integer.parseInt(version_tokens[1]), Integer.parseInt(header_split[2]), header_split[3], Integer.parseInt(header_split[4]));
			
			return new ProtocolInstance(header);
		} else if(message_type.equals(MessageType.GETCHUNK.toString())) {
			if(header_split.length != 5)
				throw new IllegalArgumentException("Invalid message buffer (GETCHUNK expects 4 aditional tokens).");
			
			String[] version_tokens = header_split[1].split("\\.");
			
			ProtocolHeader header = new ProtocolHeader(MessageType.GETCHUNK, Integer.parseInt(version_tokens[0]), Integer.parseInt(version_tokens[1]), Integer.parseInt(header_split[2]), header_split[3], Integer.parseInt(header_split[4]));			
		
			return new ProtocolInstance(header);
		} else if(message_type.equals(MessageType.CHUNK.toString())) {
			if(header_split.length != 5)
				throw new IllegalArgumentException("Invalid message buffer (CHUNK expects 4 aditional tokens).");
			
			String[] version_tokens = header_split[1].split("\\.");
			
			if(temp.length < 3) {
				throw new IllegalArgumentException("Invalid message buffer (no body found).");				
			}
			
			ProtocolHeader header = new ProtocolHeader(MessageType.CHUNK, Integer.parseInt(version_tokens[0]), Integer.parseInt(version_tokens[1]), Integer.parseInt(header_split[2]), header_split[3], Integer.parseInt(header_split[4]));
			ProtocolBody body = new ProtocolBody(message_str.substring(message_str.indexOf(LINE_SEPARATOR) + 2*LINE_SEPARATOR.length(), message_str.length()-1).getBytes());
			
			return new ProtocolInstance(header, body);			
		} else if(message_type.equals(MessageType.DELETE.toString())) {
			if(header_split.length != 4)
				throw new IllegalArgumentException("Invalid message buffer (DELETE expects 3 aditional tokens).");
			
			String[] version_tokens = header_split[1].split("\\.");
			
			ProtocolHeader header = new ProtocolHeader(MessageType.DELETE, Integer.parseInt(version_tokens[0]), Integer.parseInt(version_tokens[1]), Integer.parseInt(header_split[2]), header_split[3]);			
		
			return new ProtocolInstance(header);			
		} else if(message_type.equals(MessageType.REMOVED.toString())) {
			if(header_split.length != 5)
				throw new IllegalArgumentException("Invalid message buffer (REMOVED expects 4 aditional tokens).");
			
			String[] version_tokens = header_split[1].split("\\.");
			
			ProtocolHeader header = new ProtocolHeader(MessageType.REMOVED, Integer.parseInt(version_tokens[0]), Integer.parseInt(version_tokens[1]), Integer.parseInt(header_split[2]), header_split[3], Integer.parseInt(header_split[4]));			
		
			return new ProtocolInstance(header);			
		}
		
		return null;
	}
	
	public static String currentVersion() {
		return "" + PROTOCOL_VERSION_MAJOR + "." + PROTOCOL_VERSION_MINOR;
	}
	
}
