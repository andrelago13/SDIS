package backupservice.protocols;

import utils.ArrayUtils;

public class ProtocolInstance {
	
	private ProtocolHeader header;
	private ProtocolBody body;
	
	public ProtocolInstance(ProtocolHeader header, ProtocolBody body) {
		this.header = header;
		this.body = body;
	}
	
	public ProtocolHeader getHeader() {
		return header;
	}

	public void setHeader(ProtocolHeader header) {
		this.header = header;
	}

	public ProtocolBody getBody() {
		return body;
	}

	public void setBody(ProtocolBody body) {
		this.body = body;
	}
	
	public byte[] toBytes() {
		return ArrayUtils.appendArrays(header.toBytes(), body.getContent());
	}

}
