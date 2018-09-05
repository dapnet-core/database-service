package de.hampager.dapnet.service.database;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import de.hampager.dapnet.service.database.model.LoginRequest;
import de.hampager.dapnet.service.database.model.LoginResponse;

/**
 * This class implements the authentication service.
 * 
 * @author Philipp Thiel
 */
@Service
class DapnetAuthService {

	private final RestTemplate restTemplate = new RestTemplate();
	private final String url;

	public DapnetAuthService(@Value("${auth.service}") String serviceUrl) {
		this.url = serviceUrl + "/auth/users/login";
	}

	/**
	 * Performs an authentication attempt against the DAPNET auth service.
	 * 
	 * @param request Authentication request
	 * @return Authentication response
	 */
	public LoginResponse login(String username, String password) {
		LoginRequest request = new LoginRequest(username, password);
		return restTemplate.postForObject(url, request, LoginResponse.class);
	}

}
