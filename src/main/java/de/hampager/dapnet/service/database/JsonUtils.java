package de.hampager.dapnet.service.database;

import com.fasterxml.jackson.databind.JsonNode;

class JsonUtils {

	private JsonUtils() {
	}

	public static void validateRequiredFields(JsonNode json, String[] fields) throws MissingFieldException {
		for (String field : fields) {
			if (!json.hasNonNull(field)) {
				throw new MissingFieldException("Field '" + field + "' is missing.");
			}
		}
	}

}
