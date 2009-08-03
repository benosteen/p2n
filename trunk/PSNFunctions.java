import com.eaio.uuid.UUID;
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

class PSNFunctions {
	
	private String namespace_prefix = "x-amz";

	public PSNFunctions() {
	}

	private String getDate() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date date = new Date();
		return dateFormat.format(date);
	}

	public String getDateTime() {
		SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
		format.setCalendar(Calendar.getInstance(new SimpleTimeZone(0, "GMT")));
		return format.format(new Date());

		//DateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
		//Date date = new Date();
		//return dateFormat.format(date) + " GMT";
	}

	public Long getDateTimeUnix() {
		TimeZone tz = TimeZone.getTimeZone("UTC");
		Calendar cal = new GregorianCalendar(tz);
		cal.setTime(new Date());
		return ((cal.getTime().getTime())/1000);
	}

	private String get_requested_path(Hashtable request_ht,Hashtable settings) {
		NodeConfigurationHandler nch = new NodeConfigurationHandler();
		String url_base = nch.get_settings_value(settings,"url_base");
		String node_url = nch.get_settings_value(settings,"node_url");
		try {
			node_url = node_url.substring(0,node_url.indexOf(":"));
		} catch (Exception e) {
		}
		String host_part = (String)request_ht.get("host");
		String cache = host_part;
		try {
			host_part = host_part.replace(url_base,"");
			host_part = host_part.replace(node_url,"");
		} catch (Exception e) {
			host_part = cache;
		}
		try {
			if (host_part.indexOf(":") > 0) {
				host_part = host_part.substring(0,host_part.indexOf(":"));
			}
			if (host_part.substring(host_part.length()-1,host_part.length()).equals(".")) {
				host_part = host_part.substring(0,host_part.length()-1);
			}
			if (!host_part.equals("")) {
				host_part = "/" + host_part;
			}
		} catch (Exception e) {
		}
		String uri = (String)request_ht.get("uri");
		if (uri.indexOf("?") > -1 && (uri.indexOf("=") > uri.indexOf("?"))) {
			uri = uri.substring(0,uri.indexOf("?"));
		}
		if (host_part != null) {
			return host_part + uri;
		} else {
			return uri;
		}
	}

	public String getStringToSign(Hashtable request_ht, Hashtable settings) {

		String string_to_sign="";
		Hashtable amz_values = new Hashtable();
		Vector amz_keys = new Vector();
		Enumeration keys = request_ht.keys();
		while ( keys.hasMoreElements() )
		{
			String key = (String)keys.nextElement();
			if (key.length() > 4) {
				String lkey = key.toLowerCase();
				if (lkey.substring(0,namespace_prefix.length()).equals(namespace_prefix)) {
					amz_keys.add(lkey);
					amz_values.put(lkey,(String)request_ht.get(key));
				}
			}
		}
		Collections.sort(amz_keys);

		try {
			String type = (String)request_ht.get("type");
			string_to_sign += type + "\n";
			if (type.equals("GET") || type.equals("HEAD")) {
				string_to_sign += "\n\n";
				string_to_sign += (String)request_ht.get("date") + "\n";
				for(int i=0; i<amz_keys.size(); i++) {
					String local_key = ((String)amz_keys.get(i));
					string_to_sign += local_key + ":" + (String)amz_values.get(local_key) + "\n";
				}
			} else if (type.equals("PUT") || type.equals("POST")) {
				if (request_ht.containsKey("content-md5")) {
					string_to_sign += (String)request_ht.get("content-md5") + "\n";
				} else {
					string_to_sign += "\n";
				}
				if (request_ht.containsKey("content-type")) {
					string_to_sign += (String)request_ht.get("content-type") + "\n";
				} else {
					string_to_sign += "\n";
				}
				string_to_sign += (String)request_ht.get("date") + "\n";
				for(int i=0; i<amz_keys.size(); i++) {
					String local_key = ((String)amz_keys.get(i));
					string_to_sign += local_key + ":" + (String)amz_values.get(local_key) + "\n";
				}
			} else if (type.equals("DELETE")) {
				string_to_sign += "\n";
				string_to_sign += "\n";
				if ((String)amz_values.get(namespace_prefix + "-date") == null) {
					string_to_sign += (String)request_ht.get("date") + "\n";
				} else {
					string_to_sign += "\n";
					string_to_sign += namespace_prefix + "-date:" + (String)amz_values.get(namespace_prefix + "-date") + "\n";
				}
			}

			String requested_path = get_requested_path(request_ht,settings);
			string_to_sign += requested_path;
			return string_to_sign;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";


	/**
	 * Computes RFC 2104-compliant HMAC signature.
	 * * @param data
	 * The data to be signed.
	 * @param key
	 * The signing key.
	 * @return
	 * The Base64-encoded RFC 2104-compliant HMAC signature.
	 * @throws
	 * java.security.SignatureException when signature generation fails
	 */
	public static String calculateRFC2104HMAC(String data, String key)
		throws java.security.SignatureException
		{
			String result;
			try {

				// get an hmac_sha1 key from the raw key bytes
				SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), HMAC_SHA1_ALGORITHM);

				// get an hmac_sha1 Mac instance and initialize with the signing key
				Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
				mac.init(signingKey);

				// compute the hmac on input data bytes
				byte[] rawHmac = mac.doFinal(data.getBytes());

				// base64-encode the hmac
				result = Encoding.EncodeBase64(rawHmac);

			} catch (Exception e) {
				throw new SignatureException("Failed to generate HMAC : " + e.getMessage());
			}
			return result;
		}


}
