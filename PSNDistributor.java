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

		boolean flag = true;

		NodeConfigurationHandler nch = new NodeConfigurationHandler();
		DatabaseConnector_Mysql dbm = new DatabaseConnector_Mysql();
		dbm.setCredentials(nch.get_settings_value(settings,"database_host"),nch.get_settings_value(settings,"database_name"),nch.get_settings_value(settings,"database_user"),nch.get_settings_value(settings,"database_pass"));
		PSNObject psno = dbm.get_psn_object(uuid);
		if (!psno.getLocalCopy()) {
			return;
		}
		File f = new File(psno.getLocalPath());
		if (f.length() < 1) {
			return;
		}
		int dist = psno.getPSNDistribution();
		int res = psno.getPSNResiliance();
		int required = dist - res;
		

		if (dist < 1) {
			return;
		}

		if (dist > dbm.getNodeCount()) {
			System.out.println("LOG: Not enough nodes available to perform required distribution");
			System.out.println("At this point you should reduce the distrubution and distribute the object in this way until the desired distribution is available, or you should refuse the object earlier.");
			return;
		}
		
		String time = Long.toString(System.nanoTime());
		String path = nch.get_settings_value(settings,"data_path") + time;
		String file_name = psno.getLocalPath().substring(psno.getLocalPath().lastIndexOf("/")+1,psno.getLocalPath().length());
	
		File tempdir = new File(path);

		if (!(tempdir.mkdir())) {
			return;
		}
		String command = "ln -s " + psno.getLocalPath() + " " + time + ".tmp";
		File link = new File(time+".tmp");

		System.out.println(command);
		try {
			Process p = Runtime.getRuntime().exec(command);
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
			BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			
			String s = null;

			System.out.println("Here is the standard output of the command:\n");
			while ((s = stdInput.readLine()) != null) {
				flag = false;
				System.out.println(s);
			}

			// read any errors from the attempted command

			System.out.println("Here is the standard error of the command (if any):\n");
			while ((s = stdError.readLine()) != null) {
				flag = false;
				System.out.println(s);
			}

		} catch ( Exception e ) {
			e.printStackTrace();
		}
		
		command = "zfec " + time + ".tmp -m " + dist + " -k " + required + " -p " + file_name + " -d " + path + "/";
		System.out.println(command);

		try {
			Process p = Runtime.getRuntime().exec(command);
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
			BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			
			String s = null;

			System.out.println("Here is the standard output of the command:\n");
			while ((s = stdInput.readLine()) != null) {
				flag = false;
				System.out.println(s);
			}

			// read any errors from the attempted command

			System.out.println("Here is the standard error of the command (if any):\n");
			while ((s = stdError.readLine()) != null) {
				flag = false;
				System.out.println(s);
			}
			link.delete();
		} catch ( Exception e ) {
			e.printStackTrace();
		}
		
		if (flag == false) {
			System.out.println("FAILED");
			deleteDir(tempdir);
		}
	
		Vector done_nodes = new Vector();

		System.out.println("DONE returning");

			
	}


	public boolean deleteDir(File dir) {
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i=0; i<children.length; i++) {
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}

		// The directory is now empty so delete it
		return dir.delete();
	}


}
