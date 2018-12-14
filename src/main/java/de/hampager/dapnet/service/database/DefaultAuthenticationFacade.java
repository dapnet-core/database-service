package de.hampager.dapnet.service.database;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Default authentication facade implementation.
 * 
 * @author Philipp Thiel
 */
@Component
public class DefaultAuthenticationFacade implements AuthenticationFacade {

	@Override
	public Authentication getAuthentication() {
		return SecurityContextHolder.getContext().getAuthentication();
	}

}
