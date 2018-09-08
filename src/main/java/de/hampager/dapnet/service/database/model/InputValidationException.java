package de.hampager.dapnet.service.database.model;

/**
 * This exception is thrown on model validation errors.
 * 
 * @author Philipp Thiel
 */
public class InputValidationException extends Exception {

	private static final long serialVersionUID = 1L;

	public InputValidationException(String message, Throwable cause) {
		super(message, cause);
	}

	public InputValidationException(String message) {
		super(message);
	}

}
