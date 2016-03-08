package backupservice.protocols;

public class ProtocolBody {
	
	private byte[] content;
	
	public ProtocolBody(byte[] content) {
		this.content = content;
	}
	
	public byte[] getContent() {
		return content;
	}
}
