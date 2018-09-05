package de.hampager.dapnet.service.database.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LoginUser implements Serializable {

	private static final long serialVersionUID = 1L;
	@JsonProperty(value = "_id", required = true)
	private String id;
	@JsonProperty(value = "_rev", required = true)
	private String revision;
	@JsonProperty(required = true)
	private String email;
	@JsonProperty(required = true)
	private boolean enabled;
	@JsonProperty(required = true)
	private Set<String> roles;
	@JsonProperty(value = "created_on", required = true)
	private Instant createdOn;
	@JsonProperty(value = "created_by", required = true)
	private String createdBy;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getRevision() {
		return revision;
	}

	public void setRevision(String revision) {
		this.revision = revision;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Set<String> getRoles() {
		return roles;
	}

	public void setRoles(Set<String> roles) {
		this.roles = roles;
	}

	public Instant getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Instant createdOn) {
		this.createdOn = createdOn;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

}
