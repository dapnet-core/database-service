package de.hampager.dapnet.service.database;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

abstract class AbstractResource {

	@Context
	protected UriInfo uriInfo;
	@Context
	protected HttpHeaders httpHeaders;
	@Context
	protected SecurityContext securityContext;

}
