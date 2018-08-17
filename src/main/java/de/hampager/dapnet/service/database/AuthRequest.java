package de.hampager.dapnet.service.database;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

class AuthRequest {

	@JsonProperty(required = true)
	private String username;
	@JsonProperty(required = true)
	private String password;
	@JsonIgnore
	private AuthPermission permission;
	@JsonIgnore
	private String param;

	public AuthRequest() {
	}

	public AuthRequest(String username, String password, AuthPermission permission) {
		this(username, password, permission, null);
	}

	public AuthRequest(String username, String password, AuthPermission permission, String param) {
		this.username = username;
		this.password = password;
		this.permission = permission;
		this.param = param;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public AuthPermission getPermission() {
		return permission;
	}

	public void setPermission(AuthPermission permission) {
		this.permission = permission;
	}

	public String getParam() {
		return param;
	}

	public void setParam(String param) {
		this.param = param;
	}

}
