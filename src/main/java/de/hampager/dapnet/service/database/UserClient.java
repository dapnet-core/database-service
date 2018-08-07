package de.hampager.dapnet.service.database;

import javax.json.JsonObject;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.configuration2.ImmutableConfiguration;

final class UserClient extends RestClient {

	private final WebTarget resourceTarget;

	public UserClient(ImmutableConfiguration config) {
		super(config);

		resourceTarget = rootTarget.path("users");
	}

	public JsonObject getAll(boolean fullData) {
		Response r = resourceTarget.path("_all_docs").queryParam("include_docs", String.valueOf(fullData))
				.request(MediaType.APPLICATION_JSON_TYPE).get();
		return r.readEntity(JsonObject.class);
	}

	public JsonObject get(String startKey, String endKey, boolean fullData) {
		Response r = resourceTarget.path("_all_docs").queryParam("startKey", startKey).queryParam("endKey", endKey)
				.queryParam("include_docs", String.valueOf(fullData)).request(MediaType.APPLICATION_JSON_TYPE).get();
		return r.readEntity(JsonObject.class);
	}

	public JsonObject get(int limit, int skip) {
		// TODO Implementation
		throw new UnsupportedOperationException();
	}

	public JsonObject get(String username) {
		Response r = resourceTarget.path(username).request(MediaType.APPLICATION_JSON_TYPE).get();
		return r.readEntity(JsonObject.class);
	}

}
