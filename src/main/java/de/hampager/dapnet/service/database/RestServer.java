package de.hampager.dapnet.service.database;

import java.net.URI;

import org.apache.commons.configuration2.ImmutableConfiguration;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

final class RestServer implements AutoCloseable {

	private final HttpServer server;

	public RestServer(ImmutableConfiguration config) throws Exception {
		// Read config values
		final String hostname = config.getString("rest.hostname", "localhost");
		final String path = config.getString("rest.path", "/");
		final int port = config.getInt("rest.port", 8080);

		// Configure endpoint
		URI endpoint = new URI("http", null, hostname, port, path, null, null);
		// Configure resources
		ResourceConfig rescfg = new ResourceConfig();
		rescfg.register(new ObjectMapperBinder(config));
		rescfg.register(NodeResource.class);
		rescfg.register(RubricResource.class);
		rescfg.register(SubscriberGroupResource.class);
		rescfg.register(SubscriberResource.class);
		rescfg.register(TransmitterResource.class);
		rescfg.register(UserResource.class);

		// Create HTTP server
		server = GrizzlyHttpServerFactory.createHttpServer(endpoint, rescfg);
	}

	@Override
	public void close() throws Exception {
		server.shutdownNow();
	}

}
