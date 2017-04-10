package nl.prv.veiligheidstoets.util;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.File;
import java.io.OutputStream;


public class StreamToFileParser{
	/**
	 * converts an InputStream into a File object
	 * 
	 * @param is, file
	 * @return a File object
	 * @throws Exception
	 */
	
	public static File parse(InputStream is, File file)throws Exception {
		File f = file;
		OutputStream os = new FileOutputStream(file);
        byte[] buffer = new byte[1024];
        int bytesRead;
        //read from is to buffer
        while((bytesRead = is.read(buffer)) !=-1){
            os.write(buffer, 0, bytesRead);
        }
        is.close();
        os.flush();
        os.close();			
		
		return f;
	}	
}
