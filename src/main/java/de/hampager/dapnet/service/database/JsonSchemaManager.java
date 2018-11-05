package de.hampager.dapnet.service.database;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.ValidationMessage;

/**
 * This class implements a reusable JSON schema validator.
 * 
 * @author Philipp Thiel
 */
public class JsonSchemaManager {

	private final JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance();
	private final Map<String, JsonSchema> loadedSchemas = new HashMap<>();

	private JsonSchema getSchema(String schemaPath) throws IOException {
		final String key = schemaPath.toLowerCase();

		JsonSchema schema;
		synchronized (loadedSchemas) {
			schema = loadedSchemas.get(key);
			if (schema == null) {
				final InputStream in = getClass().getResourceAsStream(schemaPath);
				if (in == null) {
					throw new FileNotFoundException(schemaPath);
				}

				schema = schemaFactory.getSchema(in);
				loadedSchemas.put(key, schema);
			}
		}

		return schema;
	}

	/**
	 * Validates the given JSON node against a schema loaded from a resource file.
	 * The schema will be cached once it has been loaded.
	 * 
	 * @param input      JSON node to validate.
	 * @param schemaPath Path of the JSON schema to use.
	 * @return Set of validation messages or empty set if the validation succeeded.
	 * @throws IOException if an IO error occurred, for example if the schema file
	 *                     could not be found.
	 */
	public Set<ValidationMessage> validate(JsonNode input, String schemaPath) throws IOException {
		final JsonSchema schema = getSchema(schemaPath);
		return schema.validate(input);
	}

}
