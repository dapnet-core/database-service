package de.hampager.dapnet.service.database;

import org.apache.commons.configuration2.ImmutableConfiguration;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Provides the binder for injection of the Jackson {@link ObjectMapper}
 * instance.
 * 
 * @author Philipp Thiel
 */
public class ObjectMapperBinder extends AbstractBinder {

	private final ObjectMapper objectMapper = new ObjectMapper();

	public ObjectMapperBinder(ImmutableConfiguration config) {
		// Read config
		final boolean prettyPrint = config.getBoolean("rest.pretty_print", false);

		objectMapper.configure(SerializationFeature.INDENT_OUTPUT, prettyPrint);
		objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

		// SimpleModule module = new SimpleModule("configModule");
		// module.addSerializer(new DurationSerializer());
		// objectMapper.registerModule(module);
	}

	@Override
	protected void configure() {
		bind(objectMapper);
	}

}
