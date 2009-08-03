import java.io.*;
import java.net.*;
import java.util.*;
import java.math.*;
import org.xml.sax.*;

import java.io.File;
import org.w3c.dom.Document;
import org.w3c.dom.*;

import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
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
			settings = get_configuration_from_root(root);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return settings;
	}

	public Hashtable get_configuration_from_root(Element root) {
		Hashtable settings = new Hashtable();
		for (Node node = root.getFirstChild(); node != null; node=node.getNextSibling()) {
			try {
				String node_name = "";
				String node_value = "";
				node_name = node.getNodeName();
				if (node_name.equals("keypair") || node_name.equals("node")) {
					Hashtable values = new Hashtable();
					for (Node inner_node = node.getFirstChild(); inner_node != null; inner_node=inner_node.getNextSibling()) {
						String inner_node_name = inner_node.getNodeName();
						if (inner_node_name.equals("#text")) {
							continue;
						}
						String inner_node_val = ((Element)inner_node).getFirstChild().getNodeValue();
						values.put(inner_node_name,inner_node_val);
					}
					if (node_name.equals("keypair")) {
						Keypair kp = new Keypair((String)values.get("access_id"),(String)values.get("private_key"));
						settings = add_to_config(node_name,kp,settings);
						continue;
					} else if (node_name.equals("node")) {
						PSNNode psnnode = new PSNNode((String)values.get("node_url"));
						settings = add_to_config(node_name,psnnode,settings);
						continue;
					}
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
	}
	
	public Node get_node_from_settings(Hashtable settings) {
		try {
			DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
			DocumentBuilder bd = fact.newDocumentBuilder();
			Document doc = bd.newDocument();
			Element root = (Element) doc.createElement("config");
			doc.appendChild(root);

			for (Enumeration keys = settings.keys(); keys.hasMoreElements();) {
				String key = (String)keys.nextElement();
				Vector locals = get_local_conf_keys();
				String value = null;
				Element elem = null;
				if (locals.contains(key)) {
					continue;
				}
				Vector v = (Vector)settings.get(key);
				try {
					value = (String) v.get(0);
				} catch (ClassCastException cle) {
					for (int i=0; i<v.size(); i++) {
						if (key.equals("node")) {
							PSNNode xnode = (PSNNode)v.get(i);
							root.appendChild(xnode.getXMLConfig(doc));
						} 
						if (key.equals("keypair")) {
							Keypair xnode = (Keypair)v.get(i);
							root.appendChild(xnode.getXMLConfig(doc));
						} 

					}
				} finally {
					if (value != null) {
						System.out.println(key + " = " + value);
						elem = (Element) doc.createElement(key);
						elem.appendChild( doc.createTextNode(value) );
						root.appendChild(elem);
					}
				}
			}
			return root;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public String get_settings_as_xml_string(Hashtable settings) {
		Node node = get_node_from_settings(settings);
		String representation = xmlToString(node);
		return representation;
	}

	public Vector get_local_conf_keys() {
		Vector v = new Vector();
		try {
			BufferedReader in = new BufferedReader(new FileReader("local_keys.conf"));
			String line = in.readLine();
			while (line != "" && line != null) {
				v.add(line);
				line = in.readLine();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return v;
	}

	public String xmlToString(Node node) {
		try {
			Source source = new DOMSource(node);
			StringWriter stringWriter = new StringWriter();
			Result result = new StreamResult(stringWriter);
			TransformerFactory factory = TransformerFactory.newInstance();
			Transformer transformer = factory.newTransformer();
			transformer.transform(source, result);
			return stringWriter.getBuffer().toString();
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}
		return null;
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

	public String get_settings_value(Hashtable settings, String key) {
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
