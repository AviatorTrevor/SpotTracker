package SpotTweeter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Date;

public class SpotMessage implements Serializable {
  private static String dmDataFilePath = SpotTweeterManager.fileDirectory + "_direct.data";
  private static String tweetDataFilePath = SpotTweeterManager.fileDirectory + "_tweet.data";

  public int id;
  public Date timestamp;
  public double latitude;
  public double longitude;
  public String message;
  public int direction; //[0, 359], units: degrees true
  public int speed; // > 0, units: knots

  public SpotMessage(int id, Date timestamp, double latitude, double longitude, String message, int direction, int speed) {
    this.id = id;
    this.timestamp = timestamp;
    this.latitude = latitude;
    this.longitude = longitude;
    this.message = message;
    this.direction = direction;
    this.speed = speed;
  }

  public SpotMessage() { //only used to initialize lastTwitterDirectMessage
    this.id = 0;
    this.timestamp = new Date(0); //when we first boot up, we don't care about messages that existed before us booting up, or we can read a file that has the date of the last message
    this.latitude = 0;
    this.longitude = 0;
    this.message = "";
    this.direction = 0;
    this.speed = 0;
  } //default constructor
  
  public double getLatitude()  { return this.latitude;  }
  public double getLongitude() { return this.longitude; }
  
  public String getMinutesSinceMessage() {
    Date currentTime = new Date();
    long milliseconds = currentTime.getTime() - this.timestamp.getTime();
    double minutes = (milliseconds / 60000.0);
    if (minutes >= 1440) {
      minutes /= -1440; //changing from minutes to days
      return String.format("%1$,.1f", minutes) + "days"; //changing from minutes to days
    }
    else if (minutes >= 60) {
      minutes /= -60; //changing from minutes to hours
      return String.format("%1$,.1f", minutes) + "hr"; //changing from minutes to hours
    }
    else {
      minutes *= -1;
      return String.format("%1$,.1f", minutes) + "min"; //return minutes
    }
  }

  //read from file
  private static SpotMessage readSpotMessage(String filename) {
    try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
      int id = (int) ois.readObject();
      Date timestamp = (Date) ois.readObject();
      double latitude = (double) ois.readObject();
      double longitude = (double) ois.readObject();
      String message = (String) ois.readObject();
      int direction = (int) ois.readObject();
      int speed = (int) ois.readObject();
      ois.close();
      return new SpotMessage(id, timestamp, latitude, longitude, message, direction, speed);
    }
    catch (FileNotFoundException ex) {
      return new SpotMessage();
    }
    catch (Exception ex) {
      SpotTweeterManager.logException("Error reading SpotMessage from file " + filename, ex);
    }
    return new SpotMessage(); //this will only be called if there is an error reading the file
  }

  public static SpotMessage readLastDmSpotMessage() {
    return readSpotMessage(dmDataFilePath);
  }

  public static SpotMessage readLastTweetedSpotMessage() {
    return readSpotMessage(tweetDataFilePath);
  }

  //write to file
  private static void writeSpotMessage(SpotMessage sm, String filename) {
    try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
      oos.writeObject(sm.id);
      oos.writeObject(sm.timestamp);
      oos.writeObject(sm.latitude);
      oos.writeObject(sm.longitude);
      oos.writeObject(sm.message);
      oos.writeObject(sm.direction);
      oos.writeObject(sm.speed);
      oos.close();
    }
    catch(Exception ex) {
      SpotTweeterManager.logException("Error writing SpotMessage to file " + filename, ex);
    }
  }
  
  public static void writeLastDmSpotMessage(SpotMessage sm) {
    writeSpotMessage(sm, dmDataFilePath);
  }

  public static void writeLastTweetedSpotMessage(SpotMessage sm) {
    writeSpotMessage(sm, tweetDataFilePath);
  }

}