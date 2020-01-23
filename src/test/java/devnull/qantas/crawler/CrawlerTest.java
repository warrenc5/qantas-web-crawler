package devnull.qantas.crawler;

/**
 * Copyright ITCL PTY LTD 2020
 */


import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author wozza
 */
public class CrawlerTest {

    private Crawler crawler;

    public CrawlerTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        this.crawler = new Crawler();
    }

    @After
    public void tearDown() {
    }

    /**
     * Step 5
     *
     * I used recursive wget to download my website
     *
     * Step 6 create some assertion tests
     *
     */
    @Test
    public void testCrawl() throws Exception {
        URL url = this.getClass().getClassLoader().getResource("somewhere.com/index.html");
        System.out.println("crawling url :" + url.toExternalForm());
        assertNotNull(url);

        PageSet result = new Crawler().crawl(url.toURI());

        assertNotNull(result);
        assertNotNull(result.getError());
        System.out.println(result.toString());
        assertNotNull(result.getTitle());
        assertNotNull(result.getLocation());
        assertFalse(result.getRefs().isEmpty());
        assertFalse(result.getNodes().isEmpty());
        System.out.println(result.toString());
    }

    /**
     * TODO: move this to an IT run after install.
     * @throws Exception 
     */
    @Test
    @Ignore
    public void testLiveCrawl() throws Exception {
        //URI uri = new URI("https://www.qantasmoney.com");
        URI uri = new URI("https://www.qantasmoney.com/credit-cards/premier-platinum/fees-and-rates");
        assertNotNull(uri);

        PageSet result = crawler.crawl(uri);
        assertNotNull(result);
        assertNotNull(result.getError());
        System.out.println(result.toString());
        assertNotNull(result.getTitle());
        assertNotNull(result.getLocation());
        assertFalse(result.getRefs().isEmpty());
        assertFalse(result.getNodes().isEmpty());
    }

    /**
     * Step 8 write match line test
     *
     * @throws Exception
     */
    @Test
    public void testLine() throws Exception {
        PageSet result = new PageSet("www.anywhere.in"); //Step 9 overload constructor because I'm lazy.

        new Crawler().matchLine("<title>Hey, The Best Page in the Web</title>", result);
        assertNotNull(result.getTitle());
        assertEquals("hey, the best page in the web", result.getTitle().toLowerCase());

        new Crawler().matchLine("<a href=\"link.to.nowhere\"/>", result); //Step 20. get better at resolving relative paths.
        assertFalse(result.getRefs().isEmpty());
        assertEquals("link.to.nowhere", result.getRefs().iterator().next().toString());
    }

    /**
     * Step 21 introduce test for url resolving
     *
     * @throws Exception
     */
    @Test
    public void testURI() throws Exception {
        PageSet result = new PageSet("http://www.anywhere.in/"); //Step 9 overload constructor because I'm lazy.

        result.addRef(new URI("/here"));
        assertFalse(result.getRefs().isEmpty());
        assertEquals("http://www.anywhere.in/here", result.getRefs().iterator().next().toString());
    }

    @Test
    public void testAnchor() throws URISyntaxException {

        URI uri1 = new URI("https://www.qantasmoney.com/some");
        URI uri2 = new URI("https://www.qantasmoney.com/some#blip");
        URI uri3 = new URI("https://www.qantasmoney.com/some?blah");
        URI uri4 = new URI("https://www.qantasmoney.com/some2");

        assertTrue(new Crawler().isAnchor(uri1, uri2));
        assertTrue(new Crawler().isAnchor(uri1, uri3));
        assertFalse(new Crawler().isAnchor(uri1, uri4));
    }
}
