package de.hampager.dapnet.service.database;

import java.io.IOException;

import javax.json.JsonObject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

final class TransmitterClient extends AbstractClient {

	public TransmitterClient(RestClient client) {
		super(client, "transmitters");
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
