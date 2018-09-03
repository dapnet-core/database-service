package de.hampager.dapnet.service.database;

/**
 * This class represents the database configuration.
 */
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("db")
public class DbConfig {

	private String host;
	private String user;
	private String password;

	/**
	 * Gets the database host name.
	 * 
	 * @return Database host
	 */
	public String getHost() {
		return host;
	}

	/**
	 * Sets the database host name.
	 * 
	 * @param host Database host
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * Gets the database user name.
	 * 
	 * @return Username
	 */
	public String getUser() {
		return user;
	}

	/**
	 * Sets the database user name.
	 * 
	 * @param user Username
	 */
	public void setUser(String user) {
		this.user = user;
	}

	/**
	 * Gets the database password.
	 * 
	 * @return Password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Sets the database password.
	 * 
	 * @param password Password
	 */
	public void setPassword(String password) {
		this.password = password;
	}

}
