package de.hampager.dapnet.service.database;

class MissingFieldException extends Exception {

	private static final long serialVersionUID = 1L;

	public MissingFieldException() {
	}

	public MissingFieldException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public MissingFieldException(String message, Throwable cause) {
		super(message, cause);
	}

	public MissingFieldException(String message) {
		super(message);
	}

	public MissingFieldException(Throwable cause) {
		super(cause);
	}

}
