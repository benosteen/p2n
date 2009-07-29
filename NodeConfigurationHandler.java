import java.io.*;
import java.net.*;
import java.util.*;
import java.math.*;
import org.xml.sax.*;

import java.io.File;
import org.w3c.dom.Document;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

class NodeConfigurationHandler {
	
	public NodeConfigurationHandler() {
	}

	public Hashtable get_configuration_from_file(String path) {
		Hashtable settings = new Hashtable();
		try {
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			Document doc = docBuilder.parse (new File(path));
	
			doc.getDocumentElement ().normalize ();
			Element root = doc.getDocumentElement();
			for (Node node = root.getFirstChild(); node != null; node=node.getNextSibling()) {
				try {
					String node_name = "";
					String node_value = "";
					node_name = node.getNodeName();
					if (node_name.equals("node")) {
						Node inner_node = node.getFirstChild().getNextSibling();
						String node_url = (((Element)inner_node).getFirstChild()).getNodeValue();
						System.out.println("Creating node with url " + node_url);
						PSNNode psnnode = new PSNNode(node_url);
						settings = add_to_config(node_name,psnnode,settings);
						continue;
					}
					if (node_name.equals("keypair")) {
						Node inner_node = node.getFirstChild().getNextSibling();
						String access_id = (((Element)inner_node).getFirstChild()).getNodeValue();
						inner_node = inner_node.getNextSibling().getNextSibling().getFirstChild();
						String private_key  = inner_node.getNodeValue();
						Keypair kp = new Keypair(access_id,private_key);
						settings = add_to_config(node_name,kp,settings);
						continue;
					}
					if (node_name.equals("#text")) {
						continue;
					}
					node_value = ((Element)node).getFirstChild().getNodeValue();
					settings = add_to_config(node_name,node_value,settings);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return settings;
		} catch (Exception e) {
			e.printStackTrace();
			return settings;
		}
	}
	
	private Hashtable add_to_config(String key,Object value,Hashtable settings) {
		Vector current;
		try {
			current = (Vector)settings.get(key);
		} catch (Exception e) {
			current = new Vector();
		}
		if (current == null) {
			current = new Vector();
		}
		current.add(value);
		settings.put(key,current);
		return settings;
	}

	private String get_settings_value(Hashtable settings, String key) {
                try {
                        Vector v = (Vector)settings.get(key);
                        String value = (String)v.get(0);
                        return value;
                } catch (Exception e) {
                        e.printStackTrace();
                        return "";
                }
        }	

	public boolean check_local_node_settings(Hashtable settings, DatabaseConnector_Mysql dbm) {
		dbm.setCredentials(get_settings_value(settings,"database_host"),get_settings_value(settings,"database_name"),get_settings_value(settings,"database_user"),get_settings_value(settings,"database_pass"));
		
		String local_node_id = get_settings_value(settings,"node_id");
		String local_node_url = get_settings_value(settings,"node_url");
		String url_base = get_settings_value(settings,"url_base");
		int allocated_space = Integer.parseInt(get_settings_value(settings,"allocated_space"));


		if (local_node_id == null || local_node_id == ("") || local_node_url == null || local_node_url == ("") ) {
			System.out.println("Failed blank");
			return false;
		}
		
		String local_access_id = "";
		String local_private_key = "";
		Vector keypairs = (Vector)settings.get("keypair");
		for (Enumeration e = keypairs.elements(); e.hasMoreElements();) {
			Keypair kp = (Keypair)e.nextElement();
			local_access_id = kp.get_access_id();
			local_private_key = kp.get_private_key();
			if (!dbm.register_local_keys(local_access_id,local_private_key)) {
				System.out.println("Failed database checks");
				return false;
			}
			if (!dbm.register_access_key(local_node_id,local_access_id)) {
				System.out.println("Failed to localize node id, major error!");
				return false;
			}
		}


		if (!dbm.register_node_id(local_node_id,local_node_url,url_base,allocated_space)) {
			System.out.println("Failed to register node id, major error!");
			return false;
		}

		return true;
	}

	public Hashtable update_settings_from_db(Hashtable settings,DatabaseConnector_Mysql dbm) {
		String node_id = get_settings_value(settings,"node_id");
		Vector keypairs = dbm.getLocalKeyPairs(node_id,(Vector)settings.get("keypair"));
		Vector nodes = dbm.getKnownNodes(node_id,(Vector)settings.get("node"));
		settings.put("keypair",keypairs);
		settings.put("node",nodes);
		return settings;
	}

	public static void main(String[] args) {
		NodeConfigurationHandler nch = new NodeConfigurationHandler();
		Hashtable ret = nch.get_configuration_from_file("p2n.conf");
	}

}
