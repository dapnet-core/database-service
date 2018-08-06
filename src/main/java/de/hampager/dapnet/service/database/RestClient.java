package de.hampager.dapnet.service.database;

import javax.json.stream.JsonGenerator;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.apache.commons.configuration2.ImmutableConfiguration;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.jsonp.JsonProcessingFeature;

abstract class RestClient implements AutoCloseable {

	private final Object lockObj = new Object();
	private final Client client;
	protected WebTarget rootTarget;

	public RestClient(ImmutableConfiguration config) {
		final String server = config.getString("db.server", "couchdb");
		final int port = config.getInt("db.port", 5984);
		final String user = config.getString("db.user", null);
		final String password = config.getString("db.password", null);
		final boolean prettyPrint = config.getBoolean("rest.pretty_print", false);

		ClientConfig cc = new ClientConfig();
		client = ClientBuilder.newClient(cc);
		client.register(JsonProcessingFeature.class);
		client.property(JsonGenerator.PRETTY_PRINTING, prettyPrint);

		if (user != null && !user.isEmpty() && password != null && !password.isEmpty()) {
			final HttpAuthenticationFeature auth = HttpAuthenticationFeature.basic(user, password);
			client.register(auth);
		}

		final String endpoint = String.format("http://%s:%d/", server, port);
		rootTarget = client.target(endpoint);
	}

	@Override
	public void close() throws Exception {
		synchronized (lockObj) {
			if (client != null) {
				client.close();
			}
		}
	}

}
