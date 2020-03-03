package SpotTweeter;

public class WebResponse {
  private String url;
  private String html;
  private int responseCode;
  private RequestType requestType;

  public enum RequestType { RequestTypeGet, RequestTypePost, RequestTypeDelete };
  
  WebResponse() {
    url = "";
    html = "";
    responseCode = -1;
  }
  
  public WebResponse(String url, String html, int responseCode, RequestType requestType) {
    this.url = url;
    this.html = html;
    this.responseCode = responseCode;
    this.requestType = requestType;
  }
  
  public String getHtml() {
    return html;
  }
  
  public boolean isGood() {
    printErrors();
    return responseCode == 200;
  }
  
  private void printErrors() {
    if (responseCode != 200) {
      if (requestType == RequestType.RequestTypeGet) {
        printGetError();
      }
      else {
        printPostError();
      }
    }
  }
  
  private void printGetError() {
    SpotTweeterManager.logError("GET URL '" + url + "' returned code " + responseCode);
  }
  
  private void printPostError() {
    SpotTweeterManager.logError("POST URL '" + url + "' returned code " + responseCode);
  }
}
