package de.hampager.dapnet.service.database;

/**
 * Exception that can be thrown if a required field is missing.
 * 
 * @author Philipp Thiel
 */
public class MissingFieldException extends Exception {

	private static final long serialVersionUID = 1L;
	private final String fieldName;

	public MissingFieldException(String fieldName) {
		this.fieldName = fieldName;
	}

	public MissingFieldException(String message, String fieldName) {
		super(message);
		this.fieldName = fieldName;
	}

	public String getFieldName() {
		return fieldName;
	}

}
