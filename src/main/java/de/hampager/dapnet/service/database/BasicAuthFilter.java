package de.hampager.dapnet.service.database;

import java.io.IOException;
import java.security.Principal;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHENTICATION)
@PreMatching
public final class BasicAuthFilter implements ContainerRequestFilter {

	@Inject
	private UserClient users;

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		final String authCreds = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
		if (authCreds == null) {
			return;
		}

		final byte[] decodedBytes = Base64.getDecoder().decode(authCreds);
		if (decodedBytes == null || decodedBytes.length == 0) {
			requestContext.abortWith(Response.status(Status.UNAUTHORIZED).build());
		}

		final String userAndPassword = new String(decodedBytes, "UTF-8");
		final StringTokenizer tok = new StringTokenizer(userAndPassword, ":");
		final String username = tok.nextToken();
		final String password = tok.nextToken();

		if (username == null || password == null) {
			return;
		}

		JsonObject obj = users.get(username);
		if (obj == null || obj.containsKey("error") || !validatePassword(password, obj.getString("password"))) {
			requestContext.abortWith(Response.status(Status.UNAUTHORIZED).build());
		} else {
			SecurityContext oldContext = requestContext.getSecurityContext();
			requestContext.setSecurityContext(new BasicSecurityContext(obj, oldContext.isSecure()));
		}
	}

	private static boolean validatePassword(String typedPassword, String storedPassword) {
		return storedPassword.equals(typedPassword);
	}

	private static final class UserPrincipal implements Principal {

		private final String username;
		private final Set<String> roles;

		public UserPrincipal(String username, Set<String> roles) {
			this.username = username;
			this.roles = Collections.unmodifiableSet(roles);
		}

		@Override
		public String getName() {
			return username;
		}

		public Set<String> getRoles() {
			return roles;
		}

	}

	private static final class BasicSecurityContext implements SecurityContext {

		private final boolean secure;
		private final UserPrincipal principal;

		public BasicSecurityContext(JsonObject user, boolean secure) {
			this.secure = secure;

			final String username = user.getString("_id");
			final Set<String> roles = new HashSet<>();
			JsonArray roleArray = user.getJsonArray("roles");
			if (roleArray != null) {
				roleArray.forEach(r -> roles.add(r.toString()));
			}

			this.principal = new UserPrincipal(username, roles);
		}

		@Override
		public Principal getUserPrincipal() {
			return principal;
		}

		@Override
		public boolean isUserInRole(String role) {
			return principal.getRoles().contains(role);
		}

		@Override
		public boolean isSecure() {
			return secure;
		}

		@Override
		public String getAuthenticationScheme() {
			return SecurityContext.BASIC_AUTH;
		}

	}

}
