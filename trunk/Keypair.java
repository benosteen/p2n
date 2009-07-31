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

class Keypair {
	String access_id = "";
	String key = "";
	public Keypair(String access_id, String key) {
		this.access_id = access_id;
		this.key = key;
	}

	public String get_access_id() {
		return access_id;
	}

	public String get_private_key() {
		return key;
	}
	
	public Element getXMLConfig(Document doc) {
		try {
			Element root = (Element) doc.createElement("keypair");
			Hashtable stuff = new Hashtable();
			stuff.put("access_id",this.get_access_id());
			stuff.put("private_key",this.get_private_key());
			for (Enumeration e = stuff.keys(); e.hasMoreElements();) {
				String key = (String)e.nextElement();
				String value = (String)stuff.get(key);
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
