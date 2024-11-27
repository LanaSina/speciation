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
	
	public FileBuilder(String folderName, String fileName){
		// create csv files
		try {			
			filew = new FileWriter(folderName+"/"+ fileName + ".csv");
			mlog.say("stream opened "+ fileName);
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	public FileWriter getFileWriter(){
		return filew;
	}

}
