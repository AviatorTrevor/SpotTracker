package SpotTweeter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import twitter4j.PagableResponseList;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.conf.ConfigurationBuilder;

public class SpotTwitterHandler {
  private final static String consumerKey = "buRXtaWXOBVuhrXmHTkFBhPHV";
  private final static String consumerSecret = "lr38CHKlTxTY7MXVR3te6L4lKDxNtjsADkITJgddDjdYcCpUhh";
  private final static String oAuthAccessToken = "2887944236-cHkSp0knIMv4EfWNdlsULDcK9gLfxsfObKtIkXx";
  private final static String oAuthAccessTokenSecret = "tp6hfuJY84xhevqqqNsBmWSWXypbPzyW6CWaeO7InYIwI";
  
  public static void tweet (String message) {
    ConfigurationBuilder cb = new ConfigurationBuilder();
    cb.setDebugEnabled(true)
        .setOAuthConsumerKey(consumerKey)
        .setOAuthConsumerSecret(consumerSecret)
        .setOAuthAccessToken(oAuthAccessToken)
        .setOAuthAccessTokenSecret(oAuthAccessTokenSecret);
    Twitter twitter = new TwitterFactory(cb.build()).getInstance();
    try {
      twitter.updateStatus(message); //ThrowsTwitterException
    } catch (TwitterException ex) {
      SpotTweeterManager.logException("Exception in tweeting", ex);
    }
  }

  public static void tweet (String imageFile, String message) {
    ConfigurationBuilder cb = new ConfigurationBuilder();
    cb.setDebugEnabled(true)
        .setOAuthConsumerKey(consumerKey)
        .setOAuthConsumerSecret(consumerSecret)
        .setOAuthAccessToken(oAuthAccessToken)
        .setOAuthAccessTokenSecret(oAuthAccessTokenSecret);
    Twitter twitter = new TwitterFactory(cb.build()).getInstance();
    try {
      StatusUpdate status = new StatusUpdate(message);
      status.setMedia(new File(imageFile));
      twitter.updateStatus(status); //ThrowsTwitterException
    } catch (TwitterException ex) {
      SpotTweeterManager.logException("Exception in tweeting", ex);
    }
  }

  public static void directMessageFollowers (String message) {
    ConfigurationBuilder cb = new ConfigurationBuilder();
    cb.setDebugEnabled(true)
        .setOAuthConsumerKey(consumerKey)
        .setOAuthConsumerSecret(consumerSecret)
        .setOAuthAccessToken(oAuthAccessToken)
        .setOAuthAccessTokenSecret(oAuthAccessTokenSecret);
    Twitter twitter = new TwitterFactory(cb.build()).getInstance();

    List<String> followers = getFollowers(twitter);
    for (int i = 0; i < followers.size(); i++) {
      try {
        twitter.sendDirectMessage(followers.get(i), message);
      } catch (TwitterException ex) {
        SpotTweeterManager.logException("Exception in tweeting a direct message", ex);
      }
    }
  }
  
  private static List<String> getFollowers(Twitter twitter) {
    List<String> followerScreenNames = new ArrayList<>();

    try {
      PagableResponseList<User> status = twitter.getFollowersList(twitter.getScreenName(), -1);
      for (User follower : status) {
        followerScreenNames.add(follower.getScreenName()); 
      }
    } catch (TwitterException ex) {
      SpotTweeterManager.logException("Exception in getting friend's list", ex);
    }
    return followerScreenNames;
  }
}