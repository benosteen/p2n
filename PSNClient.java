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
	private String namespace_prefix = "x-amz";
	
	public PSNClient() {
	}
	public static void main(String[] args) {
		PSNClient psn_con = new PSNClient();
		Hashtable metadata = new Hashtable();
		metadata.put("meta-remote-host","yomiko.ecs.soton.ac.uk:8452");
		Keypair kp = new Keypair("P2N74329JDMSNGOLPF3","GFFdsfWEw3+3Ggfsdsd+DSGFGsffshf322fhgu4k");
		HTTP_Response res = psn_con.perform_get("yomiko.ecs.soton.ac.uk:8452","/?key",metadata,kp);
		System.out.println(res.getBody());
	}

	private void setupSocket(String node_url) {
		int node_port = 80;
		InputStream in;
		OutputStream out;


		if (node_url.indexOf(":") > 0) {
			String[] parts = node_url.split(":");
			node_url = parts[0];
			node_port = Integer.parseInt(parts[1]);
		}

		try {
			
			client = new Socket(node_url, node_port);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public boolean connectionTest(String node_url) {
		InputStream in;
		OutputStream out;

		try {
			setupSocket(node_url);
			
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
			client.close();
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


	public HTTP_Response perform_get(String node_url,String uri,Hashtable metadata,Keypair kp) {
		PSNFunctions psnf = new PSNFunctions();

		setupSocket(node_url);

		Hashtable request_ht = new Hashtable();
		
		String host = "";
		if (node_url.indexOf(":") > -1) {
			host = node_url.substring(0,node_url.indexOf(":"));
		} else {
			host = node_url;
		}
			
		request_ht.put("type","GET");
		request_ht.put("date",psnf.getDateTime());
		request_ht.put("uri",uri);
		
		for (Enumeration e = metadata.keys(); e.hasMoreElements();) {
			String key = (String)e.nextElement();
			String value = (String)metadata.get(key);
			request_ht.put(namespace_prefix + "-" + key,value);
		}
		
		/*
			GAH BAD AND WRONG
		*/
		Hashtable settings = new Hashtable();
		Vector vec = new Vector();
		vec.add("");
		settings.put("url_base",vec);
		vec = new Vector();
		vec.add(node_url);
		settings.put("node_url",vec);

		String string_to_sign = psnf.getStringToSign(request_ht,settings);

		
		OutputStream out;
		InputStream in;
		String signature = "";

		try {
			signature = psnf.calculateRFC2104HMAC(string_to_sign, kp.get_private_key());
	
			request_ht.put("authorization","AWS " + kp.get_access_id() + ":" + signature);
				
			out = client.getOutputStream();

			in = client.getInputStream();
	
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		PrintStream psout = new PrintStream(out);
		psout.println("GET " + uri + " HTTP/1.1");
		psout.println("Host: " + host);
		psout.println("Date: " + (String)request_ht.get("date"));
		for (Enumeration e = metadata.keys(); e.hasMoreElements();) {
			String key = (String)e.nextElement();
			String value = (String)metadata.get(key);
			psout.println(namespace_prefix + "-" + key + ": " + value);
		}
		psout.println("Authorization: " + request_ht.get("authorization"));
		psout.println("");

		Vector input_lines = read_lines(in);

		Hashtable response_ht = process_input(input_lines);
		int clength = Integer.parseInt((String)response_ht.get("content-length"));
		String response_body = read_bitstream(in,clength);
		HTTP_Response res = new HTTP_Response(Integer.parseInt((String)response_ht.get("code")));
		res.setBody(response_body);
		return res;


	}


	public HTTP_Response perform_post(Hashtable settings, String node_url,String body,String mime_type,String uri,Keypair kp) {
		PSNFunctions psnf = new PSNFunctions();

		setupSocket(node_url);

		Hashtable request_ht = new Hashtable();
		
		String host = "";
		if (node_url.indexOf(":") > -1) {
			host = node_url.substring(0,node_url.indexOf(":"));
		} else {
			host = node_url;
		}
			
		int content_length = body.length();
		
		request_ht.put("type","POST");
		request_ht.put("host",host);
		request_ht.put("date",psnf.getDateTime());
		request_ht.put("content-type",mime_type);
		request_ht.put("content-length",content_length);
		request_ht.put("uri",uri);
		
		String string_to_sign = psnf.getStringToSign(request_ht,settings);


		OutputStream out;
		InputStream in;
		String signature = "";

		try {
			signature = psnf.calculateRFC2104HMAC(string_to_sign, kp.get_private_key());
	
			request_ht.put("authorization","AWS " + kp.get_access_id() + ":" + signature);
				
			out = client.getOutputStream();
	
			in = client.getInputStream();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		PrintStream psout = new PrintStream(out);
		psout.println("POST " + uri + " HTTP/1.1");
		psout.println("Host: " + host);
		psout.println("Date: " + (String)request_ht.get("date"));
		psout.println("Content-Type: " + mime_type);
		psout.println("Content-Length: " + content_length);
		psout.println("Authorization: " + request_ht.get("authorization"));
		psout.println("");

		Vector input_lines = read_lines(in);
		
		Hashtable response_ht = process_input(input_lines);
		if ((Integer.parseInt((String)response_ht.get("code"))) != 100) {
			int clength = Integer.parseInt((String)response_ht.get("content-length"));
			String response_body = read_bitstream(in,clength);
			HTTP_Response res = new HTTP_Response(Integer.parseInt((String)response_ht.get("code")));
			res.setBody(response_body);
			return res;
		} else {
			psout.println(body);
		}

		input_lines = read_lines(in);
		
		response_ht = process_input(input_lines);
		int clength = Integer.parseInt((String)response_ht.get("content-length"));
		String response_body = read_bitstream(in,clength);
		HTTP_Response res = new HTTP_Response(Integer.parseInt((String)response_ht.get("code")));
		res.setBody(response_body);
	
		return res;

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

				if (line != "") {
					input_lines.add(line);
				}
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
