package org.jboss.resteasy.plugins.server.reactor.netty;

import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.resteasy.spi.Registry;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import reactor.core.publisher.Mono;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.jboss.resteasy.test.TestPortProvider.generateURL;

public class MonoTest {

   @Path("/mono")
   public static class MonoResource {
      @GET
      public Mono<String> hello(@QueryParam("delay") Integer delayMs) {
         final Mono<String> businessLogic = Mono.just("Mono says hello!");

         return delayMs != null
                 ? businessLogic.delayElement(Duration.ofMillis(delayMs))
                 : businessLogic;
      }
      @Path("/map")
      @GET
      @Produces(MediaType.APPLICATION_JSON)
      public Mono<Map<String, String>> delayMap() {
            Map<String, String> map = new HashMap<>();
            map.put("key", "value");
            return Mono.just(map);
      }
   }

   private static Client client;

   @BeforeClass
   public static void setup() throws Exception {
      ResteasyDeployment deployment = ReactorNettyContainer.start(true);
      // TODO Need to log stack trace using ExceptionMapper
      Registry registry = deployment.getRegistry();
      registry.addPerRequestResource(MonoResource.class);
      client = ClientBuilder.newClient();
   }

   @AfterClass
   public static void end() {
      client.close();
      ReactorNettyContainer.stop();
   }

   @Test(timeout = 5_000)
   public void testNoDelayMono() {
      WebTarget target = client.target(generateURL("/mono"));
      String val = target.request().get(String.class);
      Assert.assertEquals("Mono says hello!", val);
   }

   @Test(timeout = 5_000)
   public void testMonoMap() {
      WebTarget target = client.target(generateURL("/mono/map"));
      var response = target.request().get();

      if(response.getStatus() == Response.Status.OK.getStatusCode()) {
         var val = response.readEntity(new GenericType<Map<String, String>>() {
         });
         Assert.assertFalse("Mono returns value!", val.isEmpty());
      } else {
         Assert.fail(String.format("Got non-ok response. Code: %s, error: %s", response.getStatus(), response.readEntity(String.class)));
      }
   }

   @Test(timeout = 5_000)
   public void testDelayedMono() {
      WebTarget target = client.target(generateURL("/mono")).queryParam("delay", "1000");
      String val = target.request().get(String.class);
      Assert.assertEquals("Mono says hello!", val);
   }
}