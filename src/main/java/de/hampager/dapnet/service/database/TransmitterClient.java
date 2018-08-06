package de.hampager.dapnet.service.database;

import java.io.IOException;

import javax.json.JsonObject;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.configuration2.ImmutableConfiguration;

final class TransmitterClient extends RestClient {

	private final WebTarget resourceTarget;

	public TransmitterClient(ImmutableConfiguration config) {
		super(config);

		resourceTarget = rootTarget.path("transmitters");
	}

	public JsonObject getAll() throws IOException {
		Response r = resourceTarget.path("_all_docs").queryParam("include_docs", "true")
				.request(MediaType.APPLICATION_JSON_TYPE).get();
		return r.readEntity(JsonObject.class);
	}

	public JsonObject get(String id) throws IOException {
		Response r = resourceTarget.path(id).request(MediaType.APPLICATION_JSON_TYPE).get();
		return r.readEntity(JsonObject.class);
	}

}
