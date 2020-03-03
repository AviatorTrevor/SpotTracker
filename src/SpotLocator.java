package SpotTweeter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class SpotLocator implements Runnable {
  
  private WebClient client = WebClient.getInstance();
  private final int nightTimePollingInterval = 1000 * 60 * 60 * 2; //2 hours
  private final int longPollingInterval = 1000 * 60 * 15; //15 minutes
  private final int shortPollingInterval = 1000 * 60 * 2 + 1000 * 30; //2 minutes, 30 seconds
  private final int twentyThreeMinutes = 1000 * 60 * 23;
  private final int flightSpeed = 30;
  private final int maxTweetChar = 116;
  private int pollingInterval = longPollingInterval;
  private boolean apiError = false;
  //dad's old spot tracker:
  //private final String spotUrl = "https://api.findmespot.com/spot-main-web/consumer/rest-api/2.0/public/feed/0qQHyNZzr4BHIIZgmxaCMaJv1G1IltM1A/message.xml";
  private final String spotUrl = "https://api.findmespot.com/spot-main-web/consumer/rest-api/2.0/public/feed/0CRScEJOMtrUaQzR2lqckTfLEI0qNC5r5/message.xml";  
  private List<SpotMessage> spotMessages = new ArrayList<>();
  private SpotMessage lastDirectMessage; //direct messages are used for end of flights
  private SpotMessage lastTweet = new SpotMessage(); //tweets are used for all new data

  public SpotLocator() {
    //upon start up, read file
    lastDirectMessage = SpotMessage.readLastDmSpotMessage();
    lastTweet = SpotMessage.readLastTweetedSpotMessage();
  }
  
  /* Given two sets of coordinates, determines direction in degrees true*/
  private static int calculateDirection(double latA, double lonA, double latB, double lonB) {
    double theta1 = Math.toRadians(latA);
    double theta2 = Math.toRadians(latB);
    double deltaLambda = Math.toRadians(lonB - lonA);
    double y = Math.sin(deltaLambda) * Math.cos(theta2);
    double x = Math.cos(theta1) * Math.sin(theta2) - Math.sin(theta1) * Math.cos(theta2) * Math.cos(deltaLambda);
    int direction = (int) Math.round(Math.toDegrees(Math.atan2(y, x)));
    if (direction < 0) {
      direction += 360;
    }
    return direction;
  }

  //returns speed in knots given 2 lat/longs and 2 times
  private static int calculateSpeed(double latA, double lonA, double latB, double lonB, Date timeA, Date timeB) {
    double distance = calculateDistance(latA, lonA, latB, lonB);
    int speed = (int) Math.round(distance / (Math.abs(timeB.getTime() - timeA.getTime()) / (1000.0 * 60.0 * 60.0))); //return nautical miles per hour (knots)
    return speed;
  }
  
  //returns distance between 2 points in units of nautical miles
  private static double calculateDistance(double latA, double lonA, double latB, double lonB) {
    double R = 3440.06;
    double theta1 = Math.toRadians(latA);
    double theta2 = Math.toRadians(latB);
    double deltaTheta = Math.toRadians(latB - latA);
    double deltaLambda = Math.toRadians(lonB - lonA);
    double a = Math.sin(deltaTheta/2) * Math.sin(deltaTheta/2) + Math.cos(theta1) * Math.cos(theta2) * Math.sin(deltaLambda/2) * Math.sin(deltaLambda/2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    return R * c;
  }

  private String retrieveNearestAirport(double lat, double lon) {
    int randomNumber = (int) (Math.random() * 99999);
    String rightClickApiUrl = "http://skyvector.com/api/rightClick?ll=" + lat + "%2C" + lon+"&q=&rand=" + String.format("%05d", randomNumber);
    WebResponse response;
    try {
      response = client.getPage(rightClickApiUrl);
      SpotTweeterManager.log("Getting Skyvector Nearest Airport");
    } catch (Exception ex) {
      SpotTweeterManager.logException("Unable to retrieve skyvector.com data", ex);
      return "";
    }

    String trimmedResult = RegexSearchHtml.searchSingle(response.getHtml(), "near(.*)");
    String type = RegexSearchHtml.searchSingle(trimmedResult, "\"t\":\"([^\"]+)");
    if (type.equals("APT")) {
      double aptLatitude = Double.parseDouble(RegexSearchHtml.searchSingle(trimmedResult, "\"lon\":\"([^\"]+)"));
      double aptLongitude = Double.parseDouble(RegexSearchHtml.searchSingle(trimmedResult, "\"lat\":\"([^\"]+)"));
      double distance = calculateDistance(aptLatitude, aptLongitude, lat, lon);

      //Only count it as an airport if the distance is fairly close (like 1.5nm)
      if (distance < 3.0) {
        String icaoId = RegexSearchHtml.searchSingle(trimmedResult, "\"id\":\"([^\"]+)");
        String airportPageUrl = "http://skyvector.com" + RegexSearchHtml.searchSingle(trimmedResult, "\"u\":\"([^\"]+)");

        //Get Airport Name in plain English
        try {
          response = client.getPage(airportPageUrl);
          SpotTweeterManager.log("Getting Skyvector Airport Full Name");
        } catch (Exception ex) { } //do nothing with the exception
        return "(" + icaoId + ") " + RegexSearchHtml.searchSingle(response.getHtml(), "class=\"titlebgrighta\">([^<]+)");
      }
    }
    return ""; //No airport here
  }

  private static String getFlightPathUrl(List<SpotMessage> flightPath) {
    if (flightPath.isEmpty()) {
      return "";
    }
    String skyvectorPath = "skyvector.com/?ll=" + flightPath.get(flightPath.size() - 1).latitude + "," + flightPath.get(flightPath.size() - 1).longitude + "&chart=301&zoom=2&fpl=";
    String skyvectorFlightPlan = "";
    for (int i = 0; i < flightPath.size(); i++) {
      skyvectorFlightPlan += "%20" + getLatLonDDMMSS(flightPath.get(i).latitude, flightPath.get(i).longitude);
    }
    skyvectorPath += skyvectorFlightPlan.substring(3, skyvectorFlightPlan.length());
            
    return skyvectorPath;
  }
  
  private static String getLatLonDDMMSS(double lat, double lon) {
    String northSouth = lat >= 0 ? "N" : "S";
    String eastWest   = lon >= 0 ? "E" : "W";
    
    lat = Math.abs(lat);
    lon = Math.abs(lon);
    
    int degreesLat = (int) lat;
    lat -= degreesLat;
    int minutesLat = (int) (lat * 60.0);
    lat -= (double) (minutesLat / 60.0);
    int secondsLat = (int) (lat * 3600.0);
    String latString = String.format("%02d", degreesLat) + String.format("%02d", minutesLat) + String.format("%02d", secondsLat);
    
    int degreesLon = (int) lon;
    lon -= degreesLon;
    int minutesLon = (int) (lon * 60.0);
    lon -= (double) (minutesLon / 60.0);
    int secondsLon = (int) (lon * 3600.0);
    String lonString = String.format("%03d", degreesLon) + String.format("%02d", minutesLon) + String.format("%02d", secondsLon);
    
    return latString + northSouth + lonString + eastWest;
  }
  
  private static String getDirectionString(int dir)
  {
    List<String> directions = new ArrayList<>();
    directions.add("North");
    directions.add("NorthEast");
    directions.add("East");
    directions.add("SouthEast");
    directions.add("South");
    directions.add("SouthWest");
    directions.add("West");
    directions.add("NorthWest");
    directions.add("North");
    
    int returnIndex = 0;
    while (dir > 22)
    {
      returnIndex++;
      dir -= 45;
    }
    return directions.get(returnIndex);
  }

  public void start() {
    new Thread(this).start();
  }

  @Override
  public void run() {
    Thread.currentThread().setName(Class.class.getName());
    while (true) {
      if (processSpotMessages()) {
        processFlight();
      }
      try {
        //Determine polling time day or night, and 
        if (pollingInterval == longPollingInterval) {
          Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
          int hour = calendar.get(Calendar.HOUR_OF_DAY); //24 hour format
          if (hour >= 6 && hour <= 10) {
            pollingInterval = nightTimePollingInterval;
          }
        }

        Thread.sleep(pollingInterval);
      } catch (InterruptedException ex) {
        SpotTweeterManager.logException("Main polling thread", ex);
      }
    }
  }

  public boolean processSpotMessages() {
    //Retrieve XML data
    spotMessages.clear();
    WebResponse response;
    try {
      response = client.getPage(spotUrl);
      //SpotTweeterManager.log("Getting Spot Messages");
    } catch (Exception ex) {
      SpotTweeterManager.logException("Could not retrieve spot messages", ex);
      return false;
    } //do nothing with the exception
    
    //See if the API is reporting an error
    String error = RegexSearchHtml.searchSingle(response.getHtml(), "<error>(.+?)</error>");
    apiError = !error.isEmpty();
    if (apiError)
    {
      pollingInterval = longPollingInterval; //in case it wasn't already in a long-wait poll
      String errorText = RegexSearchHtml.searchSingle(response.getHtml(), "<text>(.+?)</text>");
      if (!errorText.equals("No Messages to display")) {
        SpotTweeterManager.logError("Spot API is reporting an error: " + errorText);
      }
      return false;
    }

    //parse it down to single messages
    List<String> messages = RegexSearchHtml.searchMultiple(response.getHtml(), "<message clientUnixTime=\"0\">(.+?)</message>");
    if (messages.isEmpty()) {
      return false;
    }

    //parse out the messages into the SpotLocator structure
    for (int i = messages.size() - 1; i >= 0; i--) {
      int messageId = Integer.parseInt(RegexSearchHtml.searchSingle(messages.get(i), "<id>([^<]+)"));
      Date messageTimestamp = new Date((long)Integer.parseInt(RegexSearchHtml.searchSingle(messages.get(i), "<unixTime>([^<]+)")) * 1000L);
      double messageLatitude = Double.parseDouble(RegexSearchHtml.searchSingle(messages.get(i), "<latitude>([^<]+)"));
      double messageLongitude = Double.parseDouble(RegexSearchHtml.searchSingle(messages.get(i), "<longitude>([^<]+)"));
      String messageContent = RegexSearchHtml.searchSingle(messages.get(i), "<messageContent>([^<]+)");
      int direction = 0;
      int speed = 0;

      //not the first message, therefore we can figure out speed/distance
      if (!spotMessages.isEmpty()) {
        direction = calculateDirection(spotMessages.get(spotMessages.size() - 1).latitude, spotMessages.get(spotMessages.size() - 1).longitude, messageLatitude, messageLongitude);
        speed = calculateSpeed(spotMessages.get(spotMessages.size() - 1).latitude, spotMessages.get(spotMessages.size() - 1).longitude, messageLatitude, messageLongitude, spotMessages.get(spotMessages.size() - 1).timestamp, messageTimestamp);
      }
      spotMessages.add(new SpotMessage(messageId, messageTimestamp, messageLatitude, messageLongitude, messageContent, direction, speed));
    }
    return true;
  }

  public void processFlight() {
    Date currentTime = new Date();
    List<SpotMessage> flight = new ArrayList<>();
    int indexAfterPreviousFlight = 0;
    for (int i = 0; i < spotMessages.size(); i++)
    {
      //looking for the beginning of the flight
      if (flight.isEmpty())
      {
        if (spotMessages.get(i).speed >= flightSpeed)
        {
          int j = indexAfterPreviousFlight;
          if (indexAfterPreviousFlight < i - 3)
              j = i - 3;
          for (; j <= i; j++) {//if there are previous messages, add them
            flight.add(spotMessages.get(j));
          }
          if (flight.size() >= 2) {
            tweetBreadcrumb(flight);
          }
        }
      }
      else //looking for the end of the flight
      {
        flight.add(spotMessages.get(i));
        
        // if below flight speed, or time between this message and the next is large, or time since last message in the list is large...
        if ((spotMessages.get(i).speed < flightSpeed)
             || ((i + 1 < spotMessages.size()) && (spotMessages.get(i + 1).timestamp.getTime() - spotMessages.get(i).timestamp.getTime() >= twentyThreeMinutes))
             || ((i == spotMessages.size() - 1) && (currentTime.getTime() - spotMessages.get(i).timestamp.getTime() >= twentyThreeMinutes * 2)))
        {
          //if time between messages is large, log a message about it
          if ((i + 1 < spotMessages.size()) && (spotMessages.get(i + 1).timestamp.getTime() - spotMessages.get(i).timestamp.getTime() >= twentyThreeMinutes)) {
            long timeBetween = (spotMessages.get(i + 1).timestamp.getTime() - spotMessages.get(i).timestamp.getTime()) / 60000;
            SpotTweeterManager.log("Ending flight because time between XML entries was " + timeBetween);
          }
          //if time since the last message was posted is large, log a message about it
          if ((i == spotMessages.size() - 1) && (currentTime.getTime() - spotMessages.get(i).timestamp.getTime() >= twentyThreeMinutes * 2)) {
            long timeBetween = (currentTime.getTime() - spotMessages.get(i).timestamp.getTime()) / 60000;
            SpotTweeterManager.log("Ending flight because time since last data was " + timeBetween);
          }
          indexAfterPreviousFlight = i + 1;
          directMessageEndOfFlight(flight);
          pollingInterval = longPollingInterval;
          flight.clear();
        }
        else {
          tweetBreadcrumb(flight);
        }
      }
    }

    //Check how recent the last message was and adjust polling interval accordingly
    if (currentTime.getTime() - spotMessages.get(spotMessages.size() - 1).timestamp.getTime() < twentyThreeMinutes) {
      pollingInterval = shortPollingInterval;
    }
    else {
      pollingInterval = longPollingInterval;
    }
  }
  
  private void tweetBreadcrumb(List<SpotMessage> flight)
  {
    int lastIndex = flight.size() - 1;
    if (flight.get(lastIndex).timestamp.getTime() > lastTweet.timestamp.getTime())
    {
      String direction = getDirectionString(flight.get(lastIndex).direction);
      String message;
      String departureAirport = retrieveNearestAirport(flight.get(0).latitude, flight.get(0).longitude);
      if (!departureAirport.isEmpty()) {
        message = "Enroute from " + departureAirport + ", flying " + direction + ".";
      }
      else {
        message = "Enroute, flying " + direction + ".";
      }
      message += "\n" + flight.get(lastIndex).getMinutesSinceMessage();
      if (message.length() > maxTweetChar) {
        message = message.substring(0, maxTweetChar);
      }
      message += "\n" + getFlightPathUrl(flight);
      SpotTwitterHandler.tweet(GoogleMaps.downloadStaticMapSpotTrackerImage(flight, false, false), message);
      lastTweet = flight.get(lastIndex);
      SpotMessage.writeLastTweetedSpotMessage(lastTweet);
      SpotTweeterManager.log("Tweeting breadcrumb");
    }
  }
  
  private void directMessageEndOfFlight(List<SpotMessage> flight)
  {
    int lastIndex = flight.size() - 1;
    if (flight.get(lastIndex).timestamp.getTime() > lastDirectMessage.timestamp.getTime())
    {
      //Getting departure and arrival locations and building the message
      String departureAirport = retrieveNearestAirport(flight.get(0).latitude, flight.get(0).longitude);
      String arrivalAirport = retrieveNearestAirport(flight.get(lastIndex).latitude, flight.get(lastIndex).longitude);
      String message, directMessage, flightPathUrl;
      flightPathUrl = getFlightPathUrl(flight);
      boolean offAirport = false;
      if (arrivalAirport.isEmpty()) {
        offAirport = true;
        if (departureAirport.isEmpty()) {
          message = "Flight terminated off-airport!";
        }
        else {
          message = "Flight from " + departureAirport + " terminated off-airport!";
        }
      }
      else if (departureAirport.isEmpty()) {
        message = "Flight has landed at " + arrivalAirport + ".";
      }
      else {
        
        message = "Flight from " + departureAirport + " has landed at " + arrivalAirport + ".";
      }
      message += "\n" + flight.get(lastIndex).getMinutesSinceMessage();
      
      //Build Direct Message
      String googleMapUrl = GoogleMaps.getMapPinUrl(flight.get(lastIndex));
      directMessage = message + "\n" + flightPathUrl + "\n\n" + googleMapUrl;

      //Direct Message
      SpotTwitterHandler.directMessageFollowers(directMessage);
      lastDirectMessage = flight.get(lastIndex);
      SpotMessage.writeLastDmSpotMessage(lastDirectMessage);
      
      //Tweet
      if (message.length() > maxTweetChar) {
        SpotTweeterManager.log("Tweet was shortened because it was too long: " + message);
        message = message.substring(0, maxTweetChar);
      }
      message += "\n" + flightPathUrl;

      SpotTwitterHandler.tweet(GoogleMaps.downloadStaticMapSpotTrackerImage(flight, true, offAirport), message);
      lastTweet = flight.get(lastIndex);
      SpotMessage.writeLastTweetedSpotMessage(lastTweet);
      
      SpotTweeterManager.log("Tweeting end of flight");
    }
  }
}
