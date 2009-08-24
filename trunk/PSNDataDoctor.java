import java.io.*;
import java.net.*;
import java.util.*;
import java.math.*;
import org.xml.sax.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import org.xml.sax.helpers.XMLReaderFactory;

public class PSNDataDoctor {
	
	private DatabaseConnector_Mysql dbm = new DatabaseConnector_Mysql();
	
	private String node_id = "";
	private String node_url = "";
	private String url_base = "";
	private String log_path = "";
	private String log_file = "";
	private String base_path = "";
	private int allocated_space = 0;
	private Hashtable settings = new Hashtable();
	private NodeConfigurationHandler nch = new NodeConfigurationHandler();

	public PSNDataDoctor(String conf_file) {
		settings = nch.get_configuration_from_file(conf_file);
		node_id = get_settings_value("node_id");
		node_url =  get_settings_value("node_url");
		dbm.setCredentials(get_settings_value("database_host"),get_settings_value("database_name"),get_settings_value("database_user"),get_settings_value("database_pass"));
		/*
		 * Set up the rest if needed here
		 */
	}
	
	public PSNDataDoctor(Hashtable settings) {
		this.settings = settings;
		node_id = get_settings_value("node_id");
		node_url =  get_settings_value("node_url");
		dbm.setCredentials(get_settings_value("database_host"),get_settings_value("database_name"),get_settings_value("database_user"),get_settings_value("database_pass"));
		/*
		 * Set up the rest if needed here
		 */
	}

	private String get_settings_value(String key) {
		try {
			Vector v = (Vector)settings.get(key);
			String value = (String)v.get(0);
			return value;
		} catch (Exception e) {
			return null;
		}
	}

	private Keypair getNetworkKeypair() {
		String network_access_id = get_settings_value("network_access_id");
		String network_private_key = get_settings_value("network_private_key");
	
		Keypair kp = new Keypair(network_access_id,network_private_key);
	
		return kp;
	}

	private void outputStatus() {
	}

	public void scanProcess() {
		Vector newItems  = (Vector)dbm.get_non_scanned();
		for (int i=0;i<newItems.size();i++) {
			int file_id = (Integer)newItems.get(i);
			scanFile(file_id);
		}
	}

	public void scanFile(int file_id) {
		PSNObject psno = dbm.getPSNObjectFromFile(file_id);
		PSNNode node = psno.getPSNNode();
		String node_url = node.get_node_url();
		String uri = psno.getRequestedPath();
		Keypair kp = getNetworkKeypair();
		PSNClient psn_con = new PSNClient();

		PSNFunctions psnf = new PSNFunctions();
		long unix = psnf.getDateTimeUnix();

		String host = get_settings_value("url_base");
		if (psno.getType().equals("local")) {
			PSNObject local = dbm.get_psn_object(psno.getUUID());
			String bucket = local.getBucket();
			host = bucket + "." + host;

			uri = local.getRequestedPath();
			uri = uri.replace("/" + bucket,"");
		}
	
		HTTP_Response res = psn_con.perform_head(settings,node_url,host,uri,kp);
		Hashtable request_ht = res.getRawData();
		String res_mime = (String)request_ht.get("content-type");
		String res_md5 = (String)request_ht.get("content-md5");
		if (res.getErrorCode() != 200) {
			System.out.println("Possible failure");
			return;
		}
		if (res_mime.equals((String)psno.getMimeType()) && res_md5.equals((String)psno.getMD5Sum())) {
			dbm.updateScanningLog(file_id,"md5+mime","passed",unix);
			System.out.println("All checks successful");
		} else {
			System.out.println("Checks failed");
			dbm.updateScanningLog(file_id,"md5+mime","failed",unix);
		}
	}
	
	public static void main(String[] args) {
		String conf_file = "p2n.conf";
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
		
		PSNDataDoctor psn_doc = new PSNDataDoctor(conf_file);

		while (true) {
			
			psn_doc.scanProcess();		
	
			try {
				System.out.println("SLEEPING");
				Thread.sleep(540000);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}



}
