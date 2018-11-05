package de.hampager.dapnet.service.database;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.networknt.schema.ValidationMessage;

/**
 * This class contains JSON utility methods
 * 
 * @author Philipp Thiel
 */
public class JsonUtils {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final JsonSchemaManager VALIDATOR = new JsonSchemaManager();

	private JsonUtils() {
	}

	public static void checkRequiredFields(JsonNode json, String[] fields) throws MissingFieldException {
		for (String field : fields) {
			if (!json.hasNonNull(field)) {
				throw new MissingFieldException("Required field is missing.", field);
			}
		}
	}

	public static JsonNode trimValues(JsonNode input) {
		if (input.isObject()) {
			final ObjectNode out = OBJECT_MAPPER.createObjectNode();
			input.fields().forEachRemaining(e -> out.set(e.getKey(), trimValues(e.getValue())));
			return out;
		} else if (input.isArray()) {
			final ArrayNode out = OBJECT_MAPPER.createArrayNode();
			input.elements().forEachRemaining(e -> out.add(trimValues(e)));
			return out;
		} else if (input.isTextual()) {
			final String text = input.asText();
			if (text != null) {
				return TextNode.valueOf(text.trim());
			}
		}

		return input;
	}

	public static boolean isOwner(JsonNode node, String username) {
		final JsonNode ownersNode = node.get("owners");
		if (ownersNode == null || !ownersNode.isArray()) {
			throw new IllegalArgumentException("Node does not contain owners array.");
		}

		final Iterator<JsonNode> owners = ownersNode.elements();
		while (owners.hasNext()) {
			if (owners.next().asText("").equalsIgnoreCase(username)) {
				return true;
			}
		}

		return false;
	}

	public static void keepFields(ObjectNode node, Set<String> fieldsToKeep) {
		final Iterator<Entry<String, JsonNode>> it = node.fields();
		while (it.hasNext()) {
			Entry<String, JsonNode> e = it.next();
			if (!fieldsToKeep.contains(e.getKey().toLowerCase())) {
				it.remove();
			}
		}
	}

	public static Set<ValidationMessage> validate(JsonNode input, String schemaPath) throws IOException {
		return VALIDATOR.validate(input, schemaPath);
	}

}
