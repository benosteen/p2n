import java.io.*;
import java.net.*;
import java.util.*;
import java.math.*;
import org.xml.sax.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import org.xml.sax.helpers.XMLReaderFactory;
import java.security.SignatureException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;

class ServiceInterface implements Runnable {
	private String url_base = "s3.amazonaws.com";
	private Socket client;
	private String log_file = "log/status.log_" + getDate();
	private Hashtable request_ht = new Hashtable();
	private Hashtable response_ht = new Hashtable();
	private String message = "";	

	ServiceInterface(Socket client) {
		this.client = client;
	}

	private String getDate() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date date = new Date();
		return dateFormat.format(date);
	}

	private String getDateTime() {
		DateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
		Date date = new Date();
		return dateFormat.format(date);
	}

	public void run(){
		String line = "";
		BufferedReader in = null;
		PrintWriter out = null;
		BufferedWriter log_writer = null;
		try{
			log_writer = new BufferedWriter(new FileWriter(log_file,true));
			in = new BufferedReader(new InputStreamReader(client.getInputStream()));
			out = new PrintWriter(client.getOutputStream(), true);
		} catch (IOException e) {
			try {
				log_writer.write("Failed to establish input and output streams."); log_writer.newLine();
				client.close();
			} catch (IOException ex) {
			}
		}
		request_ht.put("content-length",0);
		Vector input_lines = read_lines(in,log_writer);
		int http_code = process_input(input_lines);
		String string_to_sign = "";
		if (http_code > 399) {
			try {
				int status = header_message(http_code,out);
				client.close();	
			} catch (IOException s_error) {	
				s_error.printStackTrace();
			}
		} 
		http_code = authorize_request();
		if (http_code > 399) {
			try {
				int status = header_message(http_code,out);
				client.close();	
			} catch (IOException s_error) {	
				s_error.printStackTrace();
			}
		} else {
			String type = (String)request_ht.get("type"); 
			if (type.equals("PUT")) {
				int status = header_message(http_code,out);
				http_code = read_body(in,log_writer);
				status = header_message(http_code,out);
			} else if (type.equals("GET")) {
				http_code = get_file(out);
			} else if (type.equals("HEAD")) {
				http_code = send_head(out);
			} else if (type.equals("DELETE")) {
				if (file_delete()) {
					http_code = header_message(200,out);
				} else {
					http_code = header_message(500,out);
				}
			}
		}
		try {
			client.close();	
			log_writer.close();
		} catch (IOException s_error) {	
			s_error.printStackTrace();
		}
	}
	
	private int send_head(PrintWriter out) {
		try {
			File file = new File("data/foo.txt");
			out.println("Date: " + getDateTime());
			out.println("Server: Service Controller");
			out.println("X-Powered-By: Java");
			out.println("Connection: close");
			out.println("Content-Type: text/html; charset=utf-8");
			out.println("Content-Length: " + file.length());
			out.println("Last-Modified: " + file.lastModified());
			out.println("Content-MD5: " + get_md5());
			out.println("");
			out.println("");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 200;
	}


	private int get_file(PrintWriter out) {
		try {
			File file = new File("data/foo.txt");
			System.out.println(file.length());
			out.println("Date: " + getDateTime());
			out.println("Server: Service Controller");
			out.println("X-Powered-By: Java");
			out.println("Connection: close");
			out.println("Content-Type: text/html; charset=utf-8");
			out.println("Content-Length: " + file.length());
			out.println("");
			int ccount=0;
			FileInputStream in = new FileInputStream(file);
			while (ccount<file.length()) {
				try {
					char current = (char)in.read();
					out.print(current);
					System.out.print(current);
					ccount++;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			out.println("");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 200;
	}


public int authorize_request() 	{
	String string_to_sign="";
	Hashtable amz_values = new Hashtable();
	Vector amz_keys = new Vector();
	Enumeration keys = request_ht.keys();
	while ( keys.hasMoreElements() )
	{
		String key = (String)keys.nextElement();
		if (key.length() > 4) {
			String lkey = key.toLowerCase();
			if (lkey.substring(0,5).equals("x-amz")) {
				System.out.println("Key: "+lkey);
				amz_keys.add(lkey);
				amz_values.put(lkey,(String)request_ht.get(key));
			}
		}
	}
	Collections.sort(amz_keys);
	//for(int i=0; i<amz_keys.size(); i++) {
	//	System.out.println((String)amz_keys.get(i));
	//}

	try {
		String type = (String)request_ht.get("type");
		string_to_sign += type + "\n";
		if (type.equals("GET")) {
			string_to_sign += "\n\n";
			string_to_sign += (String)request_ht.get("date") + "\n";
		} else if (type.equals("PUT")) {
			if (request_ht.containsKey("content-md5")) {
				string_to_sign += (String)request_ht.get("content-md5") + "\n";
			} else {
				string_to_sign += "\n";
			}
			string_to_sign += (String)request_ht.get("content-type") + "\n";
			string_to_sign += (String)request_ht.get("date") + "\n";
			for(int i=0; i<amz_keys.size(); i++) {
				String local_key = ((String)amz_keys.get(i));
				string_to_sign += local_key + ":" + (String)amz_values.get(local_key) + "\n";
			}
		} else if (type.equals("DELETE")) {
			string_to_sign += "\n";
			string_to_sign += "\n";
			string_to_sign += "\n";
			string_to_sign += "x-amz-date:" + (String)amz_values.get("x-amz-date") + "\n";
		}

		String host_part = (String)request_ht.get("host");
		host_part = host_part.replace(url_base,"");
		try {
		if (host_part.indexOf(":") > 0) {
			host_part = host_part.substring(0,host_part.indexOf(":"));
		}
		if (host_part.substring(host_part.length()-1,host_part.length()).equals(".")) {
			host_part = host_part.substring(0,host_part.length()-1);
		}
		if (!host_part.equals("")) {
			host_part = "/" + host_part;
		}
		} catch (Exception e) {
		}
		String uri = (String)request_ht.get("uri");
		if (uri.indexOf("?") > -1 && (uri.indexOf("=") > uri.indexOf("?"))) {
			uri = uri.substring(0,uri.indexOf("?"));
		}
		string_to_sign += host_part + uri;
		System.out.println(string_to_sign);

		String aws_access_string = (String)request_ht.get("authorization");
		String[] parts = aws_access_string.split(" ");
		aws_access_string = parts[1];
		parts = aws_access_string.split(":");
		String aws_access_id = parts[0];
		String aws_signature = parts[1];
		
		DatabaseConnector_Mysql dbm = new DatabaseConnector_Mysql();
		int http_code = dbm.userExists(aws_access_id);
		if (http_code > 399) {
			message = "InvalidAccessKeyId: The AWS Access Key Id you provided does not exist in our records.";
			return 403;
		}

		String aws_private_key = dbm.getPrivateKey(aws_access_id);
		if (aws_private_key.equals("500")) {
			return 500;
		}
		String our_sign = calculateRFC2104HMAC(string_to_sign,aws_private_key);
		if (our_sign.equals(aws_signature)) {
			return 100;
		} else {
			message = "SignatureDoesNotMatch: The request signature we calculated does not match the signature you provided. Check your AWS Secret Access Key and signing method. For more information, see Authenticating REST Requests and Authenticating SOAP Requests for details.";
			return 403;
		} 
	} catch (Exception s2serror) {
		s2serror.printStackTrace();
		return 400;
	}
}

	public int read_body(BufferedReader in, BufferedWriter log_writer) {
		BufferedWriter file_writer = null;
		try{
			file_writer = new BufferedWriter(new FileWriter("data/foo.txt",false));
		} catch (Exception e) {
			return 500;
		}
		int clength = Integer.parseInt(request_ht.get("content-length").toString().trim());
		int ccount = 0;
		String body = "";
		while (ccount<clength) {
			try {
				char current = (char)in.read();
				file_writer.write(current);
				ccount++;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			file_writer.close();
		} catch (Exception e) {
			return 500;
		}
		int status = check_md5();
		if (status == 200) {
			return 200;
		} else {
			boolean success = file_delete();
			message = "BadDigest: The Content-MD5 you specified did not match what we received.";
			return 400;
		}
	}

	public boolean file_delete() {
		return new File("data/foo.txt").delete();
	}

	public String get_md5() {
		String file = "data/foo.txt";
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			File f = new File(file);
			InputStream is = new FileInputStream(f);				
			byte[] buffer = new byte[8192];
			int read = 0;
			while( (read = is.read(buffer)) > 0) {
				digest.update(buffer, 0, read);
			}		
			byte[] md5sum = digest.digest();
			BigInteger bigInt = new BigInteger(1, md5sum);
			String output = bigInt.toString(16);
			return output;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public int check_md5() {
		String output = get_md5();
		if (output.equals((String)request_ht.get("content-md5"))) {
			return 200;
		} else {
			return 400;
		}
	}
		
	public Vector read_lines(BufferedReader in, BufferedWriter log_writer) {
		String line = "foo";
		Vector input_lines = new Vector();
		int chars = 0;
		try {
			log_writer.write("Processing Request from " + client.getInetAddress().toString() + " (" + getDateTime() + ")"); log_writer.newLine();
			line = in.readLine().trim();
		} catch (IOException e) {
			try {
				log_writer.write("Read from " + client.getInetAddress().toString() +" at " + getDateTime() + " failed"); log_writer.newLine();
				client.close();
			} catch (IOException ex) {
			}
		}
		System.out.println("Line: " + line);
		while(!line.equals("")){
			try{
				input_lines.add(line);
				log_writer.write(line); log_writer.newLine();
				line = in.readLine();
			} catch (IOException e) {
				try {
					log_writer.write("Read from " + client.getInetAddress().toString() +" at " + getDateTime() + " failed"); log_writer.newLine();
					client.close();
				} catch (IOException ex) {
				}
			}
		}
		return input_lines;
	}

	private int process_input(Vector input_lines) {
		String line = (String)input_lines.get(0);
		System.out.println(line);
		String[] request = line.split(" ");
		if (request[0].equalsIgnoreCase("HEAD") || request[0].equalsIgnoreCase("GET") || request[0].equalsIgnoreCase("POST") || request[0].equalsIgnoreCase("PUT") || request[0].equalsIgnoreCase("DELETE") || request[0].equalsIgnoreCase("TRACE") || request[0].equalsIgnoreCase("OPTIONS") || request[0].equalsIgnoreCase("CONNECT")) {
			request_ht.put("type",request[0]);
			request_ht.put("uri",request[1]);
			request_ht.put("protocal",request[2]);
			Enumeration e = input_lines.elements();
			e.nextElement();
			while (e.hasMoreElements()) {
				line = (String)e.nextElement();
				request = line.split(":",2);
				String to_put = "";
				String req_key = request[0].trim().toLowerCase();
				try {
					to_put = (String)request_ht.get(req_key);
					if (to_put != null) {
						to_put += ",";
					}
				} catch (Exception not_existant) {}
				if (to_put != null ) {
					to_put += request[1].trim();
				} else {
					to_put = request[1].trim();
				}
				System.out.println ("Putting @ "  + req_key + " : " + to_put);
				request_ht.put(req_key,to_put);
			}
			return 200;
		} else {
			System.out.println("Line process error");
			message = "Bad Request: Could not understand headers";
			return 400;
		}
	}

	private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";


	/**
	 * Computes RFC 2104-compliant HMAC signature.
	 * * @param data
	 * The data to be signed.
	 * @param key
	 * The signing key.
	 * @return
	 * The Base64-encoded RFC 2104-compliant HMAC signature.
	 * @throws
	 * java.security.SignatureException when signature generation fails
	 */
	public static String calculateRFC2104HMAC(String data, String key)
		throws java.security.SignatureException
		{
			String result;
			try {

				// get an hmac_sha1 key from the raw key bytes
				SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), HMAC_SHA1_ALGORITHM);

				// get an hmac_sha1 Mac instance and initialize with the signing key
				Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
				mac.init(signingKey);

				// compute the hmac on input data bytes
				byte[] rawHmac = mac.doFinal(data.getBytes());

				// base64-encode the hmac
				result = Encoding.EncodeBase64(rawHmac);

			} catch (Exception e) {
				throw new SignatureException("Failed to generate HMAC : " + e.getMessage());
			}
			return result;
		}



	private int header_message(int http_code, PrintWriter out) {
		switch (http_code) {
			case 100: out.println("HTTP/1.1 100 Continue"); outputResponse(100,out); break;
			case 200: out.println("HTTP/1.1 200 OK"); break;
			case 302: out.println("HTTP/1.1 302 Found"); outputResponse(302,out); break;
			case 307: out.println("HTTP/1.1 307 Temporary Redirect"); outputResponse(307,out); break;
			case 400: out.println("HTTP/1.1 400 Bad Request"); outputResponse(400,out); break;
			case 403: out.println("HTTP/1.1 403 Forbidden"); outputResponse(403,out); break;
			case 404: out.println("HTTP/1.1 404 Not Found"); outputResponse(404,out); break;
			case 415: out.println("HTTP/1.1 415 Unsupported Media Type"); outputResponse(415,out); break;
			case 500: out.println("HTTP/1.1 500 Internal Server Error"); outputResponse(500,out); break;
			default: out.println("HTTP/1.1 400 Bad Request"); break;
		}
		//out.println("Date: " + getDateTime());
		//out.println("Server: Service Controller");
		//out.println("X-Powered-By: Java");
		//out.println("Connection: close");
		//out.println("Content-Type: text/xml; charset=utf-8");
		out.println("");
		return http_code;
	}
	private void outputResponse(int http_code,PrintWriter out) {
		out.println("Date: " + getDateTime());
		out.println("Server: Service Controller");
		out.println("X-Powered-By: Java");
		out.println("Connection: close");
		if (http_code == 302 || http_code == 307) {
			out.println("Location: " + response_ht.get("location"));
		}
		if (!message.equals("")) {
			out.println("Content-Type: text/html; charset=utf-8");
			out.println("Content-Length: " + message.toCharArray().length);
			out.println("");
			out.println(message);
		} else {
			out.println("Content-Type: text/xml; charset=utf-8");
			out.println("Content-Length: 0");
		}
	}
}

class SocketThrdServer {

	ServerSocket server = null;

	SocketThrdServer(){
		/**
		 * Constructor
		 * In here you can do things like construct a UI or something, but since each thread will handle things differently we don't want a master interfacce.
		 */
	} 

	public void listenSocket(int port){
		try{
			server = new ServerSocket(port);
			System.out.println("Listening on port " + port);
		} catch (IOException e) {
			System.out.println("Could not listen on port " + port);
			System.exit(-1);
		}
		while(true){
			ServiceInterface si;
			try{
				si = new ServiceInterface(server.accept()) ;
				Thread sit = new Thread(si);
				sit.start();
			} catch (IOException e) {
				System.out.println("Accept failed: " + port);
				System.exit(-1);
			}
		}
	}

	protected void finalize(){
		//Objects created in run method are finalized when 
		//program terminates and thread exits
		try{
			server.close();
		} catch (IOException e) {
			System.out.println("Could not close socket");
			System.exit(-1);
		}
	}

	public static void main(String[] args){
		int port = 4444;
		try {
			if (args[0].equals("--help")) {
				System.out.println("Service Interface Listener (v0.1nea)");
				System.out.println("");
				System.out.println("Usage:");
				System.out.println("");
				System.out.println(" -p    Port Number to Listen On");
				System.out.println("");
				System.out.println("");
				System.exit(0);
			} else {
				while (args.length > 0) {
					if (args[0].equals("-p")) {
						port = Integer.parseInt(args[1]);
					}
					if (args.length > 2) {
						String[] foo = new String[args.length-2];
						for (int i=2;i<args.length;i++) {
							foo[i-2] = args[i];
						}
						args = foo;
					} else {
						args = new String[0];
					}
				}
			}
		} catch ( Exception e) {
		}
		SocketThrdServer sts = new SocketThrdServer();
		sts.listenSocket(port);
	}
}

