/**
 * Copyright ITCL PTY LTD 2020 - All rights reserved
 */
package devnull.qantas.crawler;

import java.net.URI;
import javax.ejb.Local;
import javax.validation.constraints.NotNull;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * Step 1.
 *
 * @author wozza
 */
@Local
@Path("crawl")
public interface CrawlerLocal {

    /**
     * crawl the location and extract hrefs
     *
     * Step 2.
     *
     * @param location
     * @return
     */
    @POST
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    PageSet crawl(@NotNull @FormParam("url") URI location) throws Exception;

    @GET
    @Path("/")
    String hello();

    @GET
    @Path("/settings")
    void setSettings(
            @QueryParam(value = "max-time-seconds") Integer maxTime,
            @QueryParam(value = "max-depth") Integer maxDepth,
            @QueryParam(value = "max-redirects") Integer maxRedirects,
            @QueryParam(value = "allow") Boolean allow);

    void setMaxTime(int maxTime);

    void setMaxRedirects(int max);

    void setMaxDepth(int max);

    void setAllowOffsite(boolean allow);

}