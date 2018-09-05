package de.hampager.dapnet.service.database;

import org.springframework.security.core.Authentication;

@FunctionalInterface
public interface AuthenticationFacade {

	Authentication getAuthentication();

}
