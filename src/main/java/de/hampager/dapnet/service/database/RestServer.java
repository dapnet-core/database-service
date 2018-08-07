package de.hampager.dapnet.service.database;

import java.lang.System;
import java.net.URI;

import javax.json.stream.JsonGenerator;

import org.apache.commons.configuration2.ImmutableConfiguration;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jsonp.JsonProcessingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

final class RestServer implements AutoCloseable {

	private final HttpServer server;

	public RestServer(ImmutableConfiguration config, ObjectRegistry<RestClient> clients) throws Exception {
		// Read config values
		final String hostname = config.getString("rest.hostname", "localhost");
		final String path = config.getString("rest.path", "/");
		final int port = config.getInt("rest.port", 8080);
		final boolean prettyPrint = config.getBoolean("rest.pretty_print", false);

		// Configure endpoint
		URI endpoint;
		if (System.getenv("NODE_NAME") != null) {
			// Always listen on 0.0.0.0:80 in a docker container
			endpoint = new URI("http", null, "0.0.0.0", 80, path, null, null);
		} else {
			endpoint = new URI("http", null, hostname, port, path, null, null);
		}
		// Configure resources
		ResourceConfig rescfg = new ResourceConfig();
		rescfg.register(AuthFilter.class);
		rescfg.register(JsonProcessingFeature.class);
		rescfg.register(RolesAllowedDynamicFeature.class);
		rescfg.register(new DatabaseBinder(clients));
		// rescfg.register(NodeResource.class);
		// rescfg.register(RubricResource.class);
		// rescfg.register(SubscriberGroupResource.class);
		// rescfg.register(SubscriberResource.class);
		rescfg.register(TransmitterResource.class);
		rescfg.register(UserResource.class);
		// Configure properties
		rescfg.property(JsonGenerator.PRETTY_PRINTING, prettyPrint);

		// Create HTTP server
		server = GrizzlyHttpServerFactory.createHttpServer(endpoint, rescfg);
	}

	@Override
	public void close() throws Exception {
		server.shutdownNow();
	}

}
