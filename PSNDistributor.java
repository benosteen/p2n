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

class PSNDistributor implements Runnable {
	Hashtable settings = null;
	String uuid = null;

	public PSNDistributor(Hashtable settings,String uuid) {
		this.settings = settings;
		this.uuid = uuid;
	}

	public void run() {
		NodeConfigurationHandler nch = new NodeConfigurationHandler();
		DatabaseConnector_Mysql dbm = new DatabaseConnector_Mysql();
		dbm.setCredentials(nch.get_settings_value(settings,"database_host"),nch.get_settings_value(settings,"database_name"),nch.get_settings_value(settings,"database_user"),nch.get_settings_value(settings,"database_pass"));
		PSNObject psno = dbm.get_psn_object(uuid);
		if (psno.getLocalCopy()) {
			System.out.println("GOT THE OBJECT");	
		}
	}

}
