package org.jboss.resteasy.plugins.server.reactor.netty;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;
import org.jboss.resteasy.core.ResteasyDeploymentImpl;
import org.jboss.resteasy.core.interception.jaxrs.AbstractWriterInterceptorContext;
import org.jboss.resteasy.plugins.server.embedded.SecurityDomain;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.util.PortProvider;
import reactor.blockhound.BlockHound;
import reactor.blockhound.BlockingOperationError;
import reactor.core.scheduler.ReactorBlockHoundIntegration;

public class ReactorNettyContainer {

    private static final Logger log = Logger.getLogger(ReactorNettyContainer.class);

    public static ReactorNettyJaxrsServer reactorNettyJaxrsServer;

    @Provider
    public static class TestAppExceptionHandler implements ExceptionMapper<Throwable> {
        @Override
        public Response toResponse(Throwable exception)
        {
            exception.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(exception.getMessage()).build();
        }
    }

    public static ResteasyDeployment start(boolean enableBlockHound) throws Exception
    {
        if (enableBlockHound) {
            // Uncomment to to pass testMonoMap
            // BlockHound.builder().allowBlockingCallsInside(
            //      AbstractWriterInterceptorContext.class.getName(), "writeTo").install();
            BlockHound.install();
            log.info("Block-Hound installed. woof!");
        }
        return start("");
    }

    public static ResteasyDeployment start() throws Exception {
       return start(false);
    }

    public static ResteasyDeployment start(String bindPath) throws Exception
    {
        return start(bindPath, null);
    }

    public static void start(ResteasyDeployment deployment)
    {
        reactorNettyJaxrsServer = new ReactorNettyJaxrsServer();
        reactorNettyJaxrsServer.setDeployment(deployment);
        reactorNettyJaxrsServer.setPort(PortProvider.getPort());
        reactorNettyJaxrsServer.setRootResourcePath("");
        reactorNettyJaxrsServer.setSecurityDomain(null);
        reactorNettyJaxrsServer.start();
    }

    public static ResteasyDeployment start(ReactorNettyJaxrsServer server)
    {
        final ResteasyDeployment deployment = new ResteasyDeploymentImpl();
        reactorNettyJaxrsServer = server;
        reactorNettyJaxrsServer.setDeployment(deployment);
        reactorNettyJaxrsServer.start();
        return reactorNettyJaxrsServer.getDeployment();
    }

    public static ResteasyDeployment start(String bindPath, SecurityDomain domain) throws Exception
    {
        ResteasyDeployment deployment = new ResteasyDeploymentImpl();
        // TODO lazy code, remove soon.
        deployment.getActualProviderClasses().add(TestAppExceptionHandler.class);
        deployment.setSecurityEnabled(true);
        return start(bindPath, domain, deployment);
    }

    public static ResteasyDeployment start(
            String bindPath,
            SecurityDomain domain,
            ResteasyDeployment deployment) throws Exception
    {
        reactorNettyJaxrsServer = new ReactorNettyJaxrsServer();
        reactorNettyJaxrsServer.setDeployment(deployment);
        reactorNettyJaxrsServer.setPort(PortProvider.getPort());
        reactorNettyJaxrsServer.setRootResourcePath(bindPath);
        reactorNettyJaxrsServer.setSecurityDomain(domain);
        reactorNettyJaxrsServer.start();
        return reactorNettyJaxrsServer.getDeployment();
    }

    public static void stop()
    {
        if (reactorNettyJaxrsServer != null)
        {
            try
            {
                reactorNettyJaxrsServer.stop();
            }
            catch (Exception e)
            {
                log.error("Failed to stop the server", e);
            }
        }
        reactorNettyJaxrsServer = null;
    }

    public static void main(String[] args) throws Exception {
        start();
    }
}
