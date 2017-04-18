import com.panforge.robotstxt.RobotsTxt;
import org.jsoup.nodes.Document;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class Crawler {
    public static void main(String[] args) {
        LinkedBlockingQueue<String> frontier = new LinkedBlockingQueue<>();
        LinkedBlockingQueue<Document> collection = new LinkedBlockingQueue<>();
        ConcurrentHashMap<String, Object> visitedList = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, RobotsTxt> robotRules = new ConcurrentHashMap<>();

        int PAGE_LIMIT = 1000;
        frontier.add("http://ciir.cs.umass.edu");
        boolean limitLinks = false;

        Producer linkDownloader = new Producer(frontier, collection, robotRules);
        Consumer documentParser = new Consumer(frontier, collection, robotRules, visitedList, PAGE_LIMIT, limitLinks);
        new Thread(linkDownloader).start();
        new Thread(documentParser).start();
    }
}