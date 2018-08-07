package de.hampager.dapnet.service.database;

import java.util.Objects;

import org.glassfish.hk2.utilities.binding.AbstractBinder;

public final class DatabaseBinder extends AbstractBinder {

	private final ObjectRegistry<AbstractClient> clients;

	public DatabaseBinder(ObjectRegistry<AbstractClient> clients) {
		this.clients = Objects.requireNonNull(clients);
	}

	@Override
	protected void configure() {
		clients.forEach((c) -> bind(c));
	}

}
