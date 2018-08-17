package de.hampager.dapnet.service.database;

enum AuthPermission {

	USER_LIST("user.list"), USER_READ_LIST("user.read"), USER_READ("user.read");

	private final String path;

	private AuthPermission(String path) {
		this.path = "/auth/users/permission/" + path;
	}

	public String getPath() {
		return path;
	}

}
