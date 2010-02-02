import java.io.*;

class PSNHTTPFunctions {
	
	public PSNHTTPFunctions() {
	}

	PSNFunctions psnf = new PSNFunctions();
	
	public int header_message(int http_code, OutputStream ops, String message, String location) {
		PrintStream out = new PrintStream(ops);
		switch (http_code) {
			case 100: out.println("HTTP/1.1 100 Continue"); outputResponse(100,out,message,location); break;
			case 200: out.println("HTTP/1.1 200 OK"); break;
			case 201: out.println("HTTP/1.1 201 Created"); outputResponse(201,out,message,location); break;
			case 202: out.println("HTTP/1.1 202 Accepted"); outputResponse(202,out,message,location); break;
			case 204: out.println("HTTP/1.1 204 No Content"); outputResponse(204,out,message,location); break;
			case 302: out.println("HTTP/1.1 302 Found"); outputResponse(302,out,message,location); break;
			case 307: out.println("HTTP/1.1 307 Temporary Redirect"); outputResponse(307,out,message,location); break;
			case 400: out.println("HTTP/1.1 400 Bad Request"); outputResponse(400,out,message,location); break;
			case 403: out.println("HTTP/1.1 403 Forbidden"); outputResponse(403,out,message,location); break;
			case 404: out.println("HTTP/1.1 404 Not Found"); outputResponse(404,out,message,location); break;
			case 409: out.println("HTTP/1.1 409 Conflict"); outputResponse(409,out,message,location); break;
			case 415: out.println("HTTP/1.1 415 Unsupported Media Type"); outputResponse(415,out,message,location); break;
			case 500: out.println("HTTP/1.1 500 Internal Server Error"); outputResponse(500,out,message,location); break;
			default: out.println("HTTP/1.1 400 Bad Request"); break;
		}
		out.println("");
		return http_code;
	}

	private void outputResponse(int http_code,PrintStream out,String message, String location) {

		out.println("Date: " + psnf.getDateTime());
		out.println("Server: Service Controller");
		out.println("X-Powered-By: Java");
		out.println("Connection: close");
		if (http_code == 302 || http_code == 307) {
			out.println("Location: " + location);
		}
		if (!message.equals("")) {
			out.println("Content-Type: text/html; charset=utf-8");
			out.println("Content-Length: " + message.toCharArray().length);
			out.println("");
			out.println(message);
		} else {
			out.println("Content-Length: 0");
		}
	}

}
