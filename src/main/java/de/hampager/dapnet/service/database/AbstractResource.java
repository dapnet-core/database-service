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
	private SecurityContext securityContext;

	protected boolean isAuthenticated() {
		return securityContext.getUserPrincipal() != null;
	}

	protected String getUsername() {
		if (securityContext.getUserPrincipal() != null) {
			return securityContext.getUserPrincipal().getName();
		} else {
			return null;
		}
	}

	protected boolean isUserInRole(String role) {
		return securityContext.isUserInRole(role);
	}

	protected boolean isCurrentUser(String username) {
		return username.equalsIgnoreCase(getUsername());
	}

}
