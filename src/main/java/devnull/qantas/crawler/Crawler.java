/**
 * Copyright ITCL PTY LTD 2020 - All rights reserved
 */
package devnull.qantas.crawler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.time.LocalTime;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.ejb.Singleton;
import javax.transaction.Transactional;

/**
 * The crawliest web crawler.
 *
 * @author wozza
 */
@Singleton
@Local(CrawlerLocal.class)
public class Crawler implements CrawlerLocal {

    private Logger logger = Logger.getLogger(Crawler.class.getName());
    private boolean allowOffsite = false;
    private Set<URI> visited = new ConcurrentSkipListSet<>();

    private static final int DEFAULT_MAX_DEPTH = 4;
    private static final int DEFAULT_MAX_REDIRECTS = 3;
    private static final String LOCATION_HEADER = "LOCATION";
    private static final int DEFAULT_MAX_TIME = 10;

    private Pattern titlePattern;
    private Pattern hrefPattern;
    private Pattern anchorPattern;

    private int maxTimeSeconds = DEFAULT_MAX_TIME;
    private int maxDepth = DEFAULT_MAX_DEPTH;
    private int maxRedirects = DEFAULT_MAX_REDIRECTS;

    private LocalTime maxTime = LocalTime.now();

    /**
     * Make the constructor call initialize for the tests
     */
    public Crawler() {
        this.initialize();
    }

    /**
     * Start at the location given and recurse into the pages hyperlinks
     *
     * @param location
     * @return the set of pages with brief details
     */
    @Override
    @Transactional(Transactional.TxType.NOT_SUPPORTED)
    public PageSet crawl(URI location) throws Exception {

        if (!location.isAbsolute()) {
            throw new Exception("only absolute urls please");
        }

        PageSet page = new PageSet(location);

        maxTime = LocalTime.now().plusSeconds(maxTimeSeconds);
        return this.crawl(page, location);
    }

    public PageSet crawl(PageSet parent, URI location) throws Exception {

        PageSet page = new PageSet(parent.getDepth() + 1, location);
        visited.add(location);

        if (logger.isLoggable(Level.INFO)) {
            logger.info(String.format("crawling depth %1$s, url :%2$s", page.getDepth(), location.toString()));
        }

        try {
            int redirectCount = 0;
            InputStream in = this.openStreamForLocation(page, location, redirectCount);
            this.deconstructPage(page, in);
        } catch (MalformedURLException ex) {
            page.setError("Skipping " + ex.getMessage());
            Logger.getLogger(Crawler.class.getName()).log(Level.SEVERE, page.getError());
        } catch (IOException ex) {
            page.setError("By Jupiters outer moons! " + ex.getMessage());
            Logger.getLogger(Crawler.class.getName()).log(Level.SEVERE, page.getError());
        } catch (Exception ex) {
            page.setError(ex.getMessage());
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.fine(page.getRefs().toString());
        }

        if (page.getDepth() >= maxDepth || timeOutExceded()) {
            return page;
        }

        resolveRefs(page);

        return page;
    }

    /**
     *
     * @param page
     * @param in
     */
    public void deconstructPage(PageSet page, InputStream in) {

        try (Reader inReader = new InputStreamReader(in);
                BufferedReader reader = new BufferedReader(inReader);) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                try {
                    matchLine(line, page);
                } catch (URISyntaxException ex) {
                    Logger.getLogger(Crawler.class.getName()).log(Level.WARNING, "faulty link " + line + " " + ex.getMessage());
                }
            }
        } catch (MalformedURLException ex) {
            page.setError("Can't start " + ex.getMessage());
            Logger.getLogger(Crawler.class.getName()).log(Level.SEVERE, page.getError());
        } catch (IOException ex) {
            page.setError("By Jupiter! " + ex.getMessage());
            Logger.getLogger(Crawler.class.getName()).log(Level.SEVERE, page.getError());
        }
    }

    /**
     * Check each line to see if we can extract a title or a link
     *
     * @param line
     * @param pages
     */
    void matchLine(String line, PageSet page) throws URISyntaxException {
        Matcher matcher = titlePattern.matcher(line);

        if (matcher.matches()) {
            page.setTitle(matcher.group(1));
        }

        matcher = hrefPattern.matcher(line);

        if (matcher.matches()) {
            page.addRef(new URI(matcher.group(1).trim()));
        }
    }

    /**
     * Follow each link in the page in parallel
     *
     * @param page
     *
     */
    private void resolveRefs(PageSet page) {
        page.getNodes().addAll(page.getRefs().parallelStream().map(u -> {
            try {
                return resolveRef(page, u);
            } catch (Exception ex) {
                page.setError(ex.getMessage());
                Logger.getLogger(Crawler.class.getName()).log(Level.SEVERE, ex.getMessage());
            }
            return null;
        }).filter(p -> p != null).collect(Collectors.toList()));
    }

    /**
     * Logic to resolve a single ref.
     *
     * @param page
     * @param uri
     * @return
     */
    private PageSet resolveRef(PageSet page, URI uri) throws Exception {
        if (!uri.isAbsolute()) {
            throw new Exception("url not absolute " + uri.toString());
        }
        if (page.getLocation().equals(uri) || isAnchor(page.getLocation(), uri)) {
            throw new Exception("self url " + uri.toString());
        }
        if (!allowOffsite && page.getLocation().getHost() != null && uri.getHost() != null
                && !page.getLocation()
                        .getHost().equals(uri.getHost())) {
            throw new Exception("offsite not allowed " + uri.getHost());
        }
        if (!allowOffsite && !page.getLocation().getScheme().equals(uri.getScheme())) {
            throw new Exception("scheme not allowed " + uri.getHost());
        }
        if (visited.contains(uri)) {
            throw new Exception("already visited " + uri.toString());
        }

        if (timeOutExceded()) {
            throw new Exception("timeout");
        }

        try {
            return crawl(page, uri);
        } catch (Exception ex) {
            throw new Exception("crawl failed " + uri.toString(), ex);
        }
    }

    /**
     * This method will follow redirects limited by makeRedirects
     *
     * @param location
     * @return
     * @throws IOException
     * @throws URISyntaxException
     * @throws Exception
     */
    private InputStream openStreamForLocation(PageSet page, URI location, int depth) throws IOException, URISyntaxException, Exception {
        URLConnection urlConnection = location.toURL().openConnection();

        if (!(urlConnection instanceof HttpURLConnection)) {//FOR TESTING
            return urlConnection.getInputStream();
        }

        HttpURLConnection connection = (HttpURLConnection) urlConnection;
        switch (connection.getResponseCode()) {
            case HttpURLConnection.HTTP_OK:
                String contentType = connection.getHeaderField("Content-Type");
                if (contentType != null && contentType.contains("html")) {
                    return connection.getInputStream();
                } else {
                    throw new Exception(String.format("%1$s %2$s", contentType, location.toString()));
                }
            case HttpURLConnection.HTTP_MOVED_TEMP:
            case HttpURLConnection.HTTP_MOVED_PERM:
            case HttpURLConnection.HTTP_SEE_OTHER:
                if (depth >= maxRedirects) {
                    throw new Exception(String.format("%1$s exceeds MAX_REDIRECTS", location.toString()));
                }
                URI uri = new URI(connection.getHeaderField(LOCATION_HEADER));
                page.setLocation(uri);
                page.setRedirect(true);
                return openStreamForLocation(page, uri, ++depth);
            default:
                throw new Exception(String.format("%1$s %2$s", connection.getResponseCode(), location.toString()));
        }

    }

    @PostConstruct
    public void initialize() {
        titlePattern = Pattern.compile(".*<title>([^<]*).*", Pattern.CASE_INSENSITIVE);
        hrefPattern = Pattern.compile(".*<a[^>]*href=\"([^\"]*).*", Pattern.CASE_INSENSITIVE); //FIXME: this regexp is broken for so many reasons.
        anchorPattern = Pattern.compile("^[#\\?]+.*");
    }

    boolean isAnchor(URI location, URI uri) {
        URI relative = location.relativize(uri);
        return anchorPattern.matcher(relative.toString()).matches();
    }

    private boolean timeOutExceded() {
        return LocalTime.now().isAfter(maxTime);
    }

    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    public void setMaxRedirects(int maxRedirects) {
        this.maxRedirects = maxRedirects;
    }

    public void setMaxTime(int maxTime) {
        this.maxTimeSeconds = maxTime;
    }

    @Override
    public void setAllowOffsite(boolean allow) {
        this.allowOffsite = allow;
    }

    @Override
    public String hello() {
        return "Warren Crossing Web Crawler (slater)";
    }

    @Override
    public void setSettings(Integer maxTime, Integer maxDepth, Integer maxRedirects, Boolean allow) {
        if (maxDepth != null) {
            this.setMaxDepth(maxDepth);
        }
        if (maxTime != null) {
            this.setMaxTime(maxTime);
        }
        if (maxRedirects != null) {
            this.setMaxRedirects(maxRedirects);
        }
        if (allow != null) {
            this.setAllowOffsite(allow);
        }
    }

}
