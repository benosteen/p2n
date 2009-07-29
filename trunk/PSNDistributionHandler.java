import java.io.*;
import java.net.*;
import java.util.*;
import java.math.*;
import org.xml.sax.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import org.xml.sax.helpers.XMLReaderFactory;

public class PSNDistributionHandler {
	private String node_id = "1001";
	private String node_url = "yomiko.ecs.soton.ac.uk:8452";
	
	private String url_base = "storage.p2n.org";
	private String namespace_prefix = "x-amz";
	private String base_path = "data/";
	private DatabaseConnector_Mysql dbm = new DatabaseConnector_Mysql();

	public PSNDistributionHandler() {
	}
	
	public void checkNew() {
		Vector uuids = dbm.get_psn_uuids(node_id);
		Enumeration e = uuids.elements();
		while ( e.hasMoreElements() ) {
			String uuid = (String)e.nextElement();
			System.out.println("To be distributed : " + uuid);
		}
	
	}

	public static void main(String[] args) {
		PSNDistributionHandler psndh = new PSNDistributionHandler();
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
