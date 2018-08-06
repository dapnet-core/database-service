package de.hampager.dapnet.service.database;

import java.io.IOException;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.configuration2.ImmutableConfiguration;

import com.fasterxml.jackson.databind.JsonNode;

final class UserClient extends RestClient {

	private final WebTarget resourceTarget;

	public UserClient(ImmutableConfiguration config) {
		super(config);

		resourceTarget = rootTarget.path("users");
	}

	public JsonNode getUser(String username) throws IOException {
		Response r = resourceTarget.path(username).request(MediaType.APPLICATION_JSON_TYPE).get();
		if (r.getStatus() == 200) {
			String raw = r.readEntity(String.class);
			return objectMapper.readTree(raw);
		} else {
			throw new RuntimeException("Invalid response.");
		}
	}

}
