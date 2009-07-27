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

	public PSNManager(String conf_file) {
		NodeConfigurationHandler nch = new NodeConfigurationHandler();
		settings = nch.get_configuration_from_file(conf_file);
		node_id = get_settings_value("node_id");
		node_url =  get_settings_value("node_url");
		String node_port = get_settings_value("node_port");
		if (!node_port.equals("") || !(node_port == null)) {
			node_url = node_url + ":" + node_port;
		}
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
	}



}
