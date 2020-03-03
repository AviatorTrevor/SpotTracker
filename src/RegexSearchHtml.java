/*This class provides static utility functions for regular expression searches */

package SpotTweeter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexSearchHtml {
  public static String searchSingle(String sourceMaterial, String regex) {
    Pattern p = Pattern.compile(regex);
    Matcher m = p.matcher(sourceMaterial);
    int counter = 0;
    if (m.find()) {
      counter++;
      return m.group(1);
    }

    return "";
  }
  
  public static List<String> searchMultiple(String sourceMaterial, String regex) {
    Pattern p = Pattern.compile(regex);
    Matcher m = p.matcher(sourceMaterial);
    List<String> results = new ArrayList<>();
    while (m.find()) {
      results.add(m.group(1));
    }

    return results;
  }
}
