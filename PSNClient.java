import java.io.*;
import java.net.*;
import java.util.*;
import java.math.*;
import org.xml.sax.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.security.SignatureException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;

class PSNClient {
	Socket client;

	public PSNClient() {
	}

	public boolean connectionTest(String node_url) {
		int node_port = 80;
		InputStream in;
		OutputStream out;


		if (node_url.indexOf(":") > 0) {
			String[] parts = node_url.split(":");
			node_url = parts[0];
			node_port = Integer.parseInt(parts[1]);
		}
		try {
			System.out.println("attempting connection test");
			String message = "GET /connection/test HTTP/1.1\nHost:" + node_url + "\n\n";
			
			client = new Socket(node_url, node_port);
			
			out = client.getOutputStream();
			//out.flush();

			in = client.getInputStream();
		
			PrintStream psout = new PrintStream(out);
			psout.println("GET /connection/test HTTP/1.1");
			psout.println("Host: " + node_url);
			psout.println("");
			psout.println("");
			
			Vector input_lines = read_lines(in);
			Hashtable response_ht = process_input(input_lines);	
			
			String content_type = (String)response_ht.get("content-type");
			String in_clength = (String)response_ht.get("content-length");
			in_clength = in_clength.trim();
			
			int content_length = Integer.parseInt(in_clength);
			
			
			String value = read_bitstream(in,content_length);
			value = value.trim();
			if (value.equals("aws sanity check succeeded!")){
				return true;
			} else {
				
				return false;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	

	public Vector read_lines(InputStream in) {

		Vector input_lines = new Vector();
		String line = "first";
		while(!line.equals("")){
			line = "";
			try {
				String current = "f";
				byte[] b = new byte[1];
				while (current.indexOf("\n") < 0) {
					int done = in.read(b);
					current = new String(b);
					line = line + current;
				}
				line = line.trim();
				input_lines.add(line);
			} catch (IOException e) {
				try {
					System.out.println("Connection Closed 2");
					client.close();
					System.exit(0);
				} catch (IOException ex) {
				}
			}
		}
		try {
		} catch (Exception e) {
			e.printStackTrace();
		}
		return input_lines;
	}

	public String read_bitstream(InputStream ins,int content_length) {
		DataInputStream in = new DataInputStream(new BufferedInputStream(ins));
		int clength = content_length;
		int ccount = 0;
		String body = "";
		int fl = clength;
		int read_size = 10 * 1024 * 1024; 
		String buffer = "";
		while (fl > (read_size)){
			try {
				byte[] b = new byte[read_size];
				in.readFully(b);
				buffer = buffer + (new String(b));
				fl = fl - read_size;
			} catch (Exception e) {
				e.printStackTrace();
				break;
			}
		}
		if (fl > 0) {
			try {
				byte[] b = new byte[fl];
				in.readFully(b);
				buffer = buffer + (new String(b));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return buffer;
	}

	private Hashtable process_input(Vector input_lines) {
		Hashtable response_ht = new Hashtable();
		String line = (String)input_lines.get(0);
		String[] request = line.split(" ");
		response_ht.put("protocol",request[0]);
		response_ht.put("code",request[1]);
		response_ht.put("message",request[2]);
		Enumeration e = input_lines.elements();
		e.nextElement();
		while (e.hasMoreElements()) {
			line = (String)e.nextElement();
			request = line.split(":",2);
			String to_put = "";
			String req_key = request[0].trim().toLowerCase();
			if (req_key == null || req_key == "") {
				continue;
			}
			try {
				to_put = (String)response_ht.get(req_key);
				if (to_put != null) {
					to_put += ",";
				}
				if (to_put != null ) {
					to_put += request[1].trim();
				} else {
					to_put = request[1].trim();
				}
				response_ht.put(req_key,to_put);
			} catch (Exception not_existant) {}
		}
		return response_ht;
	}

}
