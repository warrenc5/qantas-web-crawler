package devnull.qantas.crawler;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.servlet.ServletContext;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;

@ApplicationPath("rest")
@Stateless
public class QantasApplication extends Application {

    @EJB
    CrawlerLocal crawler;

    @Context
    protected ServletContext servletContext;

    public QantasApplication() {
    }

    @Override
    public Set<Object> getSingletons() {
        Set resources = new java.util.HashSet(Arrays.asList(crawler));
        return resources;
    }

    @Override
    public Set<Class<?>> getClasses() {
        return new HashSet<>();
    }

}
