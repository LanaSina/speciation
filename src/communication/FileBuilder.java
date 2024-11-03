package communication;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import startup.Constants;

public class FileBuilder {
	MyLog mlog = new MyLog("fileBuilder",true);
	/**File writer*/
	FileWriter filew;
	
	public FileBuilder(String name){
		
		/**data directory*/
		String folderName;
		
		//get current date
	    DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm");
	    Date date = new Date();
	    String strDate = dateFormat.format(date);
	
		folderName = Constants.DataPath + "/" + strDate + "/";
	    	
    	//first create directory
		File theDir = new File(folderName);
		// if the directory does not exist, create it
		if (!theDir.exists()) {
		    mlog.say("creating directory: " + folderName);
		    boolean result = false;

		    try{
		        theDir.mkdir();
		        result = true;
		    } 
		    catch(SecurityException se){
		        //handle it
		    }        
		    if(result) {    
		        System.out.println("DIR created");  
		    }
		}
		
		//now create csv files
		try {			
			filew = new FileWriter(folderName+"/"+ name + ".csv");
			mlog.say("stream opened "+ name);
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	public FileWriter getFileWriter(){
		return filew;
	}

}
