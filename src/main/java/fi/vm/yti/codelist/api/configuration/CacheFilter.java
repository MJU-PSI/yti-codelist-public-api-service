package fi.vm.yti.codelist.api.configuration;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

@Provider
public class CacheFilter implements ContainerResponseFilter {

    @Override
    public void filter(final ContainerRequestContext request,
                       final ContainerResponseContext response) throws IOException {
        response.getHeaders().add("Cache-Control", "no-cache");
    }
}