import java.util.*;
import java.io.*;
import java.sql.*;

public class DatabaseConnector_Mysql {
	String server;
	String database;
	String username;
	String password;

	public DatabaseConnector_Mysql() {
	}

	public void setCredentials(String server, String database, String username, String password) {
		this.server = server;
		this.database = database;
		this.username = username;
		this.password = password;
	}

	public Long getDateTimeUnix() {
		TimeZone tz = TimeZone.getTimeZone("UTC");
		Calendar cal = new GregorianCalendar(tz);
		cal.setTime(new java.util.Date());
		return ((cal.getTime().getTime())/1000);
	}

	private Connection connectMysql() throws Exception {
		Class.forName("com.mysql.jdbc.Driver").newInstance();
		String connection_url = "jdbc:mysql://" + server + "/" + database;
		Connection con = DriverManager.getConnection(connection_url,username,password);
		return con;
	}

	private void disconnectMysql(Connection con) throws Exception {
		con.close();
	}

	public int userExists(String access_id) {
		try {
			Connection con = connectMysql();
			PreparedStatement pstmt = con.prepareStatement("SELECT count(*) as total from Users where access_id=?;");
			pstmt.setString(1,access_id);
			ResultSet rs = pstmt.executeQuery();
			rs.first();
			int size = rs.getInt("total");
			disconnectMysql(con);
			if (size < 1) {
				return 403;
			} else {
				return 200;
			}
		} catch (Exception e) {
			return 500;
		}
	}
	
	public String getPrivateKey(String access_id) {
		try {
			Connection con = connectMysql();
			PreparedStatement pstmt = con.prepareStatement("SELECT private_key from Users where access_id=?;");
			pstmt.setString(1,access_id);
			ResultSet rs = pstmt.executeQuery();
			rs.first();
			String key = rs.getString("private_key");
			disconnectMysql(con);
			return key;
		} catch (Exception e) {
			return "500";
		}

	}
	public String get_uuid_from_request(String access_id,String requested_path) {
		Connection con;
		ResultSet rs;
		try {
			con = connectMysql();
			PreparedStatement pstmt = con.prepareStatement("SELECT uuid from mappings where access_id=? and requested_path=?;");
			pstmt.setString(1,access_id);
			pstmt.setString(2,requested_path);
			rs = pstmt.executeQuery();
		} catch (Exception e) {
			return "500";
		} 
		try {
			rs.first();
			String key = rs.getString("uuid");
			disconnectMysql(con);
			return key;
		} catch (Exception e) {
			return "404";
		}
	}
	
	/**
	  * Updates the network key, plain overwrite method!
	  */
	public boolean register_network_key(String access_id,String private_key) {
		Connection con;
		try {
			con = connectMysql();
			PreparedStatement pstmt = con.prepareStatement("DELETE from Users where user_type='network';");
			pstmt.execute();
			pstmt = con.prepareStatement("INSERT into Users set access_id=?, private_key=?, user_type=?");
			pstmt.setString(1,access_id);
			pstmt.setString(2,private_key);
			pstmt.setString(3,"network");
			pstmt.execute();
			disconnectMysql(con);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean register_access_key(String node_id, String access_id) {
		Connection con;
		try {
			con = connectMysql();
			PreparedStatement pstmt = con.prepareStatement("DELETE from node_associations where access_id_owned=?");
			pstmt.setString(1,access_id);
			pstmt.execute();
			pstmt = con.prepareStatement("INSERT into node_associations set node_id=?, access_id_owned=?, last_update=?");
			pstmt.setString(1,node_id);
			pstmt.setString(2,access_id);
			pstmt.setLong(3,getDateTimeUnix());
			pstmt.execute();
			disconnectMysql(con);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
		
	}

	public boolean register_node_id(String node_id,String node_url,String url_base,int allocated_space){
		Connection con;
		ResultSet rs;
		try {
			con = connectMysql();
			PreparedStatement pstmt = con.prepareStatement("SELECT id from nodes where id=? or url=?;");
			pstmt.setString(1,node_id);
			pstmt.setString(2,node_url);
			rs = pstmt.executeQuery();
			int count = 0;
			boolean state = true;
			while (rs.next()) {
				count++;
				if (rs.getString("id").equals(node_id)) {
					state = true;
				}
			}
			if (count > 1) {
				disconnectMysql(con);
				return false;
			}
			if (count == 0) {
				pstmt = con.prepareStatement("INSERT INTO nodes set id=?, url=?, url_base=?, allocated_space=?;");
				pstmt.setString(1,node_id);
				pstmt.setString(2,node_url);
				pstmt.setString(3,url_base);
				pstmt.setInt(4,allocated_space);
			} else if (state == true) {
				pstmt = con.prepareStatement("UPDATE nodes set url=?, url_base=?, allocated_space=? where id=?;");
				pstmt.setString(1,node_url);
				pstmt.setString(2,url_base);
				pstmt.setInt(3,allocated_space);
				pstmt.setString(4,node_id);
			}
			pstmt.execute();
			disconnectMysql(con);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
		
		
	}

	public boolean register_local_keys(String access_id,String private_key) {
		Connection con;
		ResultSet rs;
		try {
			con = connectMysql();
			PreparedStatement pstmt = con.prepareStatement("SELECT access_id,private_key from Users where user_type='local';");
			rs = pstmt.executeQuery();
			int count = 0;
			while (rs.next()) {
				count++;
				if (rs.getString("access_id").equals(access_id) && rs.getString("private_key").equals(private_key)) {
					disconnectMysql(con);
					return true;
				}
			}
			if (count > 0) {
				disconnectMysql(con);
				return false;
			} 
			pstmt = con.prepareStatement("INSERT INTO Users set access_id=?, private_key=?, user_type='local';");
			pstmt.setString(1,access_id);
			pstmt.setString(2,private_key);
			pstmt.execute();
			disconnectMysql(con);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
		
	}
	public boolean store_uuid_mapping(String access_id,String requested_path,String uuid,String acl) {
		try {
			Connection con = connectMysql();
			PreparedStatement pstmt = con.prepareStatement("INSERT INTO mappings set access_id=?, requested_path=?, uuid=?,acl=?");
			pstmt.setString(1,access_id);
			pstmt.setString(2,requested_path);
			pstmt.setString(3,uuid);
			pstmt.setString(4,acl);
			pstmt.execute();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} 
		
	}
	
	public void write_metadata(String uuid,String outkey,String value) {
		try {
			Connection con = connectMysql();
			PreparedStatement pstmt = con.prepareStatement("INSERT INTO object_metadata set mapping_uuid=?, word=?, value=?");
			pstmt.setString(1,uuid);
			pstmt.setString(2,outkey);
			pstmt.setString(3,value);
			pstmt.execute();
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
	
	public void unset_local_copy(String uuid) {
		try {
			Connection con = connectMysql();
			PreparedStatement pstmt = con.prepareStatement("UPDATE mappings set local_copy=0 where uuid=?;");
			pstmt.setString(1,uuid);
			pstmt.execute();
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
	
	public void unset_p2n_copy(String uuid) {
		try {
			Connection con = connectMysql();
			PreparedStatement pstmt = con.prepareStatement("UPDATE mappings set psn_copy=0 where uuid=?;");
			pstmt.setString(1,uuid);
			pstmt.execute();
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}

	public boolean delete_uuid(String uuid) throws Exception {
		Connection con = connectMysql();
		try {
			con.setAutoCommit(false);
			Statement sta = con.createStatement();
			int startTransaction = sta.executeUpdate("START TRANSACTION;");
			PreparedStatement pstmt = con.prepareStatement("DELETE FROM mappings where uuid=?;");
			pstmt.setString(1,uuid);
			pstmt.execute();
			pstmt = con.prepareStatement("DELETE FROM files where mapping_uuid=?;");
			pstmt.setString(1,uuid);
			pstmt.execute();
			pstmt = con.prepareStatement("DELETE FROM object_metadata where mapping_uuid=?;");
			pstmt.setString(1,uuid);
			pstmt.execute();
			pstmt = con.prepareStatement("DELETE FROM node_files where mapping_uuid=?");
			pstmt.setString(1,uuid);
			pstmt.execute();
			con.commit();
			con.close();
			return true;
		} catch (Exception e) {
			con.rollback();
			e.printStackTrace();
			con.close();
			return false;
		} 
		
	}

	public int delete_uuid_mapping(String uuid,String type,String path,String node_id) throws Exception {
		Connection con = connectMysql();
		try {
			con.setAutoCommit(false);
			Statement sta = con.createStatement();
			int startTransaction = sta.executeUpdate("START TRANSACTION;");
			if (type.equals("local")) {
				PreparedStatement pstmt = con.prepareStatement("DELETE FROM mappings where uuid=?;");
				pstmt.setString(1,uuid);
				pstmt.execute();
			}
			PreparedStatement pstmt = con.prepareStatement("DELETE FROM files where mapping_uuid=? and type=? and path=?;");
			pstmt.setString(1,uuid);
			pstmt.setString(2,type);
			pstmt.setString(3,path);
			pstmt.execute();
			pstmt = con.prepareStatement("DELETE FROM object_metadata where mapping_uuid=?;");
			pstmt.setString(1,uuid);
			pstmt.execute();
			pstmt = con.prepareStatement("DELETE FROM node_files where mapping_uuid=? and type=? and node_id=?;");
			pstmt.setString(1,uuid);
			pstmt.setString(2,type);
			pstmt.setString(3,node_id);
			pstmt.execute();
			con.commit();
			con.close();
			return 100;
		} catch (Exception e) {
			con.rollback();
			con.close();
			e.printStackTrace();
			return 500;
		} 
	}

	public String get_content_type(String uuid,String type) {
		try {
			Connection con = connectMysql();
			PreparedStatement pstmt = con.prepareStatement("SELECT mime_type from files where mapping_uuid=? and type=?;");
			pstmt.setString(1,uuid);
			pstmt.setString(2,type);
			ResultSet rs = pstmt.executeQuery();
			rs.first();
			String content_type = rs.getString("mime_type");
			disconnectMysql(con);
			return content_type;
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		} 

	}

	public boolean update_file_cache(String uuid,String store_path,String md5,String mime_type,String type,String node_id, String psndis, String psnres) {
		try {
			Connection con = connectMysql();
			PreparedStatement pstmt = con.prepareStatement("INSERT INTO files set mapping_uuid=?,path=?,md5_sum=?,mime_type=?,type=?;");
			pstmt.setString(1,uuid);
			pstmt.setString(2,store_path);
			pstmt.setString(3,md5);
			pstmt.setString(4,mime_type);
			pstmt.setString(5,type);
			PreparedStatement pstmt2 = con.prepareStatement("INSERT INTO node_files set node_id=?,mapping_uuid=?,type=?;");
			pstmt2.setString(1,node_id);
			pstmt2.setString(2,uuid);
			pstmt2.setString(3,type);
			PreparedStatement pstmt3 = con.prepareStatement("UPDATE mappings set " +type+"_copy=1,psn_distribution=?,psn_resiliance=? where uuid=?;");
			pstmt3.setString(3,uuid);
			if (psndis == null) {
				pstmt3.setInt(1,0);
				pstmt3.setInt(2,0);
			} else {
				pstmt3.setInt(1,Integer.parseInt(psndis));
				pstmt3.setInt(2,Integer.parseInt(psnres));
			}
			pstmt.execute();
			pstmt2.execute();
			pstmt3.execute();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} 
		
	}
	
	public String get_uuid_from_requested_path(String requested_path) {
		try {
			Connection con = connectMysql();
			PreparedStatement pstmt = con.prepareStatement("SELECT uuid from mappings where requested_path=?;");
			pstmt.setString(1,requested_path);
			ResultSet rs = pstmt.executeQuery();
			rs.first();
			String uuid = rs.getString("uuid");
			disconnectMysql(con);
			return uuid;
		} catch (Exception e) {
			return "404";
		}
	}
	
	public boolean has_local_copy(String uuid) {
		try {
			Connection con = connectMysql();
			PreparedStatement pstmt = con.prepareStatement("SELECT local_copy from mappings where uuid=?;");
			pstmt.setString(1,uuid);
			ResultSet rs = pstmt.executeQuery();
			rs.first();
			int lc = rs.getInt("local_copy");
			disconnectMysql(con);
			if (lc > 0) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			return false;
		}
	}
	
	public boolean has_psn_copy(String uuid) {
		try {
			Connection con = connectMysql();
			PreparedStatement pstmt = con.prepareStatement("SELECT psn_copy from mappings where uuid=?;");
			pstmt.setString(1,uuid);
			ResultSet rs = pstmt.executeQuery();
			rs.first();
			int lc = rs.getInt("local_copy");
			disconnectMysql(con);
			if (lc > 0) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			return false;
		}
	}

	public boolean public_access(String uuid) {
		try {
			Connection con = connectMysql();
			PreparedStatement pstmt = con.prepareStatement("SELECT acl from mappings where uuid=?;");
			pstmt.setString(1,uuid);
			ResultSet rs = pstmt.executeQuery();
			rs.first();
			String acl = rs.getString("acl");
			disconnectMysql(con);
			if (acl.equals("public-read")) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			return false;
		}
	}
	
	public Vector getLocalKeyPairs(String node_id, Vector vec) {
		try {
			Connection con = connectMysql();
			PreparedStatement pstmt = con.prepareStatement("select access_id,private_key from Users inner join node_associations on node_associations.access_id_owned=Users.access_id where node_associations.node_id=?;");
			pstmt.setString(1,node_id);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				Keypair kp = new Keypair(rs.getString("access_id"),rs.getString("private_key"));
				boolean done = false;
				for (Enumeration e = vec.elements(); e.hasMoreElements();) {
					Keypair present = (Keypair)e.nextElement();
					if (present.get_access_id().equals(rs.getString("access_id"))) {
						done = true;
					}
				}
				if (!done) {
					vec.add(kp);
				}
			}
			disconnectMysql(con);
			return vec;

		} catch (Exception e) {
			e.printStackTrace();
			return vec;
		}
	}
	
	public Vector getKnownNodes(String node_id,Vector vec) {
		try {
			Connection con = connectMysql();
			PreparedStatement pstmt = con.prepareStatement("select id,url,url_base,allocated_space from nodes where id!=?;");
			pstmt.setString(1,node_id);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				PSNNode node = new PSNNode(rs.getString("url"));
				node.set_node_id(rs.getString("id"));
				node.set_url_base(rs.getString("url_base"));
				node.set_allocated_space(rs.getInt("allocated_space"));
				boolean done = false;
				for (Enumeration e = vec.elements(); e.hasMoreElements();) {
					PSNNode present = (PSNNode)e.nextElement();
					if (present.get_node_url().equals(rs.getString("url"))) {
						done = true;
					}
				}
				if (!done) {
					vec.add(node);
				}
			}
			disconnectMysql(con);
			return vec;

		} catch (Exception e) {
			e.printStackTrace();
			return vec;
		}
	}
	
	public Hashtable get_object_metadata(String uuid) {
		Hashtable ht = new Hashtable();
		try {
			Connection con = connectMysql();
			PreparedStatement pstmt = con.prepareStatement("SELECT word,value from object_metadata where mapping_uuid=?;");
			pstmt.setString(1,uuid);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				ht.put(rs.getString("word"),rs.getString("value"));
			}
			pstmt = con.prepareStatement("SELECT local_copy,psn_copy from mappings where uuid=?;");
			pstmt.setString(1,uuid);
			rs = pstmt.executeQuery();
			rs.first();
			String lc = "0";
			try {
				lc = "" + rs.getInt("local_copy");
			} catch (Exception e) {
			}
			ht.put("LocalCopy",lc);
			String psn = "0";
			try {
				psn = "" + rs.getInt("psn_copy");
			} catch (Exception e) {
			}
			ht.put("PSNCopy",psn);
			return ht;
		} catch (Exception e) {
			e.printStackTrace();
			return ht;
		}
	}
	
	public Vector get_psn_uuids(String node_id) {
		Vector vec = new Vector();
		try {
			Connection con = connectMysql();
			PreparedStatement pstmt = con.prepareStatement("select uuid from mappings inner join node_associations on nodes_associations.access_id_owned=mappings.access_id where node_associations.node_id=? and ISNULL(psn_copy) and psn_distribution>0;");
			pstmt.setString(1,node_id);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				vec.add(rs.getString("uuid"));
			}
			return vec;
		} catch (Exception e) {
			e.printStackTrace();
			return vec;
		}
	}


	public String get_local_path_from_uuid(String uuid) {
		try {
			Connection con = connectMysql();
			PreparedStatement pstmt = con.prepareStatement("SELECT path from files where mapping_uuid=? and type=?;");
			pstmt.setString(1,uuid);
			pstmt.setString(2,"local");
			ResultSet rs = pstmt.executeQuery();
			rs.first();
			String path = rs.getString("path");
			disconnectMysql(con);
			return path;
		} catch (Exception e) {
			e.printStackTrace();
			return "404";
		}
	}

}
