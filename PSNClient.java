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
		OutputStream out;
		InputStream in;


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
			out.flush();

			in = client.getInputStream();
		
			PrintStream psout = new PrintStream(out);
			psout.println("GET /connection/test HTTP/1.1");
			psout.println("Host: " + node_url);
			psout.println("");
			psout.println("");
			
			Vector input_lines = read_lines(in);
			
			System.out.println("Rerurned");
			String value = read_bitstream(in);
			System.out.println("I Got");
			System.out.println(value);

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;


	}
	

	public Vector read_lines(InputStream ins) {
		BufferedReader in = new BufferedReader(new InputStreamReader(ins));
		
		String line = "foo";
		Vector input_lines = new Vector();
		int chars = 0;
		try {
			line = in.readLine();
			line = line.trim();
		} catch (Exception e) {
			e.printStackTrace();
		}
		while(!line.equals("")){
			try{
				System.out.println("Request Line: " + line);
				input_lines.add(line);
				line = in.readLine();
			} catch (IOException e) {
				try {
					System.out.println("Connection Closed 2");
					client.close();
					System.exit(0);
				} catch (IOException ex) {
				}
			}
		}
		//try {
		//	in.close();	
		//} catch (Exception e) {
		//	e.printStackTrace();
		//}
		return input_lines;
	}

	public String read_bitstream(InputStream ins) {
		DataInputStream in = new DataInputStream(new BufferedInputStream(ins));
		int clength = 27;
		//int clength = Integer.parseInt(request_ht.get("content-length").toString().trim());
		System.out.println("GOT LENGTH: " + clength);
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
		System.out.println("READING");
			try {
				byte[] b = new byte[fl];
				in.readFully(b);
				buffer = buffer + (new String(b));
				System.out.println("STUFF " + b);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return buffer;
	}

}
