package nl.prv.veiligheidstoets.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class URLToFile {

		/**
		 * uploads a file using an url string 
		 * 
		 * @param url string
		 * @return File object
		 * @throws Exception
		 */
		
	public static File fill(String urlstr, File file) throws Exception {
		URL url = new URL(urlstr);
		URLConnection connection = url.openConnection();
		InputStream in = connection.getInputStream();
		FileOutputStream fos = new FileOutputStream(file);
		byte[] buf = new byte[512];
		while (true) {
		    int len = in.read(buf);
		    if (len == -1) {
		        break;
		    }
		    fos.write(buf, 0, len);
		}
		in.close();
		fos.flush();
		fos.close();
		return file;
	}
	
	
	
}
