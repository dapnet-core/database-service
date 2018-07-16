package de.hampager.dapnet.service.database;

import javax.inject.Inject;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import com.fasterxml.jackson.databind.ObjectMapper;

abstract class AbstractResource {

	@Inject
	protected ObjectMapper objectMapper;
	@Context
	protected UriInfo uriInfo;
	@Context
	protected HttpHeaders httpHeaders;

}
