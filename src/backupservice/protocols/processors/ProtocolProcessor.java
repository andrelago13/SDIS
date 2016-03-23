package backupservice.protocols.processors;

import backupservice.protocols.ProtocolInstance;

public interface ProtocolProcessor {

	// handle messages from other peers
	// return TRUE if message was handled, false otherwise (or not interested)
	public Boolean handle(ProtocolInstance message);
	
	// must return false if processor is either busy or inactive
	public Boolean active();
	
	public void initiate();
	public void terminate();
}
