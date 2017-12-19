/**
 * Copyright ITCL PTY LTD 2018 - All rights reserved
 */
package devnull.qantas.crawler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.ejb.Local;
import javax.ejb.Singleton;

/**
 * Step 7 Implement the Local interface.
 *
 * @author wozza
 */
@Singleton
@Local(CrawlerLocal.class)
public class Crawler implements CrawlerLocal {

    private Logger logger = Logger.getLogger(Crawler.class.getName());
    //Step 18 configuration  and recursion detection
    private boolean allowOffsite = false;
    private Set<URI> visited = new HashSet<>();
    private int maxDepth = 10;
    private int currentDepth = 0;

    /**
     * Step 7 Write the actual logic
     *
     * @param location
     * @return
     */
    @Override
    public PageSet crawl(URI location) throws Exception {

        if (!location.isAbsolute()) {
            throw new Exception("only absolute urls please");
        }

        PageSet page = new PageSet(location);

        visited.add(location);

        logger.info("crawling url :" + location.toString());
        try {
            URLConnection connection = location.toURL().openConnection();
            connection.setDoInput(true);

            try (Reader inReader = new InputStreamReader(connection.getInputStream());
                    BufferedReader reader = new BufferedReader(inReader);) {
                String line = null;
                while ((line = reader.readLine()) != null) {
                    try {
                        matchLine(line, page); // Step 8. now write more tests
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
        } catch (MalformedURLException ex) {
            page.setError("Skipping " + ex.getMessage());
            Logger.getLogger(Crawler.class.getName()).log(Level.SEVERE, page.getError());
        } catch (IOException ex) {
            page.setError("By Jupiters outer moons! " + ex.getMessage());
            Logger.getLogger(Crawler.class.getName()).log(Level.SEVERE, page.getError());
        }

        logger.info(page.getRefs().toString());
        resolveRefs(page); //Step 15 resolve refs

        return page;
    }

    /**
     * Step 10. Friendly to tests
     *
     * Step 14. I spent about 20 minutes testing these regex and fixing tests.
     *
     * @param line
     * @param pages
     */
    void matchLine(String line, PageSet page) throws URISyntaxException {
        Pattern pattern = Pattern.compile(".*<title>(.*)</title>"); //TODO: initialize in constructor.
        Matcher matcher = pattern.matcher(line.toLowerCase());

        //Step 12. start running Junit here, R.G.R and fixing bugs in the regexp!
        if (matcher.matches()) {
            page.setTitle(matcher.group(1));
        }

        pattern = Pattern.compile(".*<a[^>]*href=\"([^\"]*.html).*"); //FIXME: this regexp is broken for so many reasons.
        matcher = pattern.matcher(line.toLowerCase());

        if (matcher.matches()) { // Step 13. at this point it became apparent to resolve all links after the page scan.
            page.addRef(new URI(matcher.group(1)));
        }
    }

    /**
     * Step 15. use a parallel stream and to crawl the refs
     *
     * @param page
     *
     */
    private void resolveRefs(PageSet page) {
        page.getNodes().addAll(page.getRefs().parallelStream().map(u -> {
            try {
                return resolveRef(page, u);
            } catch (Exception ex) {
                Logger.getLogger(Crawler.class.getName()).log(Level.SEVERE, ex.getMessage());
            }
            return null;
        }).collect(Collectors.toList()));
    }

    /**
     * Step 19. Refactor this out to control crawl behaviour
     *
     * @param page
     * @param u
     * @return
     */
    private PageSet resolveRef(PageSet page, URI u) throws Exception {
        if (!u.isAbsolute()) {
            throw new Exception("url not absolute " + u.toString());
        } else if (page.getLocation().equals(u)) {
            throw new Exception("same url " + u.toString());
        } else if (!allowOffsite && page.getLocation().getHost() != null && u.getHost() != null
                && !page.getLocation()
                        .getHost().equals(u.getHost())) {
            throw new Exception("offsite not allowed " + u.getHost());
        } else if (!allowOffsite && !page.getLocation().getScheme().equals(u.getScheme())) {
            throw new Exception("scheme not allowed " + u.getHost());
        } else if (visited.contains(u.toString())) {
            throw new Exception("already visited " + u.toString());
        } else {
            try {
                return crawl(u);
            } catch (Exception ex) {
                throw new Exception("crawl failed " + u.toString(), ex);
            }
        }
    }
}
