package de.hampager.dapnet.service.database.model;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class represents the authentication response sent by the auth service.
 * 
 * @author Philipp Thiel
 */
public class AuthResponse {

	@JsonProperty(required = true)
	private boolean authenticated;
	@JsonProperty(value = "access", required = true)
	private boolean allowed;
	@JsonProperty(value = "limited_to", required = false)
	private Set<String> limitedTo;
	@JsonProperty(required = false)
	private Set<String> roles;

	/**
	 * Whether access to the requested function is allowed.
	 * 
	 * @return {@code true} if access is allowed.
	 */
	public boolean isAllowed() {
		return allowed;
	}

	/**
	 * Whether access to the requested function is allowed.
	 * 
	 * @param allowed {@code true} if access is allowed.
	 */
	public void setAllowed(boolean allowed) {
		this.allowed = allowed;
	}

	public Set<String> getLimitedTo() {
		return limitedTo;
	}

	public void setLimitedTo(Set<String> limitedTo) {
		this.limitedTo = limitedTo;
	}

	public boolean canAccess(String field) {
		return limitedTo != null ? limitedTo.contains(field) : true;
	}

	public boolean isAuthenticated() {
		return authenticated;
	}

	public void setAuthenticated(boolean authenticated) {
		this.authenticated = authenticated;
	}

	public Set<String> getRoles() {
		return roles;
	}

	public void setRoles(Set<String> roles) {
		this.roles = roles;
	}

}
