package de.hampager.dapnet.service.database.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum PermissionValue {

	NONE, LIMITED, IF_OWNER, ALL;

	@JsonCreator
	public static PermissionValue fromString(String value) {
		for (PermissionValue v : PermissionValue.values()) {
			if (v.name().equalsIgnoreCase(value)) {
				return v;
			}
		}

		return PermissionValue.NONE;
	}

}
