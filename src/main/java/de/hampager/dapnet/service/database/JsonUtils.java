package de.hampager.dapnet.service.database;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * This class contains JSON utility methods
 * 
 * @author Philipp Thiel
 */
public class JsonUtils {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
		final ObjectNode out = OBJECT_MAPPER.createObjectNode();
		final Iterator<Entry<String, JsonNode>> it = input.fields();
		while (it.hasNext()) {
			final Entry<String, JsonNode> e = it.next();
			if (e.getValue().isTextual()) {
				final String value = e.getValue().asText();
				if (value != null) {
					out.put(e.getKey(), value.trim());
				} else {
					out.set(e.getKey(), null);
				}
			} else if (e.getValue().isContainerNode()) {
				out.set(e.getKey(), trimValues(e.getValue()));
			} else {
				out.set(e.getKey(), e.getValue());
			}
		}

		return out;
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

}
