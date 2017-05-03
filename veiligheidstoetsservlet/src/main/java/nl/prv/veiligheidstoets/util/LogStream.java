package nl.prv.veiligheidstoets.util;

import java.io.ByteArrayOutputStream;

public class LogStream {
private ByteArrayOutputStream logStream = null;	
	
    public LogStream(ByteArrayOutputStream logStream){
    	this.logStream = logStream; 	
    }
    
    public void write(String logLine){
    	try{
    		logStream.write(logLine.getBytes());
    	}
    	catch(Exception e){	
    		e.printStackTrace();
    	}
    }
 
    public ByteArrayOutputStream getStream(){
    	 return logStream;
     }
}