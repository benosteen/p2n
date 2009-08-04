import com.eaio.uuid.UUID;
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

class ServiceInterface implements Runnable {
	
	/**
	  * Internal Global Variables
	  */
	private String namespace_prefix = "x-amz";
	private Socket client;
	private Hashtable request_ht = new Hashtable();
	private Hashtable response_ht = new Hashtable();
	private String message = "";	
	private Hashtable settings = new Hashtable();
	private DatabaseConnector_Mysql dbm = new DatabaseConnector_Mysql();

	/**
	  * Global Varibales set from configuration file
	  */
	private String node_id = "";
	private String node_url = "";
	private String url_base = "";
	private String log_path = "";
	private String log_file = ""; 
	private String base_path = "";
	private int allocated_space = 0;
		

	ServiceInterface(Socket client,Hashtable settings) {
		this.client = client;
		this.settings = settings;
	}

	
	private String getDate() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date date = new Date();
		return dateFormat.format(date);
	}

	public String getDateTime() {
		SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
		format.setCalendar(Calendar.getInstance(new SimpleTimeZone(0, "GMT")));
		return format.format(new Date());
		
		//DateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
		//Date date = new Date();
		//return dateFormat.format(date) + " GMT";
	}

	public Long getDateTimeUnix() {
		TimeZone tz = TimeZone.getTimeZone("UTC");
		Calendar cal = new GregorianCalendar(tz);
		cal.setTime(new Date());
		return ((cal.getTime().getTime())/1000);
	}


	private String get_settings_value(String key) {
		try {
			Vector v = (Vector)settings.get(key);
			String value = (String)v.get(0);
			return value;
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}

	public boolean global_variables() {
		try {
			node_id = get_settings_value("node_id");
			node_url =  get_settings_value("node_url");
			url_base = get_settings_value("url_base");
			log_path = get_settings_value("log_path");
			log_file = log_path + "status.log_" + getDate();
			base_path = get_settings_value("data_path");
			allocated_space = Integer.parseInt(get_settings_value("allocated_space"));
			dbm.setCredentials(get_settings_value("database_host"),get_settings_value("database_name"),get_settings_value("database_user"),get_settings_value("database_pass"));
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		try {	
			if (node_id.equals("") || node_url.equals("") || url_base.equals("") || log_path.equals("") || log_file.equals("") || base_path.equals("") || allocated_space == 0) {
				return false;
			} else {
				return true;
			}
		} catch (Exception e) {
			return false;
		}

	}


	public void run(){
		boolean success = global_variables();
		if (!success) {
			System.out.println("Configuration wrong!");
			System.exit(0);
		}
		InputStream in = null;
		OutputStream out = null;
		BufferedWriter log_writer = null;
		try{
			client.setKeepAlive(true);
			log_writer = new BufferedWriter(new FileWriter(log_file,true));
			in = client.getInputStream();
			out = client.getOutputStream();
			//in = new BufferedReader(new InputStreamReader(client.getInputStream()));
			//out = new PrintWriter(client.getOutputStream(), true);
		} catch (IOException e) {
			try {
				System.out.println("Connection Closed 0");
				log_writer.write("Failed to establish input and output streams."); log_writer.newLine();
				client.close();
			} catch (IOException ex) {
			}
		}
		boolean processor = true;
		while (processor) {
			processor = process_single_request(in,out,log_writer);
		}
	}
	
	private boolean process_single_request(InputStream in, OutputStream out, BufferedWriter log_writer) {
		String line = "";
		System.out.println("NEW REQUEST");
		request_ht = new Hashtable();
		response_ht = new Hashtable();
		Vector input_lines = new Vector();
		
		request_ht.put("content-length","0");
		input_lines = read_lines(in,log_writer);
		int http_code = process_input(input_lines);
		String string_to_sign = "";
		if (http_code > 399) {
			try {
				int status = header_message(http_code,out);
				client.close();	
				return false;
			} catch (IOException s_error) {	
				s_error.printStackTrace();
			}
		}
		String type = (String)request_ht.get("type"); 
		boolean bypass_authorisation = false;
		if (type.equals("GET") || type.equals("HEAD")) {
			String bucket = get_bucket_name();
			if (bucket == null) {
				String requested_path = (String)request_ht.get("uri");
				if (requested_path.equals("/connection/test")) {
					output_connection_test_success(out);
					return true;
				}
			} else {
				if (file_exists()) {
					if (public_access_allowed()) {
						bypass_authorisation = true;
					} else {
						message = "File not found";
						http_code = 404;
					}
				} else {
					message = "File not found";
					http_code = 404;
				}
			}
		}
		if (http_code > 399) {
			try {
				int status = header_message(http_code,out);
				client.close();	
				return false;
			} catch (IOException s_error) {	
				s_error.printStackTrace();
			}
		}
		if (bypass_authorisation) {
			http_code = 100;
		} else {
			String auth_temp_amz = (String)request_ht.get("authorization");
			if (auth_temp_amz == null) {
				message = "MissingSecurityHeader: Your request was missing a required header.";
				http_code = 400;
			} else {
				http_code = authorize_request();
			}
		}
		if (http_code > 399) {
			try {
				int status = header_message(http_code,out);
				client.close();	
				return false;
			} catch (IOException s_error) {	
				s_error.printStackTrace();
			}
		} else {
			int status = 0;
			if (type.equals("PUT")) {
				int clength = Integer.parseInt(request_ht.get("content-length").toString().trim());
				if (clength == 0) {
					http_code = handle_bucket_creation(log_writer);
				} else {
					status = header_message(http_code,out);
					http_code = put_bitstream(in,log_writer);
				}
				status = header_message(http_code,out);
			} else if (type.equals("POST")) {
				if (request_ht.get("uri").equals("/?config")) {
					http_code = 100;
				} else {
					http_code = 400;
					message = "Bad or Invalid Post Request";
				}
				status = header_message(http_code,out);
				if (http_code == 100) {
					http_code = process_post(in,log_writer);
					System.out.println("HTTP CODE : " + http_code + " " + message);
					status = header_message(http_code,out);
					if (http_code == 202) {
						http_code = node_handshake(message);
					}
				}
			} else if (type.equals("GET")) {
				http_code = get_file(out);
				if (http_code != 200) {
					status = header_message(http_code,out);
				}
			} else if (type.equals("HEAD")) {
				http_code = send_head(out);
			} else if (type.equals("DELETE")) {
				String uri = (String)request_ht.get("uri");
				if (uri.indexOf("?") > -1 && (uri.indexOf("=") > uri.indexOf("?"))) {
					uri = uri.substring(0,uri.indexOf("?"));
				}
				if (uri.trim().equals("/")) {
					http_code = handle_bucket_deletion(log_writer);
				} else {
					http_code = delete_file(log_writer);
				}
				status = header_message(http_code,out);
			}
		}
		try {
			client.close();	
			log_writer.close();
			return false;
		} catch (IOException s_error) {	
			s_error.printStackTrace();
		}
		return true;
	}
	
	private boolean public_access_allowed() {
		String requested_path = get_requested_path();
		String uuid = dbm.get_uuid_from_requested_path(requested_path);
		return dbm.public_access(uuid);
	}

	private boolean file_exists() {
		String requested_path = get_requested_path();
		String uuid = dbm.get_uuid_from_requested_path(requested_path);
		if (uuid.equals("404")) {
			return false;
		} else {
			return true;
		}
	}

	private void output_connection_test_success(OutputStream ops) {
		PrintStream out = new PrintStream(ops);
		out.println("HTTP/1.1 200 OK");
		out.println("Date: " + getDateTime());
		out.println("Server: Service Controller");
		out.println("X-Powered-By: Java");
		out.println("Connection: close");
		out.println("Content-Type: text/plain; charset=utf-8");
		out.println("Content-Length: 28");
		out.println("");
		out.println("aws sanity check succeeded!");
	}

	private int send_head(OutputStream ops) {
		PrintStream out = new PrintStream(ops);
		try {
			String requested_path = get_requested_path();
			String uuid = dbm.get_uuid_from_requested_path(requested_path);
			if (!dbm.has_local_copy(uuid) || uuid.equals("404")) {
				message = "File Not Found";
				return 404;
			}
			String actual_path = dbm.get_local_path_from_uuid(uuid);
			if (actual_path.equals("404")) {
				message = "File Not Found";
				return 404;
			} 
			output_object_headers(out,uuid);
			out.println("");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 200;
	}
	
	private void output_object_headers(PrintStream out, File file, String content_type) throws Exception {
			out.println("HTTP/1.1 200 OK");
			out.println("Date: " + getDateTime());
			out.println("Server: Service Controller");
			out.println("X-Powered-By: PSN Node Control");
			out.println("Connection: close");
			out.println("Content-Type: " + content_type);
			out.println("Content-Length: " + file.length());
			out.println("Last-Modified: " + file.lastModified());
			out.println("Content-MD5: " + get_md5(file));
			out.println("");
	}

	private void output_object_headers(PrintStream out, String uuid) throws Exception {
			String actual_path = dbm.get_local_path_from_uuid(uuid);
			File file = new File(actual_path);
			out.println("HTTP/1.1 200 OK");
			out.println("Date: " + getDateTime());
			out.println("Server: Service Controller");
			out.println("X-Powered-By: PSN Node Control");
			Hashtable metadata = dbm.get_object_metadata(uuid);
			Enumeration keys = metadata.keys();
			while ( keys.hasMoreElements() ) {
				String key = (String)keys.nextElement();
				String value = (String)metadata.get(key);
				out.println(namespace_prefix + "-meta-" + key + ": " + value);
			}
			out.println("Connection: close");
			String content_type = dbm.get_content_type(uuid,"local");
			out.println("Content-Type: " + content_type);
			out.println("Content-Length: " + file.length());
			out.println("Last-Modified: " + file.lastModified());
			out.println("Content-MD5: " + get_md5(file));
			out.println("");	
	}

	private int get_file(OutputStream ops) {
		try {
			String actual_path = "";
			String uuid = "";
			if (request_ht.get("uri").equals("/?key")) {
				actual_path = get_settings_value("log_path") + (String)request_ht.get(namespace_prefix + "-meta-remote-host") + ".data";
			} else {
			
				String requested_path = get_requested_path();
				uuid = dbm.get_uuid_from_requested_path(requested_path);
				if (!dbm.has_local_copy(uuid) || uuid.equals("404")) {
					message = "File Not Found";
					return 404;
				}
				actual_path = dbm.get_local_path_from_uuid(uuid);
				if (actual_path.equals("404")) {
					message = "File Not Found";
					return 404;
				}
			}


			File file = new File(actual_path);
			if (!file.exists()) {
				message = "File Not Found (" + actual_path + ")";
				return 404;
			}
			PrintStream out = new PrintStream(ops);
			if (uuid != "") {
				output_object_headers(out,uuid);
			} else {
				output_object_headers(out,file,"text/plain");
			}
			int ccount=0;
			
			DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
			//DataOutputStream out2 = new DataOutputStream(new BufferedOutputStream(ops));
			DataOutputStream out2 = new DataOutputStream(ops);
			int fl = ((Long)file.length()).intValue();
			int read_size = 128 * 1024 * 1024; 
			while (fl > (read_size)){
				try {
					byte[] b = new byte[read_size];
					in.readFully(b);
					out.write(b);
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
					out.write(b);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			out = new PrintStream(new BufferedOutputStream(ops));
			out.println("");
		} catch (Exception e) {
			e.printStackTrace();
		}
		message = "finished";
		return 200;
	}

private String get_requested_path() {
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
	return host_part + uri;
}

public int authorize_request() 	{

	String aws_access_string = (String)request_ht.get("authorization");
	String[] parts = aws_access_string.split(" ");
	System.out.println("AWS access string : " + aws_access_string);
	aws_access_string = parts[1];
	parts = aws_access_string.split(":");
	String aws_access_id = parts[0];
	String aws_signature = parts[1];

	int http_code = dbm.userExists(aws_access_id);
	if (http_code > 399) {
		message = "InvalidAccessKeyId: The AWS Access Key Id you provided does not exist in our records. (" + aws_access_id + ")";
		return 403;
	}
	request_ht.put("access_id",aws_access_id);

	String aws_private_key = dbm.getPrivateKey(aws_access_id);
	if (aws_private_key.equals("500")) {
		return 500;
	}

	PSNFunctions psnf = new PSNFunctions();
	String string_to_sign = psnf.getStringToSign(request_ht,settings);

	String our_sign = "";
	try {
		our_sign = psnf.calculateRFC2104HMAC(string_to_sign,aws_private_key);
	} catch (Exception e) {
		e.printStackTrace();
	}
	if (our_sign.equals(aws_signature)) {
		return 100;
	} else {
		System.out.println("Yours : " + aws_signature + " OURS " + our_sign + "\n" + string_to_sign);
		message = "SignatureDoesNotMatch: The request signature we calculated does not match the signature you provided. Check your AWS Secret Access Key and signing method. For more information, see Authenticating REST Requests and Authenticating SOAP Requests for details.";
		return 403;
	} 
}

	private int check_bucket() {
		String host_part = (String)request_ht.get("host");
		String bucket = "";
		try {
			bucket = host_part.substring(0,host_part.indexOf(url_base)-1);
		} catch (Exception e) {
			message = "InvalidURI: Couldn't parse the specified URI or URI of host not matched to this domain.";
			return 400;
		}
		boolean success = false;
		try {
			success = (new File(base_path+"local/"+(String)request_ht.get("access_id")+"/"+bucket)).isDirectory();
		} catch (Exception e) {
		}
		if (success == false) {
			message = "NoSuchBucket: The specified bucket does not exist.";
			return 404;
		} else {
			return 200;
		}
	}

	private int handle_bucket_creation(BufferedWriter log_writer) {
		String bucket = get_bucket_name();
		if (bucket == null) {
			return 500;
		}
		if (check_bucket() == 200) {
			message = "BucketAlreadyExists: Bucket already exists.";
			return 409;
		}
		Boolean success = false;
		try { 
			success = (new File(base_path+"local/"+(String)request_ht.get("access_id")+"/"+bucket)).mkdirs();
		} catch (Exception e) {
			e.printStackTrace();
			return 500;
		}
		if (success == true) {
			return 200;
		} else {
			message = "Unable to create bucket";
			return 500;
		}
		
	}

	public int delete_file(BufferedWriter log_writer) {
		int status = check_bucket();
		if (status != 200) {
			return status;
		}
		String requested_path = get_requested_path();
		String uuid = dbm.get_uuid_from_requested_path(requested_path);
		if (uuid.equals("404")) {
			message = "File Not Found";
			return 404;
		}
		boolean success=true;
		if (dbm.has_psn_copy(uuid)) {
			//DO SOME STUFF WITH THE PSN
			success=true;
			if (success) {
				dbm.unset_p2n_copy(uuid);
			}
		} 
		if (success==false) {
			message = "Error removing file from p2n distribution";
			return 500;
		} 
		if (dbm.has_local_copy(uuid)) {
			String actual_path = dbm.get_local_path_from_uuid(uuid);
			File file = new File(actual_path);	
			try {
				file.delete();
			} catch (Exception e) {
				success=false;
			}
			if (success) {
				dbm.unset_local_copy(uuid);
			}
		}
		if (success) {
			try {
				delete_empty_uuid(uuid);
				success = dbm.delete_uuid(uuid);
			} catch (Exception e) {
				success = false;
			}
		}
		if (success) {
			return 204;
		} else {
			message = "Failed to remove records from Database, big error!";
			return 500;
		}
	}

	public void delete_empty_uuid(String uuid) {
		File store_path = new File(base_path+"local/"+(String)request_ht.get("access_id")+"/"+get_bucket_name() + "/" + uuid);
		try {
			store_path.delete();
		} catch (Exception e) {}
	 	store_path = new File(base_path+"psn/" + uuid);
		try {
			store_path.delete();
		} catch (Exception e) {}
	}

	public int put_bitstream(InputStream in, BufferedWriter log_writer) {
		int status = check_bucket();
		if (status != 200) {
			return status;
		}
		String requested_path = get_requested_path();
		String access_id = (String)request_ht.get("access_id");
		String uuid = dbm.get_uuid_from_request(access_id,requested_path);
		if (uuid.equals("500")) {
			message = "Failed to fetch indexes.";
			return 500;
		}
		boolean success = false;
		String uri = (String)request_ht.get("uri");
		if (uri.indexOf("?") > -1 && (uri.indexOf("=") > uri.indexOf("?"))) {
				uri = uri.substring(0,uri.indexOf("?"));
		}
		if (uuid.equals("404")) {
			uuid = new UUID().toString();
			success = dbm.store_uuid_mapping(access_id,requested_path,uuid,(String)request_ht.get(namespace_prefix + "-acl"));
			if (success == false) {
				message = "Failed to create file storage reference";
				return 500;
			}
			File store_path = new File(base_path+"local/"+(String)request_ht.get("access_id")+"/"+get_bucket_name() + "/" + uuid);
			try {
				success = store_path.mkdir();
			} catch (Exception e) {
				message = "Could not create file path";
				return 500;
			}
			if (success == false) {
				message = "Could not create file path";
				return 500;
			}

			store_path = new File(base_path + "local/" + (String)request_ht.get("access_id") + "/" + get_bucket_name() + "/" + uuid + "/" + uri);
			int http_code = read_bitstream(in,store_path);
			if (http_code != 200) {
				store_path = new File(base_path+"local/"+(String)request_ht.get("access_id")+"/"+get_bucket_name() + "/" + uuid + "/" + uri);
				store_path.delete();
				store_path = new File(base_path+"local/"+(String)request_ht.get("access_id")+"/"+get_bucket_name() + "/" + uuid);
				store_path.delete();
				store_path = new File(base_path+"local/"+(String)request_ht.get("access_id")+"/"+get_bucket_name() + "/" + uuid + "/" + uri);
				try {
					http_code = dbm.delete_uuid_mapping(uuid,"local",store_path.toString(),node_id);
				} catch (Exception e) {
					return 500;
				}
				return http_code;
			} else {
				//REALLY NEED TO ERROR HANDLE HERE
				String psndis = (String)request_ht.get(namespace_prefix + "-meta-psndistribution");
				String psnres = (String)request_ht.get(namespace_prefix + "-meta-psnresiliance");
				success = dbm.update_file_cache(uuid,store_path.toString(),(String)request_ht.get("content-md5"),(String)request_ht.get("content-type"),"local",node_id,psndis,psnres);

				Enumeration keys = request_ht.keys();
				while ( keys.hasMoreElements() )
				{
					String key = (String)keys.nextElement();
					String lkey = key.toLowerCase();
					try {
						if (lkey.substring(0,namespace_prefix.length()+5).equals(namespace_prefix + "-meta")) {
							String outkey = key.substring(namespace_prefix.length()+6,key.length());
							dbm.write_metadata(uuid,outkey,(String)request_ht.get(key));
						}
					} catch (Exception e) {}
				}
				if (success) {
					return 200;
				} else {
					store_path = new File(base_path+"local/"+(String)request_ht.get("access_id")+"/"+get_bucket_name() + "/" + uuid + "/" + uri);
					store_path.delete();
					store_path = new File(base_path+"local/"+(String)request_ht.get("access_id")+"/"+get_bucket_name() + "/" + uuid);
					store_path.delete();
					store_path = new File(base_path+"local/"+(String)request_ht.get("access_id")+"/"+get_bucket_name() + "/" + uuid + "/" + uri);
					try {
						http_code = dbm.delete_uuid_mapping(uuid,"local",store_path.toString(),node_id);
					} catch (Exception e) {
					}
					message = "Failed to update final locations of file, aborting";
					return 500;
				}
			}
		} else {
			File store_path = store_path = new File(base_path + "local/" + (String)request_ht.get("access_id") + "/" + get_bucket_name() + "/" + uuid + "/" + uri);
			if (store_path.exists()) {
				String in_md5 = (String)request_ht.get("content-md5");
				String md5_sum = get_md5(store_path);
				if (in_md5.equals(md5_sum)) {
					message = "File already present and up to date.";
					return 409;
				}
			}
			// Move old, re-verify, store new, verify, delete_old
			// At any point fail back to using old
		}
		return 200;
	}

	public int node_handshake(String uuid) {
		String store_path = log_path + uuid + ".xml";
		NodeConfigurationHandler nch = new NodeConfigurationHandler();
		Hashtable in_settings = nch.get_configuration_from_file(store_path);
		String remote_node_url = nch.get_settings_value(in_settings,"node_url");

		PSNClient psn_con = new PSNClient();

		Hashtable metadata = new Hashtable();
		metadata.put("meta-remote-host",nch.get_settings_value(settings,"node_url"));

		Keypair kp = dbm.getNetworkKeypair();
		try {
			Thread.sleep(1000);
		} catch (Exception e) {
		}
		HTTP_Response response = psn_con.perform_get(remote_node_url,"/?key",metadata,kp);
		System.out.println(response.getBody());
		String res_uuid = (String)response.getBody();
		if (res_uuid.equals(uuid)) {
			String remote_conf_path = log_path + uuid + ".xml";
			File remote_conf = new File(remote_conf_path);
			if (!(remote_conf.exists())) {
				message = "Remote conf file not found @ " + remote_conf_path;
				return 404;
			} 
			Hashtable remote_node_settings = nch.get_configuration_from_file(remote_conf_path);
			boolean success = nch.associate_remote_node(settings,remote_node_settings,dbm);
			if (success) {
				nch.update_settings_from_db(settings,dbm);
				PSNManager psn_man = new PSNManager(settings);
				psn_man.updateNetworkConfig();
				System.out.println("DONE?");
				return 200;
			} else {
				message = "Failed to insert node config";
				return 400;
			}
			//SEND THIS CONFIG TO THAT NODE
		} else {
			System.out.println("UUID's didn't match");
			return 500;
		}
	}

	public int process_post(InputStream in, BufferedWriter log_writer) {
		String uuid = new UUID().toString();
		String store_path = log_path + uuid;
		if (((String)request_ht.get("content-type")).indexOf("xml") > -1) {
			store_path = store_path + ".xml";
		} else if (((String)request_ht.get("content-type")).indexOf("text") > -1) {
			store_path = store_path + ".txt";
		} else {
			store_path = store_path + ".data";
		}
		File tmp_file = new File(store_path);
		
		int http_code = read_bitstream(in,tmp_file);

		if (http_code > 399) {
			tmp_file.delete();
			message = "Error reading input";
			return http_code;
		}

		if (request_ht.get("uri").equals("/?config")) {
			NodeConfigurationHandler nch = new NodeConfigurationHandler();
			Hashtable in_settings = nch.get_configuration_from_file(store_path);
			String remote_node_url = nch.get_settings_value(in_settings,"node_url");
			String local_node_url = nch.get_settings_value(settings,"node_url");

			if (remote_node_url.equals(local_node_url)) {
				message = "Bad Request : 2 nodes with same address";
				tmp_file.delete();
				return 400;
			} 
			message = uuid;
			return 202;
			

		}

		return http_code;
	}

	public int read_bitstream(InputStream ins,File store_path) {
		DataInputStream in = new DataInputStream(new BufferedInputStream(ins));
		FileOutputStream file_writer = null;
		try{
			file_writer = new FileOutputStream(store_path);
		} catch (Exception e) {
			e.printStackTrace();
			message = "Could not open file for writing";
			return 500;
		}
		int clength = Integer.parseInt(request_ht.get("content-length").toString().trim());
		System.out.println("GOT LENGTH: " + clength);
		int ccount = 0;
		String body = "";
		int fl = clength;
		int read_size = 10 * 1024 * 1024; 
		while (fl > (read_size)){
			try {
				byte[] b = new byte[read_size];
				in.readFully(b);
				file_writer.write(b);
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
				file_writer.write(b);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		try {
			file_writer.close();
		} catch (Exception e) {
			return 500;
		}
		int status = check_md5(store_path);
		if (status == 200) {
			return 200;
		} else {
			try {
				boolean success = store_path.delete();
			} catch (Exception e) {
			}
			message = "BadDigest: The Content-MD5 you specified did not match what we received.";
			return 400;
		}
	}

	private String get_bucket_name() {
		String host_part = (String)request_ht.get("host");
		String bucket = "";
		try {
			bucket = host_part.substring(0,host_part.indexOf(url_base)-1);
		} catch (Exception e) {
			message = "InvalidURI: Couldn't parse the specified URI or URI of host not matched to this domain.";
			return null;
		}
		return bucket;
	}

	private int  handle_bucket_deletion(BufferedWriter log_writer) {
		String bucket = get_bucket_name();
		if (bucket == null) {
			return 500;
		}
		File dir_path = new File(base_path+"local/"+(String)request_ht.get("access_id")+"/"+bucket);
		boolean success = false;
		try {
			success = dir_path.isDirectory();
		} catch (Exception e) {
		}
		if (success == false) {
			message = "InvalidBucketName: The specified bucket is not valid.";
			return 400;
		}

		File[] files;
		try { 
			files = dir_path.listFiles();
		} catch (Exception e) {
			e.printStackTrace();
			message = "Could not get listing of bucket contents";
			return 500;
		}
	
		if (files.length > 0) {
			message = "BucketNotEmpty: The bucket you tried to delete is not empty.";
			return 409;
		} 
		
		try {	
			success = dir_path.delete();
		} catch (Exception e) {
			e.printStackTrace();
			message = "Could not delete Bucket";
			return 500;
		}
		if (success) {
			return 204;
		} else {
			message = "Could not delete bucket, even though it seems empty.";
			return 500;
		}
		
	}

	public String get_md5(File path) {
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			File f = path;
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

	public int check_md5(File path) {
		String output = get_md5(path);
		if ((String)request_ht.get("content-md5") == null) {
			return 200;
		}
		if (output.equals((String)request_ht.get("content-md5"))) {
			return 200;
		} else {
			return 400;
		}
	}
	
	public Vector read_lines(InputStream in, BufferedWriter log_writer) {

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
				System.out.println("Request Line: " + line);
				input_lines.add(line);
			} catch (IOException e) {
				try {
					System.out.println("Connection Closed 2");
					client.close();
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

	private int process_input(Vector input_lines) {
		String line = (String)input_lines.get(0);
		String[] request = line.split(" ");
		if (request[0].equalsIgnoreCase("HEAD") || request[0].equalsIgnoreCase("GET") || request[0].equalsIgnoreCase("POST") || request[0].equalsIgnoreCase("PUT") || request[0].equalsIgnoreCase("DELETE") || request[0].equalsIgnoreCase("TRACE") || request[0].equalsIgnoreCase("OPTIONS") || request[0].equalsIgnoreCase("CONNECT")) {
			request_ht.put("type",request[0]);
			String uri = request[1];
			if (uri.indexOf("?") > 0) {
				String uri_params = uri.substring(uri.indexOf("?")+1,uri.length());
				String[] uri_params_bits = uri_params.split("&");
				String aws_access_id = "";
				String aws_signature = "";
				String expires = "";
				int success_count = 0;
				for (int i=0; i<uri_params_bits.length; i++) {
					String[] sides = uri_params_bits[i].split("=");
					if (sides[0].equals("AWSAccessKeyId")) {
						aws_access_id = sides[1];
						success_count++;
					}
					if (sides[0].equals("Signature")) {
						aws_signature = new URLDecoder().decode(sides[1]);
						success_count++;
					}
					if (sides[0].equals("Expires")) {
						expires = sides[1];
						success_count++;
					}
				}
				if (success_count == 3) {
					request_ht.put("authorization","AWS " + aws_access_id + ":" + aws_signature);
					request_ht.put("date",expires);
				}

			}
			request_ht.put("uri",uri);
			request_ht.put("protocal",request[2]);
			Enumeration e = input_lines.elements();
			e.nextElement();
			while (e.hasMoreElements()) {
				line = (String)e.nextElement();
				request = line.split(":",2);
				if (request.length < 2) {
					continue;
				}
				String to_put = "";
				String req_key = request[0].trim().toLowerCase();
				try {
					if (req_key.equals("content-length")) {
						to_put = ((String)request[1]).trim();
					} else {
						to_put = (String)request_ht.get(req_key);
						if (to_put != null) {
							to_put += ",";
						}
						if (to_put != null ) {
							to_put += ((String)request[1]).trim();
						} else {
							to_put = ((String)request[1]).trim();
						}
					}
				} catch (Exception array_wrong) {
					array_wrong.printStackTrace();
				} finally {
					request_ht.put(req_key,to_put);
				}
			}
			return 200;
		} else {
			System.out.println("Line process error");
			message = "Bad Request: Could not understand headers";
			return 400;
		}
	}



	private int header_message(int http_code, OutputStream ops) {
		PrintStream out = new PrintStream(ops);
		switch (http_code) {
			case 100: out.println("HTTP/1.1 100 Continue"); outputResponse(100,out); break;
			case 200: out.println("HTTP/1.1 200 OK"); break;
			case 201: out.println("HTTP/1.1 201 Created"); outputResponse(201,out); break;
			case 202: out.println("HTTP/1.1 202 Accepted"); outputResponse(202,out); break;
			case 204: out.println("HTTP/1.1 204 No Content"); outputResponse(204,out); break;
			case 302: out.println("HTTP/1.1 302 Found"); outputResponse(302,out); break;
			case 307: out.println("HTTP/1.1 307 Temporary Redirect"); outputResponse(307,out); break;
			case 400: out.println("HTTP/1.1 400 Bad Request"); outputResponse(400,out); break;
			case 403: out.println("HTTP/1.1 403 Forbidden"); outputResponse(403,out); break;
			case 404: out.println("HTTP/1.1 404 Not Found"); outputResponse(404,out); break;
			case 409: out.println("HTTP/1.1 409 Conflict"); outputResponse(409,out); break;
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
	private void outputResponse(int http_code,PrintStream out) {
		
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
			out.println("Content-Length: 0");
		}
	}
}

class SocketThrdServer {

	ServerSocket server = null;
	Hashtable settings;
	DatabaseConnector_Mysql dbm;

	SocketThrdServer(String conf_file){
		NodeConfigurationHandler nch = new NodeConfigurationHandler();
		DatabaseConnector_Mysql dbm = new DatabaseConnector_Mysql();
		settings = nch.get_configuration_from_file(conf_file);
		boolean state = nch.check_local_node_settings(settings,dbm);
		if (!state) {
			System.out.println("Failed to start node! Error in verifying local configuration");
			System.exit(0);
		} else {
			System.out.println("Local verification passed, starting node and attempting to communicate with network");
		}
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
				si = new ServiceInterface(server.accept(),settings);
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
		String conf_file = "p2n.conf";
		int port = 8452;
		try {
			if (args[0].equals("--help")) {
				System.out.println("Service Interface Listener (v0.1nea)");
				System.out.println("");
				System.out.println("Usage:");
				System.out.println("");
				System.out.println(" -c    Path to Config File");
				System.out.println("");
				System.out.println("");
				System.exit(0);
			} else {
				while (args.length > 0) {
					if (args[0].equals("-c")) {
						conf_file = args[1];
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
		
		SocketThrdServer sts = new SocketThrdServer(conf_file);
		sts.listenSocket(port);
	}
}

