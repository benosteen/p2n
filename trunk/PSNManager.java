import java.io.*;
import java.net.*;
import java.util.*;
import java.math.*;
import org.xml.sax.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import org.xml.sax.helpers.XMLReaderFactory;

public class PSNManager {
	
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

	public PSNManager(String conf_file) {
		settings = nch.get_configuration_from_file(conf_file);
		node_id = get_settings_value("node_id");
		node_url =  get_settings_value("node_url");
		dbm.setCredentials(get_settings_value("database_host"),get_settings_value("database_name"),get_settings_value("database_user"),get_settings_value("database_pass"));
		/*
		 * Set up the rest if needed here
		 */
	}
	
	public PSNManager(Hashtable settings) {
		this.settings = settings;
		node_id = get_settings_value("node_id");
		node_url =  get_settings_value("node_url");
		dbm.setCredentials(get_settings_value("database_host"),get_settings_value("database_name"),get_settings_value("database_user"),get_settings_value("database_pass"));
		/*
		 * Set up the rest if needed here
		 */
	}

	public void updateNetworkConfig() {
		System.out.println("TEST: " + settings.get("test"));
		Vector vec = (Vector)settings.get("node");
		Keypair kp = dbm.getNetworkKeypair();
		System.out.println("NUMBER OF NODES: " + vec.size());
		for (Enumeration e = vec.elements(); e.hasMoreElements();) {
			PSNNode node = (PSNNode)e.nextElement();
			System.out.println("NODE ID for " + node.get_node_url() + " = " + node.get_node_id());
			int back = node_handshake( node, kp );
			try {
				Thread.sleep(10000);
			} catch (Exception se) {
				se.printStackTrace();
			}
			System.out.println("DONE SLEEPING");
		}
	}

	private int node_handshake( PSNNode node, Keypair kp ) {
		PSNFunctions psnf = new PSNFunctions();
		PSNClient psn_con = new PSNClient();
		String node_url = node.get_node_url();
		node_url = node_url.trim();
		String this_node_id = get_settings_value( "node_id" );
		boolean next_step = true;
		if (node.get_node_id().equals(this_node_id)) {
			next_step = false;
		}
		if (node.get_last_handshake() < (psnf.getDateTimeUnix() - 300)) {
			next_step = false;
			System.out.println("Not sending to " + node_url + " as was done recently");
			System.out.println("Last Handshake = " + node.get_last_handshake());
			System.out.println("-5 min = " + (psnf.getDateTimeUnix() - 300));
		}
		if (next_step && psn_con.connectionTest(node_url)) {
			System.out.println("Sending data to <" + node_url+ ">");
			settings = nch.update_settings_from_db(settings,dbm);
			String xml = nch.get_settings_as_xml_string(settings);
			HTTP_Response res = psn_con.perform_post(settings,node_url,xml,"text/xml","/?config",kp);
			if (res != null) {
				dbm.updateNodeHandshake(node.get_node_id());
			}
			if (res.getErrorCode() == 202) {
				String uuid = (String)res.getBody();
				try {
					String file_path = get_settings_value( "log_path" ) + node_url + ".data";
					BufferedWriter out = new BufferedWriter(new FileWriter(file_path));
					out.write(uuid);
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				System.out.println(res.getBody());
			}
		}
		return 200;
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

	private void updateLocalInfo() {
		String network_access_id = get_settings_value("network_access_id");
		String network_private_key = get_settings_value("network_private_key");
		boolean done = dbm.register_network_key(network_access_id,network_private_key);
		
		if (!done) {
			System.out.println("FAILED to register network keys");
			System.exit(0);
		}
		
	}
	
	public static void main(String[] args) {
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
		
		PSNManager psn_man = new PSNManager(conf_file);
		psn_man.updateLocalInfo();
		psn_man.updateNetworkConfig();
	}



}
