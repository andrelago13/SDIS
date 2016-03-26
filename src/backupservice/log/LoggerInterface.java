package backupservice.log;

public interface LoggerInterface {

	public void log(String message);
	public void logAndShow(String message);
	public void show(String message);

	public void logError(String message);
	public void logAndShowError(String message);
	public void showError(String message);
}
