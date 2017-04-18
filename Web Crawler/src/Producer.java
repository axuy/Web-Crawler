import com.panforge.robotstxt.RobotsTxt;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Producer implements Runnable {

    private LinkedBlockingQueue<String> frontier;
    private LinkedBlockingQueue<Document> collection;
    private ConcurrentHashMap<String, RobotsTxt> robotRules;
    private HashMap<String, Long> timeoutTable;

    public Producer(LinkedBlockingQueue<String> frontier,
                    LinkedBlockingQueue<Document> collection,
                    ConcurrentHashMap<String, RobotsTxt> robotRules) {
        this.frontier = frontier;
        this.collection = collection;
        this.robotRules = robotRules;
        timeoutTable = new HashMap<>();
    }

    private String getBaseURL(String stringURL) {
        try {
            URL url = new URL(stringURL);
            return url.getProtocol() + "://" + url.getHost();
        } catch (Exception e) {
            System.out.println("Invalid URL: " + stringURL);
            e.printStackTrace();
            return "";
        }
    }

    public void run() {
        while(true) {
            try {
                String currentStringURL = frontier.poll(20, TimeUnit.SECONDS);
                if(currentStringURL == null) {
                    break;
                }
                //If the next link we will download is on the same domain, respect the crawl delay for that domain
                String currentBaseURL = getBaseURL(currentStringURL);
                Long timer = timeoutTable.get(currentBaseURL);

                //If we have a timer set for the current domain
                if(timer != null && robotRules.contains(currentBaseURL)) {
                    int crawlDelay = robotRules.get(currentBaseURL).getCrawlDelay();
                    //If we've waited long enough for the domain
                    if(!(System.currentTimeMillis() - timeoutTable.get(currentBaseURL) > crawlDelay * 1000)) {
                        if(frontier.isEmpty()) {
                            continue;
                        }
                        frontier.offer(frontier.poll()); //Shuffle current head to end of queue
                        continue;
                    }
                }

                Document doc = Jsoup.connect(currentStringURL).get();
                collection.offer(doc);
                timeoutTable.put(currentBaseURL, System.currentTimeMillis());
            } catch (HttpStatusException e) {
                continue;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
