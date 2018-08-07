package de.hampager.dapnet.service.database;

import javax.ws.rs.client.WebTarget;

abstract class AbstractClient {

	protected final WebTarget resourceTarget;

	protected AbstractClient(RestClient client, String path) {
		resourceTarget = client.createTarget(path);
	}

}
