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

class PSNNode {
	String node_id = "";
	String node_url = "";
	String url_base = "";
	int allocated_space = 0;

	public PSNNode(String node_url) {
		this.node_url = node_url;
	}

	public void set_node_id(String node_id) {
		this.node_id = node_id;
	}

	public String get_node_id() {
		return node_id;
	}

	public void set_node_url(String node_url) {
		this.node_url = node_url;
	}

	public String get_node_url() {
		return node_url;
	}

	public void set_url_base(String url_base) {
		this.url_base = url_base;
	}

	public String get_url_base() {
		return url_base;
	}

	public void set_allocated_space(int allocated_space) {
		this.allocated_space = allocated_space;
	}

	public int get_allocated_space() {
		return allocated_space;
	}

	public Element getXMLConfig(Document doc) {
		try {
			Element root = (Element) doc.createElement("node");
			Hashtable stuff = new Hashtable();
			stuff.put("node_id",this.get_node_id());
			stuff.put("node_url",this.get_node_url());
			stuff.put("url_base",this.get_url_base());
			stuff.put("allocated_space",this.get_allocated_space());
			for (Enumeration e = stuff.keys(); e.hasMoreElements();) {
				String key = (String)e.nextElement();
				String value = null;
				if (key.equals("allocated_space")) {
					value = (Integer)stuff.get(key) + "";
				} else {
					value = (String)stuff.get(key);
				}
				if (value != "" && value != null) {
					Element sub = (Element) doc.createElement(key);
					sub.appendChild( doc.createTextNode(value) );
					root.appendChild(sub);
				}
			}
			return root;	
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
