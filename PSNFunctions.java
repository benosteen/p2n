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

	public String getDate() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date date = new Date();
		return dateFormat.format(date);
	}
	
	public String getTime() {
		SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
		format.setCalendar(Calendar.getInstance(new SimpleTimeZone(0, "GMT")));
		return format.format(new Date());
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

	
	 public PSNReturnObject process_input(Vector input_lines) {

		Hashtable request_ht = new Hashtable();
		request_ht.put("content-length","0");

		PSNReturnObject psn_ro = new PSNReturnObject();
		psn_ro.setObject(request_ht);

                String line = (String)input_lines.get(0);

                String[] request = line.split(" ");

                if (request[0].equalsIgnoreCase("HEAD") || request[0].equalsIgnoreCase("GET") || request[0].equalsIgnoreCase("POST") || request[0].equalsIgnoreCase("PUT") || request[0].equalsIgnoreCase("DELETE") || request[0].equalsIgnoreCase("TRACE") || request[0].equalsIgnoreCase("OPTIONS") || request[0].equalsIgnoreCase("CONNECT")) {

                        request_ht.put("type",request[0]);

                        String uri = request[1];

                        if (uri.indexOf("?") > 0) {

                                String uri_params = uri.substring(uri.indexOf("?")+1,uri.length());
                                String[] uri_params_bits = uri_params.split("&");
                                String aws_access_id = "";
                                String aws_signature = "";
                                String expires = "";

                                int success_count = 0;

                                for (int i=0; i<uri_params_bits.length; i++) {

                                        String[] sides = uri_params_bits[i].split("=");

                                        if (sides[0].equals("AWSAccessKeyId")) {
                                                aws_access_id = sides[1];
                                                success_count++;
                                        }

                                        if (sides[0].equals("Signature")) {
                                                aws_signature = new URLDecoder().decode(sides[1]);
                                                success_count++;
                                        }

                                        if (sides[0].equals("Expires")) {
                                                expires = sides[1];
                                                success_count++;
					}

                                }

                                if (success_count == 3) {
                                        request_ht.put("authorization","AWS " + aws_access_id + ":" + aws_signature);
                                        request_ht.put("date",expires);
                                }

                        }

                        request_ht.put("uri",uri);
                        request_ht.put("protocal",request[2]);

                        Enumeration e = input_lines.elements();
                        e.nextElement();

                        while (e.hasMoreElements()) {

                                line = (String)e.nextElement();
                                request = line.split(":",2);

                                if (request.length < 2) {
                                        continue;
                                }

                                String to_put = "";
                                String req_key = request[0].trim().toLowerCase();

                                try {

                                        if (req_key.equals("content-length")) {
                                                to_put = ((String)request[1]).trim();
                                        } else {
                                                to_put = (String)request_ht.get(req_key);

                                                if (to_put != null) {
                                                        to_put += ",";
                                                }

                                                if (to_put != null ) {
                                                        to_put += ((String)request[1]).trim();
                                                } else {
                                                        to_put = ((String)request[1]).trim();
                                                }

                                        }
				
				} catch (Exception array_wrong) {
                                        array_wrong.printStackTrace();
                                } finally {
                                        request_ht.put(req_key,to_put);
                                }

                        }
			
			psn_ro.setErrorCode(200);
		
                        return psn_ro;

                } else {

                        System.out.println("Line process error");
			
			psn_ro.setMessage("Bad Request: Could not understand headers");
			psn_ro.setErrorCode(400);
	
                        return psn_ro;

                }

        }

	public String get_bucket_name(String host_part,String url_base) {

                String bucket = null;
	
		try {

                        bucket = host_part.substring(0,host_part.indexOf(url_base)-1);
			
                } catch (Exception e) {

                       String message = "InvalidURI: Couldn't parse the specified URI or URI of host not matched to this domain.";

                }
			
		return bucket;

        }


	public String get_requested_path(Hashtable request_ht,Hashtable settings) {

		NodeConfigurationHandler nch = new NodeConfigurationHandler();

		String url_base = nch.get_settings_value(settings,"url_base");
		String node_url = nch.get_settings_value(settings,"node_url");

		try {

			node_url = node_url.substring(0,node_url.indexOf(":"));

		} catch (Exception e) {

		}

		int check = -1;

		String host_part = (String)request_ht.get("host");

		try {

			System.out.println("HOST PART: " + host_part);
			System.out.println("URL_BASE: " + url_base);

			check = host_part.indexOf(url_base);

			if (check>-1) {

				String cache = host_part;

				try {

					host_part = host_part.replace(url_base,"");

				} catch (Exception e) {

					host_part = cache;

				}

				if (host_part.indexOf(":") > 0) {

					host_part = host_part.substring(0,host_part.indexOf(":"));

				}

				if (host_part.substring(host_part.length()-1,host_part.length()).equals(".")) {

					host_part = host_part.substring(0,host_part.length()-1);

				}

				if (!host_part.equals("")) {

					host_part = "/" + host_part;

				}

			}

		} catch (Exception e) {

		}

		String uri = (String)request_ht.get("uri");

		if (uri.indexOf("?") > -1 && (uri.indexOf("=") > uri.indexOf("?"))) {

			uri = uri.substring(0,uri.indexOf("?"));

		}

		if (host_part != null && check>-1) {

			return host_part + uri;

		} else {

			return uri;

		}

	}
	
	public String get_md5(File path) {
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			File f = path;
			InputStream is = new FileInputStream(f);				
			byte[] buffer = new byte[8192];
			int read = 0;
			while( (read = is.read(buffer)) > 0) {
				digest.update(buffer, 0, read);
			}		
			byte[] md5sum = digest.digest();
			BigInteger bigInt = new BigInteger(1, md5sum);
			String output = bigInt.toString(16);
			return output;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
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
			//System.out.println("======");
			//System.out.println(string_to_sign);
			//System.out.println("======");
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
