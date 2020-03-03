package SpotTweeter;

import java.util.List;

public class GoogleMaps {
  private final static String googleMapsApiKey = "AIzaSyCXb74ktW6KcPk_uyiZuxpWMmoJnm3KQyA";
  private final static String baseUrl = "https://maps.googleapis.com/maps/api/staticmap";
  private final static String zoomEnd = "15";
  private final static String zoomOffAirport = "13";
  private final static String imageSize = "640x400";
  private final static String mapType = "hybrid";
  private final static String fileName = SpotTweeterManager.fileDirectory + "spotTrackerMap.png";
  private final static WebClient client = WebClient.getInstance();

  public static String getStaticMapSpotTrackerImageUrl(List<SpotMessage> locations, boolean flightComplete, boolean offAirport) {
    String url;
    int lastIndex = locations.size() - 1;
    String lastCoordinates = locations.get(lastIndex).getLatitude() + "," + locations.get(lastIndex).getLongitude();
    String route = "";
    for (int i = 0; i < locations.size(); i++) {
      route += locations.get(i).getLatitude() + "," + locations.get(i).getLongitude();
      if (i != lastIndex) {
        route += "%7C";
      }
    }
    if (flightComplete) { //flight completed, so zoom in
      url = baseUrl
          + "?center="           + lastCoordinates
          + "&zoom="             + (offAirport ? zoomOffAirport : zoomEnd)
          + "&size="             + imageSize
          + "&maptype="          + mapType
          + "&path=color:red%7C" + route
          + "&markers="          + lastCoordinates
          + "&key="              + googleMapsApiKey;
    }
    else { //We are enroute
      url = baseUrl
          + "?size="             + imageSize
          + "&path=color:red%7C" + route
          + "&markers="          + lastCoordinates
          + "&key="              + googleMapsApiKey;
    }
    
    return url;
  }
  
  public static String getMapPinUrl(SpotMessage location) {
    String url = "https://www.google.com/maps/place/";
    url += Double.toString(Math.abs(location.latitude)) + (location.latitude < 0 ? "S" : "N");
    url += "+";
    url += Double.toString(Math.abs(location.longitude)) + (location.longitude < 0 ? "W" : "E");
    return url;
  }
  
  public static String downloadStaticMapSpotTrackerImage(List<SpotMessage> locations, boolean flightComplete, boolean offAirport) {
    String url = getStaticMapSpotTrackerImageUrl(locations, flightComplete, offAirport);
    
    try {
      client.getDataWriteToFile(url, fileName);
      SpotTweeterManager.log("Getting Google Map");
      return fileName;
    } catch (Exception ex) {
      SpotTweeterManager.logException("Unable to retrieve google map image", ex);
      return "";
    }
  }
}
