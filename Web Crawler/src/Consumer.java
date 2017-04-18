import com.panforge.robotstxt.RobotsTxt;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class Consumer implements Runnable {

    private LinkedBlockingQueue<String> frontier;
    private LinkedBlockingQueue<Document> collection;
    private ConcurrentHashMap<String, Object> visitedList;
    private ConcurrentHashMap<String, RobotsTxt> robotRules;
    private int PAGE_LIMIT;
    private boolean limitLinks;

    public Consumer(LinkedBlockingQueue<String> frontier,
                    LinkedBlockingQueue<Document> collection,
                    ConcurrentHashMap<String, RobotsTxt> robotRules,
                    ConcurrentHashMap<String, Object> visitedList,
                    int PAGE_LIMIT,
                    boolean limitLinks) {
        this.frontier = frontier;
        this.collection = collection;
        this.visitedList = visitedList;
        this.robotRules = robotRules;
        this.PAGE_LIMIT = PAGE_LIMIT;
        this.limitLinks = limitLinks;
    }

    private String getBaseURL(String stringURL) {
        try {
            URL url = new URL(stringURL);
            return url.getProtocol() + "://" + url.getHost();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private void getRobotRules(String stringURL) {
        String robotsRulesUrl = stringURL + "/robots.txt";
        try {
            InputStream robotsTxtStream = new URL(robotsRulesUrl).openStream();
            robotRules.put(stringURL, RobotsTxt.read(robotsTxtStream));
        } catch (Exception e) {
            //If the request fails assume the domain doesn't have a robots.txt
        }
    }

    public void run() {
        while(true) {
            try {
                Document doc = collection.take();
                Elements links = doc.select("a[href]");

                //For each link on the page we decide whether or not we should add it to the frontier.
                for (Element link : links) {
                    String newLink = link.attr("abs:href");
                    URL newLinkURL = new URL(newLink);
                    if(!visitedList.containsKey(newLink) && !newLink.startsWith("mailto:")) {
                        //Gets the base url then checks if we have rules associated with it
                        String baseURL = getBaseURL(newLink);
                        if (!robotRules.containsKey(baseURL)) {
                            getRobotRules(baseURL);
                        }
                        if(robotRules.get(baseURL) == null
                                || robotRules.get(baseURL).query(null, newLinkURL.getPath())
                                && !visitedList.contains(newLink)) {
                            if(limitLinks && !(baseURL.contains("cs.umass.edu") || baseURL.contains("cics.umass.edu"))) {
                                continue;
                            }
                            visitedList.put(newLink, new Object());
                            frontier.offer(newLink);
                            System.out.println(visitedList.size() + ". " + newLink);
                        }
                        if(visitedList.size() == PAGE_LIMIT) {
                            System.out.println("Page limit reached.");
                            System.out.println("Robots.txt found: " + robotRules.size());
                            return;
                        }
                    }
                }
            } catch (MalformedURLException e) {
                continue;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
