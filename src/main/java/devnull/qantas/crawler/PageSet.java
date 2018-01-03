/**
 * Copyright ITCL PTY LTD 2018 - All rights reserved
 */
package devnull.qantas.crawler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * Step 4
 *
 * @author wozza
 */
public class PageSet {

    private URI location;
    private String title;
    private Set<PageSet> nodes = new HashSet<>();
    private Set<URI> refs = new HashSet<>();
    private String error = "";
    private Integer depth = 0;
    private boolean redirect = false;

    public PageSet(URI location) {
        this.location = location;
    }

    public PageSet(String location) throws URISyntaxException {
        this.location = new URI(location);
    }

    public PageSet(int depth, URI location) {
        this.depth = depth;
        this.location = location;
    }

    public URI getLocation() {
        return location;
    }

    public void setLocation(URI location) {
        this.location = location;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Set<PageSet> getNodes() {
        return nodes;
    }

    public Set<URI> getRefs() {
        return refs;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public boolean isRedirect() {
        return redirect;
    }

    public void setRedirect(boolean redirect) {
        this.redirect = redirect;
    }

    /**
     * Step 6
     *
     * @return
     */
    @Override
    public String toString() {
        try {
            return toJavascript(this);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String toJavascript(Object o) throws JsonProcessingException {
        return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(o);
    }

    /**
     * Step 16. allow relative linking
     *
     * @param uri
     */
    public void addRef(URI uri) throws URISyntaxException {
        this.getRefs().add(this.location.resolve(uri));
    }

    public int getDepth() {
        return depth;
    }

}
