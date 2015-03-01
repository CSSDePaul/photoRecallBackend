
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.mysql.fabric.Server;
import com.mysql.jdbc.Connection;

public class driver {
	final static int PORT = 4444;
	public static void main(String[] args) throws ClassNotFoundException,
			IOException {
	
			
			
			int i = 0;
			try{
			 ServerSocket listener = new ServerSocket(PORT);	
			 Socket server;
			 int maxConnections=0;
			 while((i++<maxConnections)||(maxConnections ==0)){
				 doCommands connection;
				 server=listener.accept();
				 doCommands conc = new doCommands(server);
				 Thread t = new Thread(conc);
				 t.start();
				 
			 }
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
