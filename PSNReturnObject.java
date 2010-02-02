class PSNReturnObject {
	
	Object object;
	String message = "";
	int error_code;

	public PSNReturnObject() {
	}

	public void setObject(Object object_in) {

		this.object = object_in;

	}
	
	public Object getObject() {

		return object;

	}

	public void setMessage(String message_in) {
	
		this.message = message_in; 
	
	}	
	
	public String getMessage() {
	
		return message;

	}

	public void setErrorCode(int error_code_in) {
	
		this.error_code = error_code_in;
	
	}

	public int getErrorCode() {

		return error_code;

	}

}
