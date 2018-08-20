package de.hampager.dapnet.service.database;

enum AuthPermission {

	USER_LIST("user.list"), USER_READ_LIST("user.read"), USER_READ("user.read"), USER_CREATE("user.create"),
	USER_UPDATE("user.update"), USER_DELETE("user.delete"), USER_CHANGE_ROLE("user.change_role");

	private final String path;

	private AuthPermission(String path) {
		this.path = "/auth/users/permission/" + path;
	}

	public String getPath() {
		return path;
	}

}
