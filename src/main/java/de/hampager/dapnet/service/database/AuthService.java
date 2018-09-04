package de.hampager.dapnet.service.database;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import de.hampager.dapnet.service.database.model.AuthRequest;
import de.hampager.dapnet.service.database.model.AuthResponse;

/**
 * This class implements the authentication service.
 * 
 * @author Philipp Thiel
 */
@Service
class AuthService {

	private final RestTemplate restTemplate = new RestTemplate();
	private final String url;

	public AuthService(@Value("${auth.service}") String serviceUrl) {
		this.url = serviceUrl + "/auth/users/login";
	}

	/**
	 * Performs an authentication attempt against the DAPNET auth service.
	 * 
	 * @param request Authentication request
	 * @return Authentication response
	 */
	public AuthResponse authenticate(String username, String password) {
		AuthRequest request = new AuthRequest(username, password);
		return restTemplate.postForObject(url, request, AuthResponse.class);
	}

}
