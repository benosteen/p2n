class PSNScanData {

	private int file_id,timestamp,locked;
	private String message_type,message;

	public PSNScanData() {};

	public void setFileID(int file_id) {
		this.file_id = file_id;
	}

	public int getFileID() {
		return file_id;
	}
	
	public void setTimestamp(int ts) {
		this.timestamp = ts;
	}

	public int getTimestamp() {
		return timestamp;
	}
	
	public void setLocked(int locked) {
		this.locked = locked;
	}

	public int getLocked() {
		return locked;
	}

	public void setMessageType(String message_type) {
		this.message_type = message_type;
	}

	public String getMessageType() {
		return message_type;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}


}
