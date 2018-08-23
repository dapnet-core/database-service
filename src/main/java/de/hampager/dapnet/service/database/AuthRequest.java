package de.hampager.dapnet.service.database;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class represents an authentication request sent to the auth service.
 * 
 * @author Philipp Thiel
 */
class AuthRequest {

	@JsonProperty(required = true)
	private String username;
	@JsonProperty(required = true)
	private String password;
	@JsonIgnore
	private String path;
	@JsonIgnore
	private String param;

	/**
	 * Default constructor
	 */
	public AuthRequest() {
	}

	/**
	 * Constructs a new authentication request.
	 * 
	 * @param username Username
	 * @param password Password
	 * @param path     Requested permission path
	 */
	public AuthRequest(String username, String password, String path) {
		this(username, password, path, null);
	}

	/**
	 * Constructs a new authentication request.
	 * 
	 * @param username Username
	 * @param password Password
	 * @param path     Requested permission path
	 * @param param    Optional permission parameter
	 */
	public AuthRequest(String username, String password, String path, String param) {
		this.username = username;
		this.password = password;
		this.path = path;
		this.param = param;
	}

	/**
	 * Gets the username.
	 * 
	 * @return Username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Sets the username.
	 * 
	 * @param username Username
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Gets the password.
	 * 
	 * @return Password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Sets the password.
	 * 
	 * @param password Password
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Gets the requested permission path.
	 * 
	 * @return Permission path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Sets the requested permission path.
	 * 
	 * @param path Permission path
	 */
	public void setPermission(String path) {
		this.path = path;
	}

	/**
	 * Gets the optional permission parameter.
	 * 
	 * @return Permission parameter or {@code null}.
	 */
	public String getParam() {
		return param;
	}

	/**
	 * Sets the optional permission parameter.
	 * 
	 * @param param Permission parameter
	 */
	public void setParam(String param) {
		this.param = param;
	}

}
