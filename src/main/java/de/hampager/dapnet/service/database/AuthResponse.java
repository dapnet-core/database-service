package de.hampager.dapnet.service.database;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

class AuthResponse {

	@JsonProperty(value = "access", required = true)
	private boolean allowed;
	@JsonProperty(value = "limited_to", required = false)
	private Set<String> limitedTo;

	public boolean isAllowed() {
		return allowed;
	}

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

}
