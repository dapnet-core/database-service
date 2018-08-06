package de.hampager.dapnet.service.database;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.ConfigurationUtils;
import org.apache.commons.configuration2.ImmutableConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class contains the application entry point.
 * 
 * @author Philipp Thiel
 */
public final class App {

	private static final Logger LOGGER = LogManager.getLogger();
	private static final String DEFAULT_CONFIG = "service.properties";
	private static final String SERVICE_VERSION;
	private static volatile ImmutableConfiguration serviceConfig;
	private static volatile RestServer restServer;

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
		// Use log4j instead of JUL
		System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");

		LOGGER.info("Starting DAPNET database service {}", SERVICE_VERSION);

		try {
			registerShutdownHook();
			parseCommandLine(args);

			restServer = new RestServer(serviceConfig);

//			try (UserClient users = new UserClient(serviceConfig)) {
//				String res = users.getUser("dh3wr");
//				System.out.println(res);
//			}
		} catch (Exception ex) {
			LOGGER.fatal("Service startup failed!", ex);
		}
	}

	/**
	 * Gets the service version.
	 * 
	 * @return Service version string.
	 */
	public static String getVersion() {
		return SERVICE_VERSION;
	}

	private static void parseCommandLine(String[] args) throws ParseException, ConfigurationException {
		Options opts = new Options();
		opts.addOption("h", "help", false, "print help text");
		opts.addOption("v", "version", false, "print version information");
		opts.addOption("c", "config", true, "configuration file to use");

		CommandLineParser parser = new DefaultParser();
		CommandLine cli = parser.parse(opts, args);
		if (cli.hasOption("help")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("database-service [options]", opts);
			System.exit(1);
		} else if (cli.hasOption("version")) {
			System.out.println("DAPNET database service " + SERVICE_VERSION);
			System.exit(1);
		}

		// Load configuration file
		String param = cli.getOptionValue("config");
		if (param != null) {
			loadConfiguration(param);
		} else {
			loadConfiguration(DEFAULT_CONFIG);
		}
	}

	private static void loadConfiguration(String filename) throws ConfigurationException {
		LOGGER.debug("Loading configuration from {}", filename);
		Configurations configs = new Configurations();
		Configuration config = configs.properties(filename);
		serviceConfig = ConfigurationUtils.unmodifiableConfiguration(config);
	}

	private static void stop() {
		try {
			if (restServer != null) {
				restServer.close();
				restServer = null;
			}
		} catch (Exception ex) {
			LOGGER.error("Failed to stop the HTTP server.", ex);
		}
	}

	private static void registerShutdownHook() {
		Runnable r = () -> {
			stop();
			// Log4j automatic shutdown hook is disabled, call it manually
			LogManager.shutdown();
		};

		Runtime.getRuntime().addShutdownHook(new Thread(r));
	}

}
