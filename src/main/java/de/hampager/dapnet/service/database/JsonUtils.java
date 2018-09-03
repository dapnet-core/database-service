package de.hampager.dapnet.service.database;

import java.util.Iterator;
import java.util.Map.Entry;

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

	public static void validateRequiredFields(JsonNode json, String[] fields) throws MissingFieldException {
		for (String field : fields) {
			if (!json.hasNonNull(field)) {
				throw new MissingFieldException("Required field is missing.", field);
			}
		}
	}

	public static JsonNode trimStrings(JsonNode input) {
		final ObjectNode out = OBJECT_MAPPER.createObjectNode();
		final Iterator<Entry<String, JsonNode>> it = input.fields();
		while (it.hasNext()) {
			final Entry<String, JsonNode> entry = it.next();
			if (entry.getValue().isTextual()) {
				final String value = entry.getValue().asText();
				if (value != null) {
					out.put(entry.getKey(), value.trim());
				} else {
					out.set(entry.getKey(), null);
				}
			} else if (entry.getValue().isContainerNode()) {
				out.set(entry.getKey(), trimStrings(entry.getValue()));
			} else {
				out.set(entry.getKey(), entry.getValue());
			}
		}

		return out;
	}

	public static String replaceWhitespaces(String text) {
		if (text != null) {
			return text.replaceAll("\\s+", "");
		} else {
			return text;
		}
	}

}
