/*This is the main application class.  It provides logging functions (for log files)
 * and it starts the threads neccessary for the application to begin*/

package SpotTweeter;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class SpotTweeterManager {

  private SpotLocator spot = new SpotLocator();
  public  final static String fileDirectory = "/home/pi/Desktop/";
  private final static String logFilePath = fileDirectory + "_SpotTweeter.log";
  private final static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  public static void main(String[] args) {
    try {
      Thread.sleep(1000 * 30); //Wait 30 seconds upon startup
    } catch (InterruptedException ex) {
      SpotTweeterManager.logException("Problem sleeping upon application start", ex);
    }
    SpotTweeterManager m = new SpotTweeterManager();
    m.attachShutDownHook();
    m.start();
  }

  public void attachShutDownHook(){
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        logApp("Application Shutdown");
      }
    });
  }

  public static synchronized void log(String message) {
    String logMessage = "INFO : " + getDate() + message;
    System.out.println(logMessage);
    try (PrintWriter logger = openLogFile()) {
      logger.println(logMessage);
    }
  }

  private synchronized void logApp(String message) {
    String logMessage = "APP  : " + getDate() + message;
    System.out.println(logMessage);
    try (PrintWriter logger = openLogFile()) {
      logger.println(logMessage);
    }
  }

  public static synchronized void logError(String message) {
    String logMessage = "ERROR: " + getDate() + message;
    System.out.println(logMessage);
    try (PrintWriter logger = openLogFile()) {
      logger.println(logMessage);
    }
  }

  public static synchronized void logException(String message, Throwable thrown) {
    String logMessage = "EXCEP: " + getDate() + message + "  " + thrown.getLocalizedMessage();
    System.out.println(logMessage);
    try (PrintWriter logger = openLogFile()) {
      logger.println(logMessage);
    }
  }

  private static synchronized String getDate() {
    Date date = Calendar.getInstance().getTime();
    String dateString = "[" + dateFormat.format(date) + "] ";
    return dateString;
  }

  public SpotTweeterManager() {
    logApp("Application Start");
  }

  private static PrintWriter openLogFile() {
    PrintWriter logger = null;
    try {
      return new PrintWriter(new FileWriter(logFilePath, true));
    } catch (IOException ex) {
      logException("openLogFile() failed:", ex);
      System.exit(1);
    }
    return logger;
  }

  public void start() {
    /* This tool is for tracking my dad's "Spot" GPS Locator and
     * publishing the data to a Twitter feed.
     */
    spot.start();
  }
}