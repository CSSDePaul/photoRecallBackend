import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Blob;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;

import com.mysql.jdbc.Connection;

public class doCommands implements Runnable {
	private Socket server;
	public final double MIN_LAT = Math.toRadians(-90);
	public final double MAX_LAT = Math.toRadians(90);
	public final double MIN_LON = Math.toRadians(-180);
	public final double MAX_LON = Math.toRadians(180);
	private double minLa;
	private double maxLa;
	private double minLo;
	private double maxLo;
	private Connection con;
	private DataInputStream in;
	private DataOutputStream toClient;
	private LinkedList<Thread> threads;

	public doCommands(Socket s) throws IOException, SQLException, ClassNotFoundException {
		server = s;
		Class.forName("com.mysql.jdbc.Driver");
		con = (Connection) DriverManager.getConnection(
				"jdbc:mysql://localhost:3306/photodb", "root", "");		
	}

	public void clientPosts() throws IOException {
		double lat = in.readDouble();
		double lon = in.readDouble();
		String fName = in.readUTF();
		long sz = in.readLong();
		byte[] im = new byte[(int) sz];
		long c = sz;
		while (c > 0) {
			im[(int) (sz - c)] = in.readByte();
			c--;
		}
		
		//insert into database
		String insert = "INSERT INTO 'photos' (Filename, Latitude, Longitude, photo, size) values ("
				+ fName + ", " + lat + ", " + lon + ", " + im + ", " + sz+")";
		
		Thread t1 = new Thread(new AddtoDatabase(con, insert));
		t1.start();
		
	}

	private void clientGets() throws IOException, SQLException {
		// client sends
		double lat = in.readDouble();
		double lon = in.readDouble();
		// prep to send to database
		createRadius(0.03048, lat, lon);// calibrate to (100)ft in kms
		String query = "SELECT 'photo' FROM 'photos' WHERE '" + maxLa
				+ " > 'Latitude' AND 'Latitude' < " + minLa + " AND " + maxLo
				+ " > 'Longitude' AND 'Longitude' < " + minLo;
		Statement st = con.createStatement();
		ResultSet results = st.executeQuery(query);
		int i = 1;
		threads = new LinkedList<Thread>();
		while (results.next()) {
			Blob b = results.getBlob("photo");
			byte[] pic = b.getBytes(1, (int) b.length());
			Thread t1 = new Thread(new Uploader(server, pic, b.length()));
			t1.start();// start uploading and send the next one
			threads.add(t1);
		}
		// wait for everything to stop uploading
		for (Thread t : threads) {
			try {
				t.join();
				threads = null;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

	private void createRadius(double d, double lat, double lon) {
		double r = d / 6378.1;
		double radLat = Math.toRadians(lat);
		double radLon = Math.toDegrees(lon);
		double minLat = radLat - r;
		double maxLat = radLat + r;
		double minLon = 0;
		double maxLon = 0;
		double deltaLon = Math.asin(Math.sin(radLon) / Math.cos(radLon));
		if (minLat > MIN_LON && maxLat < MAX_LAT) {
			minLon = radLon - deltaLon;
			maxLon = radLon + deltaLon;
			if (minLon < MIN_LON) {
				minLon += 2 * Math.PI;
			}
			if (maxLon > MAX_LON) {
				maxLon -= 2 * Math.PI;
			}

		} else {
			minLat = Math.max(minLat, MIN_LAT);
			maxLat = Math.min(maxLat, MAX_LAT);
			minLon = MIN_LON;
			maxLon = MAX_LON;
		}

		minLa = minLat;
		maxLa = maxLat;
		minLo = minLon;
		maxLo = maxLon;
	}

	class AddtoDatabase implements Runnable{
		private Connection con;
		private String insert;
		
		public AddtoDatabase(Connection c, String sql){
			con = c;
			insert = sql;
		}

		@Override
		public void run() {
			try {
				Statement s = con.createStatement();
				ResultSet rs = s.executeQuery(insert);
				if(rs.next()){
					System.out.println("Insert done!");
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	
	class Uploader implements Runnable {
		private Socket r;
		private DataOutputStream out;
		private byte[] im;
		private long imbytes;

		public Uploader(Socket rms, byte[] b, long bs) throws IOException {
			r = rms;
			im = b;
			imbytes = bs;
			out = new DataOutputStream(r.getOutputStream());
		}

		@Override
		public void run() {
			try {
				// tell client how many bytes are comming
				out.writeLong(imbytes);
				int c = (int) imbytes;
				while (c > 0) {
					out.writeByte(im[(int) (imbytes - c)]);
					c--;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

	}

	@Override
	public void run() {
		try {
			in = new DataInputStream(server.getInputStream());
			toClient = new DataOutputStream(server.getOutputStream());
			
			//begin typical usecase script
			while(!server.isClosed()){
				String command = in.readUTF();
				if(command.equals("get")){
					try {
						clientGets();
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}else if(command.equals("post")){
					clientPosts();
				}else{
					toClient.writeUTF(command);
				}
					
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
