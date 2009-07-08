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

}
