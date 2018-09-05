package de.hampager.dapnet.service.database.model;

import java.io.Serializable;
import java.util.Map;

/**
 * This class represents the authentication response sent by the auth service.
 * 
 * @author Philipp Thiel
 */
public class LoginResponse implements Serializable {

	private static final long serialVersionUID = 1L;
	private LoginUser user;
	private Map<String, PermissionValue> permissions;

	public LoginUser getUser() {
		return user;
	}

	public void setUser(LoginUser user) {
		this.user = user;
	}

	public Map<String, PermissionValue> getPermissions() {
		return permissions;
	}

	public void setPermissions(Map<String, PermissionValue> permissions) {
		this.permissions = permissions;
	}

}
