package de.hampager.dapnet.service.database.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class contains static methods to validate various common inputs.
 * 
 * @author Philipp Thiel
 */
public final class CommonValidation {

	private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z0-9\\-]${3,40}");
	private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9\\.\\-_]{3,40}$");

	private CommonValidation() {
	}

	public static void validateRubricName(String rubricName) throws InputValidationException {
		final Matcher matcher = NAME_PATTERN.matcher(rubricName);
		if (!matcher.matches()) {
			throw new InputValidationException("Invalid rubric name: " + rubricName);
		}
	}

	public static void validateRubricNumber(int number) throws InputValidationException {
		if (number < 1 || number > 95) {
			throw new InputValidationException("Invalid rubric number: " + number);
		}
	}

	public static void validateDescription(String description) throws InputValidationException {
		if (description != null && description.length() > 45) {
			throw new InputValidationException("Description too long: " + description);
		}
	}

	public static void validateAuthKey(String authKey) throws InputValidationException {
		final Matcher matcher = NAME_PATTERN.matcher(authKey);
		if (!matcher.matches()) {
			throw new InputValidationException("Invalid auth key: " + authKey);
		}
	}

	public static void validateTransmitterGroupName(String groupName) throws InputValidationException {
		final Matcher matcher = NAME_PATTERN.matcher(groupName);
		if (!matcher.matches()) {
			throw new InputValidationException("Invalid transmitter group name: " + groupName);
		}
	}

	public static void validateUsername(String username) throws InputValidationException {
		final Matcher matcher = USERNAME_PATTERN.matcher(username);
		if (!matcher.matches()) {
			throw new InputValidationException("Invalid username: " + username);
		}
	}

}
