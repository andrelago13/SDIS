package backupservice.log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;

public class Logger {
	
	private static final String LOG_PATH = "resources/log";

	private int owner_id;
	private PrintWriter out_writer;
	
	public Logger(int owner_id) throws IOException {
		this.owner_id = owner_id;
		
		String path = getFilePath();
		
		File file = new File(path);
		file.getParentFile().mkdirs(); 
		file.createNewFile();
		
		out_writer = new PrintWriter(new BufferedWriter(new FileWriter(path,true)));
	}
	
	public String getFilePath() {
		return LOG_PATH + "/" + owner_id + ".txt";
	}
	
	public void log(String message) {
		appendLog(getLogStr(message));
	}
	
	public void logAndShow(String message) {
		String log = getLogStr(message);
		appendLog(log);
		showText(log);
	}
	
	public void show(String message) {
		showText(getLogStr(message));
	}
	
	private void showText(String text) {
		System.out.println(text);
	}
	
	public String getLogStr(String message) {
		return timestamp() + ":" + owner_id + "  =>  " + message;
	}
	
	private static String timestamp() {
		return "[" + getDate() + "]";
	}
	
	private static String getDate() {
		LocalDateTime time = LocalDateTime.now();
		return "" + time.getDayOfMonth() + "/" + time.getMonthValue() + "/" + time.getYear() + " " + time.getHour() + ":" + time.getMinute() + ":" + time.getSecond() + "." + time.getNano()/1000000;
	}
	
	public void appendLog(String log) {
		// TODO maybe switch to thread later
    	out_writer.println(log);
    	out_writer.flush();
    	
    	/*new Thread( new Runnable() {
		    @Override
		    public void run() {
		    	out_writer.println(log);
		    	out_writer.flush();
		    }
		}).start();*/
	}
	
	public void terminate() {
		out_writer.close();
	}
	
}
