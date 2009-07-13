import java.util.*;
import java.io.*;
import java.sql.*;

public class DatabaseConnector_Mysql {
	String server = "localhost";
	String database = "dct05r_p2n_node1";
	String username = "p2n";
	String password = "p2n_default";

	public DatabaseConnector_Mysql() {
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

	public boolean update_file_cache(String uuid,String store_path,String md5,String mime_type,String type,String node_id) {
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
			PreparedStatement pstmt3 = con.prepareStatement("UPDATE mappings set " +type+"_copy=1 where uuid=?;");
			pstmt3.setString(1,uuid);
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
