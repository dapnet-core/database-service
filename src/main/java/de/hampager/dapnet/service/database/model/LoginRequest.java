package de.hampager.dapnet.service.database.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class represents an authentication request sent to the auth service.
 * 
 * @author Philipp Thiel
 */
public class LoginRequest implements Serializable {

	private static final long serialVersionUID = 1L;
	@JsonProperty(required = true)
	private String username;
	@JsonProperty(required = true)
	private String password;

	/**
	 * Default constructor
	 */
	public LoginRequest() {
	}

	/**
	 * Constructs a new login request.
	 * 
	 * @param username Username
	 * @param password Password
	 */
	public LoginRequest(String username, String password) {
		this.username = username;
		this.password = password;
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

}
