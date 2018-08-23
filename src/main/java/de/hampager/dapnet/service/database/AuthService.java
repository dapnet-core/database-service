package de.hampager.dapnet.service.database;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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
		this.url = serviceUrl + "/auth/users/permission/{path}/{param}";
	}

	/**
	 * Performs an authentication attempt against the DAPNET auth service.
	 * 
	 * @param request Authentication request
	 * @return Authentication response
	 */
	public AuthResponse authenticate(AuthRequest request) {
		return restTemplate.postForObject(url, request, AuthResponse.class, request.getPath(), request.getParam());
	}

}
