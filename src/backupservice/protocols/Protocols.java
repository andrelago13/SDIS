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

	// TODO criar Instance a partir de byte buffer
	public static ProtocolInstance parseMessage(byte[] message) throws IllegalArgumentException {
		String[] tokens = (new String(message)).split("\\s+");
		
		if(tokens.length == 0) {
			throw new IllegalArgumentException("Invalid message buffer (empty buffer).");
		}
		
		String message_type = tokens[0];
		
		if(message_type.equals(MessageType.PUTCHUNK.toString())) {
			// TODO check length
			// TODO maybe a common parser for some subprotocols
		} else if(message_type.equals(MessageType.STORED.toString())) {
			
		} else if(message_type.equals(MessageType.GETCHUNK.toString())) {
			
		} else if(message_type.equals(MessageType.CHUNK.toString())) {
			
		} else if(message_type.equals(MessageType.DELETE.toString())) {
			
		} else if(message_type.equals(MessageType.REMOVED.toString())) {
			
		} else {
			throw new IllegalArgumentException("Invalid message buffer (first token must be valid message type).");
		}
		
		return null;
	}
	
	
	
	
	
	
	
	
}
