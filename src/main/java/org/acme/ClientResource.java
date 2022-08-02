package org.acme;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.smallrye.mutiny.Uni;

@Path("/hello")
@RegisterRestClient(configKey = "api")
public interface ClientResource {

    @GET
    String test();

    @GET
    Uni<String> testAsync();

}
