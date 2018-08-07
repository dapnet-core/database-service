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
	private static final ObjectRegistry<AbstractClient> restClients = new ObjectRegistry<>();
	private static volatile ImmutableConfiguration serviceConfig;
	private static volatile RestServer restServer;
	private static volatile RestClient restClient;

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
		LOGGER.info("Starting DAPNET database service {}", SERVICE_VERSION);

		try {
			registerShutdownHook();
			parseCommandLine(args);

			restClient = new RestClient(serviceConfig);
			restClients.put(new UserClient(restClient));
			restClients.put(new TransmitterClient(restClient));

			restServer = new RestServer(serviceConfig, restClients);
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
		opts.addOption("env", "override configuration from environment");

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

		loadConfiguration(cli.getOptionValue("config", DEFAULT_CONFIG), cli.hasOption("env"));
	}

	private static void loadConfiguration(String filename, boolean loadEnv) throws ConfigurationException {
		LOGGER.debug("Loading configuration from {}", filename);
		Configurations configs = new Configurations();
		Configuration config = configs.properties(filename);

		// Override from environment variables (used by Docker images)
		if (loadEnv) {
			config.setProperty("db.user", System.getenv("COUCHDB_USER"));
			config.setProperty("db.password", System.getenv("COUCHDB_PASSWORD"));

			if (System.getenv("NODE_NAME") != null) {
				config.setProperty("rest.hostname", "0.0.0.0");
				config.setProperty("rest.port", 80);
			}
		}

		serviceConfig = ConfigurationUtils.unmodifiableConfiguration(config);
	}

	private static void stop() {
		try {
			if (restServer != null) {
				restServer.close();
				restServer = null;
			}
		} catch (Exception ex) {
			LOGGER.error("Failed to stop the REST server.", ex);
		}

		try {
			if (restClient != null) {
				restClient.close();
			}
		} catch (Exception ex) {
			LOGGER.error("Failed to close REST client.", ex);
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
