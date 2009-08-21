import java.util.*;

class HTTP_Response {
	int error_code = 0;
	Object body = "";
	Hashtable raw = new Hashtable();


	public HTTP_Response(int code) {
		this.error_code = code;
	}

	public void setErrorCode(int code) {
		this.error_code = code;
	}

	public int getErrorCode() {
		return error_code;
	}

	public void setBody(Object in) {
		this.body = in;
	}

	public Object getBody() {
		return body;
	}

	public void setRawData(Hashtable in) {
		this.raw = in;
	}

	public Hashtable getRawData() {
		return raw;
	}
}
