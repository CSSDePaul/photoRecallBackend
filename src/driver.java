
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.mysql.jdbc.Connection;

public class driver {

	public static void main(String[] args) throws ClassNotFoundException,
			IOException {
		Class.forName("com.mysql.jdbc.Driver");
		try {
			Connection con = (Connection) DriverManager.getConnection(
					"jdbc:mysql://localhost:3306/photodb", "root", "");
			Server s = new Server(con);
			con.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
