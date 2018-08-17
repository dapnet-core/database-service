package de.hampager.dapnet.service.database;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * This class contains the application entry point.
 *
 * @author Philipp Thiel
 */
@SpringBootApplication
public class App {

	private static final String SERVICE_VERSION;

	static {
		// Read service version from package
		String ver = App.class.getPackage().getImplementationVersion();
		SERVICE_VERSION = ver != null ? ver : "UNKNOWN";
	}

	/**
	 * The application entry point.
	 *
	 * @param args Command line arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(App.class, args);
	}

	/**
	 * Gets the service version.
	 *
	 * @return Service version string.
	 */
	public static String getVersion() {
		return SERVICE_VERSION;
	}

}
