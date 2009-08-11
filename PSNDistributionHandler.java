import java.io.*;
import java.net.*;
import java.util.*;
import java.math.*;
import org.xml.sax.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import org.xml.sax.helpers.XMLReaderFactory;

public class PSNDistributionHandler {
	private DatabaseConnector_Mysql dbm = new DatabaseConnector_Mysql();
	private NodeConfigurationHandler nch = new NodeConfigurationHandler();
	private String conf_file;
	Hashtable settings = new Hashtable();

	public PSNDistributionHandler(String conf_file) {
		this.conf_file = conf_file;
		settings = nch.get_configuration_from_file(conf_file);
		dbm.setCredentials(nch.get_settings_value(settings,"database_host"),nch.get_settings_value(settings,"database_name"),nch.get_settings_value(settings,"database_user"),nch.get_settings_value(settings,"database_pass"));
	}
	
	public void checkNew() {
		String node_id = nch.get_settings_value(settings,"node_id");
		System.out.println("node id = " + node_id);
		Vector uuids = dbm.get_psn_uuids(node_id);
		Enumeration e = uuids.elements();
		while ( e.hasMoreElements() ) {
			String uuid = (String)e.nextElement();
			System.out.println("To be distributed : " + uuid);
			PSNDistributor psnd = new PSNDistributor(settings,uuid);
			psnd.run();

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
		
		PSNDistributionHandler psndh = new PSNDistributionHandler(conf_file);
		while (true) {
			psndh.checkNew();
			try {
				Thread.sleep(10000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
