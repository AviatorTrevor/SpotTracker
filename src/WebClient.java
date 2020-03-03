/*This class generically acts as a web browser does - communication with a web server
 * to fetch web pages and send "get" or "post" data. */

package SpotTweeter;

import SpotTweeter.WebResponse.RequestType;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;

public class WebClient {

  private static WebClient wc = new WebClient();
  private CloseableHttpClient client;
  private CredentialsProvider cp = new BasicCredentialsProvider();
  private String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/36.0.1985.125 Safari/537.36";
  private String cookies;
  private final int timeout = 1000 * 5; //5 seconds

  private WebClient() { //Singleton class has private constructor
    client = HttpClientBuilder.create().setRedirectStrategy(new LaxRedirectStrategy())
            .setDefaultRequestConfig(RequestConfig.custom().setConnectTimeout(timeout).build())
            .setDefaultCredentialsProvider(cp).build();
  }

  public static WebClient getInstance() { //Singleton instance
    return wc;
  }

  public synchronized void setCredentials(String username, String password) {
    Credentials cred = new UsernamePasswordCredentials(username, password);
    cp.setCredentials(AuthScope.ANY, cred);
  }

  public synchronized WebResponse getPage(String url) throws Exception {
    // Set it all up
    HttpGet request = new HttpGet(url);

    request.setHeader("User-Agent", USER_AGENT);
    request.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
    request.setHeader("Accept-Language", "en-US,en;q=0.5");

    // Execute it
    WebResponse webResponse;
    try (CloseableHttpResponse response = client.execute(request)) {
      setCookies(response.getFirstHeader("Set-Cookie") == null ? "" : response.getFirstHeader("Set-Cookie").toString());
      String html = getHtml(response.getEntity().getContent());
      int statusCode = response.getStatusLine().getStatusCode();
      webResponse = new WebResponse(url, html, statusCode, RequestType.RequestTypeGet);
    }
    return webResponse;
  }
  
  public synchronized void getDataWriteToFile(String url, String filename) throws Exception {
    // Set it all up
    HttpGet request = new HttpGet(url);

    request.setHeader("User-Agent", USER_AGENT);
    request.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
    request.setHeader("Accept-Language", "en-US,en;q=0.5");
    
    // Execute it
    try (CloseableHttpResponse response = client.execute(request)) {
      setCookies(response.getFirstHeader("Set-Cookie") == null ? "" : response.getFirstHeader("Set-Cookie").toString());
      InputStream input = response.getEntity().getContent();
      FileOutputStream output = new FileOutputStream(filename);
      byte[] data = new byte[1024];
      int nRead;
      while ((nRead = input.read(data, 0, data.length)) != -1) {
        output.write(data, 0, nRead);
      }
      output.close();
    }
  }

  public synchronized WebResponse sendPost(String url, List<NameValuePair> postParams, List<NameValuePair> headers) throws Exception {
    // Set it all up
    HttpPost post = new HttpPost(url);

    for (NameValuePair header : headers) {
      post.setHeader(header.getName(), header.getValue());
    }
    post.setHeader("Cookie", getCookies());
    post.setHeader("User-Agent", USER_AGENT);

    if (postParams != null) {
      post.setEntity(new UrlEncodedFormEntity(postParams));
    }

    // Execute it
    WebResponse webResponse;
    try (CloseableHttpResponse response = client.execute(post)) {
      setCookies(response.getFirstHeader("Set-Cookie") == null ? "" : response.getFirstHeader("Set-Cookie").toString());
      String html = getHtml(response.getEntity().getContent());
      int statusCode = response.getStatusLine().getStatusCode();
      webResponse = new WebResponse(url, html, statusCode, RequestType.RequestTypePost);
    }
    return webResponse;
  }

  /* Take the InputStream that contains the response, and turns it into a String */
  private String getHtml(InputStream response) throws Exception {
    BufferedReader rd = new BufferedReader(new InputStreamReader(response));

    StringBuilder result = new StringBuilder();
    String line;
    while ((line = rd.readLine()) != null) {
      result.append(line);
    }

    return result.toString();
  }
  
  public synchronized WebResponse deleteMethod(String url, List<NameValuePair> headers) throws IOException {
    HttpDelete deleteMethod = new HttpDelete(url);
    for (NameValuePair header : headers) {
      deleteMethod.setHeader(header.getName(), header.getValue());
    }
    deleteMethod.setHeader("User-Agent", USER_AGENT);

    // Execute it
    WebResponse webResponse;
    try (CloseableHttpResponse response = client.execute(deleteMethod)) {
      String html = ""; // I don't care about the html.  Only care about the status code.
      int statusCode = response.getStatusLine().getStatusCode();
      webResponse = new WebResponse(url, html, statusCode, RequestType.RequestTypeDelete);
    }
    return webResponse;
  }

  public synchronized String getCookies() {
    return cookies;
  }

  public synchronized void setCookies(String cookies) {
    this.cookies = cookies;
  }
}
