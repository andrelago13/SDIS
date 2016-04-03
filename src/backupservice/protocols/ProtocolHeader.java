package backupservice.protocols;

import backupservice.protocols.Protocols.MessageType;

public class ProtocolHeader {
	
	// ORDER:
	// <MessageType> <Version> <SenderId> <FileId> <ChunkNo> <ReplicationDeg> <CRLF>

	private MessageType message_type = null;
	private int version_major = -1;
	private int version_minor = -1;
	private int sender_id = -1;
	private String file_id = "";
	private int chunk_no = -1;
	private int replication_deg = -1;
	
	//PUTCHUNK <Version> <SenderId> <FileId> <ChunkNo> <ReplicationDeg> <CRLF><CRLF><Body>
	//STORED <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
	//GETCHUNK <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
	//CHUNK <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF><Body>
	//DELETE <Version> <SenderId> <FileId> <CRLF><CRLF>
	//REMOVED <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
	
	// PUTCHUNK
	public ProtocolHeader(MessageType type, int version_major, int version_minor, int sender_id, String file_id, int chunk_no, int replication_deg) throws IllegalArgumentException {
		if(type != MessageType.PUTCHUNK && type != MessageType.PUTCHUNKENH) {
			throw new IllegalArgumentException("Message Type does not match given arguments. Expected PUTCHUNK.");
		}
		
		validateVersion(version_major, version_minor);
		validateSenderId(sender_id);
		validateFileId(file_id);
		validateChunkNo(chunk_no);
		validateReplicationDeg(replication_deg);
		
		this.message_type = type;
		this.version_major = version_major;
		this.version_minor = version_minor;
		this.sender_id = sender_id;
		this.file_id = file_id;
		this.chunk_no = chunk_no;
		this.replication_deg = replication_deg;
	}
	
	// STORED, GETCHUNK, CHUNK, REMOVED
	public ProtocolHeader(MessageType type, int version_major, int version_minor, int sender_id, String file_id, int chunk_no) throws IllegalArgumentException {
		if(type != MessageType.STORED && type != MessageType.GETCHUNK && type != MessageType.CHUNK && type != MessageType.REMOVED) {
			throw new IllegalArgumentException("Message Type does not match given arguments. Expected STORED, GETCHUNK, CHUNK or REMOVED.");			
		}
		
		validateVersion(version_major, version_minor);
		validateSenderId(sender_id);
		validateFileId(file_id);
		validateChunkNo(chunk_no);
		
		this.message_type = type;
		this.version_major = version_major;
		this.version_minor = version_minor;
		this.sender_id = sender_id;
		this.file_id = file_id;
		this.chunk_no = chunk_no;
		
	}
	
	// DELETE, EXISTS, WASDELETED (The last two are enhancements)
	public ProtocolHeader(MessageType type, int version_major, int version_minor, int sender_id, String file_id) throws IllegalArgumentException {
		if(type != MessageType.DELETE && type != MessageType.EXISTS && type != MessageType.WASDELETED) {
			throw new IllegalArgumentException("Message Type does not match given arguments. Expected DELETE.");
		}
		
		validateVersion(version_major, version_minor);
		validateSenderId(sender_id);
		validateFileId(file_id);
		
		this.message_type = type;
		this.version_major = version_major;
		this.version_minor = version_minor;
		this.sender_id = sender_id;
		this.file_id = file_id;
		
	}
	
	public static Boolean validVersion(int version_major, int version_minor) {
		return (version_major >= 0 && version_minor >= 0);
	}
	
	public static Boolean validSenderId(int sender_id) {
		return sender_id >= 0;
	}
	
	public static Boolean validFileId(String file_id) {
		return file_id.replace(" ", "").length() > 0;
	}
	
	public static Boolean validChunkNo(int chunk_no) {
		return chunk_no >= 0;
	}
	
	public static Boolean validReplicationDeg(int replication_deg) {
		return replication_deg > 0 && replication_deg <= 9;
	}
	
	private static void validateVersion(int version_major, int version_minor) throws IllegalArgumentException {
		if(!validVersion(version_major,version_minor))
			throw new IllegalArgumentException("Invalid version parameters.");
	}
	
	private static void validateSenderId(int sender_id) throws IllegalArgumentException {
		if(!validSenderId(sender_id))
			throw new IllegalArgumentException("Invalid sender id.");
	}
	
	private static void validateFileId(String file_id) throws IllegalArgumentException {
		if(!validFileId(file_id))
			throw new IllegalArgumentException("Invalid file id.");
	}
	
	private static void validateChunkNo(int chunk_no) throws IllegalArgumentException {
		if(!validChunkNo(chunk_no))
			throw new IllegalArgumentException("Invalid chunk number.");
	}
	
	private static void validateReplicationDeg(int replication_deg) throws IllegalArgumentException {
		if(!validReplicationDeg(replication_deg))
			throw new IllegalArgumentException("Invalid file replication degree.");
	}
	
	public byte[] toBytes() {
		String result = "" + message_type.toString() + Protocols.FIELD_SEPARATOR;
		
		if(version_major != -1 && version_minor != -1) {
			result += "" + version_major + "." + version_minor +  Protocols.FIELD_SEPARATOR;
		}
		
		if(sender_id != -1) {
			result += "" + sender_id + Protocols.FIELD_SEPARATOR;
		}
		
		if(!file_id.equals("")) {
			result += file_id + Protocols.FIELD_SEPARATOR;
		}
		
		if(chunk_no != -1) {
			result += "" + chunk_no + Protocols.FIELD_SEPARATOR;
		}
		
		if(replication_deg != -1) {
			result += "" + replication_deg + Protocols.FIELD_SEPARATOR;
		}
		
		result += Protocols.LINE_SEPARATOR+Protocols.LINE_SEPARATOR;
		
		return result.getBytes();
	}
	

	public MessageType getMessage_type() {
		return message_type;
	}
	

	public void setMessage_type(MessageType message_type) {
		this.message_type = message_type;
	}
	

	public int getVersion_major() {
		return version_major;
	}
	

	public void setVersion_major(int version_major) {
		this.version_major = version_major;
	}
	

	public int getVersion_minor() {
		return version_minor;
	}
	

	public void setVersion_minor(int version_minor) {
		this.version_minor = version_minor;
	}
	

	public int getSender_id() {
		return sender_id;
	}
	

	public void setSender_id(int sender_id) {
		this.sender_id = sender_id;
	}
	

	public String getFile_id() {
		return file_id;
	}
	

	public void setFile_id(String file_id) {
		this.file_id = file_id;
	}
	

	public int getChunk_no() {
		return chunk_no;
	}
	

	public void setChunk_no(int chunk_no) {
		this.chunk_no = chunk_no;
	}
	

	public int getReplication_deg() {
		return replication_deg;
	}
	

	public void setReplication_deg(int replication_deg) {
		this.replication_deg = replication_deg;
	}
	
}
