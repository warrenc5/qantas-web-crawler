/**
 * Copyright ITCL PTY LTD 2020 - All rights reserved
 */
package devnull.qantas.crawler;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;

@ApplicationPath("rest")
@Stateless
public class QantasApplication extends Application {

    @Inject
    CrawlerLocal crawler;

    @Context
    protected ServletContext servletContext;

    public QantasApplication() {
    }

    @Override
    public Set<Object> getSingletons() {
        String s = null;

        if ((s = servletContext.getInitParameter("MAX_TIME_SECONDS")) != null) {
            crawler.setMaxTime(Integer.parseInt(s));
        }

        if ((s = servletContext.getInitParameter("MAX_REDIRECTS")) != null) {
            crawler.setMaxRedirects(Integer.parseInt(s));
        }

        if ((s = servletContext.getInitParameter("MAX_DEPTH")) != null) {
            crawler.setMaxDepth(Integer.parseInt(s));
        }

        if ((s = servletContext.getInitParameter("ALLOW_OFFSITE")) != null) {
            crawler.setAllowOffsite(Boolean.parseBoolean(s));
        }

        Set resources = new java.util.HashSet(Arrays.asList(crawler));
        return resources;
    }

    @Override
    public Set<Class<?>> getClasses() {
        return new HashSet<>();
    }

}
