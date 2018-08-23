package de.hampager.dapnet.service.database;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
class AuthService {

	private final RestTemplate restTemplate = new RestTemplate();
	@Value("${auth.service}")
	private String authServiceUrl;

	@Autowired
	private BCryptPasswordEncoder passwordEncoder;

	public AuthResponse authenticate(AuthRequest request) {
		request.setPassword(passwordEncoder.encode(request.getPassword()));

		return restTemplate.getForObject(buildPath(request), AuthResponse.class);
	}

	private String buildPath(AuthRequest request) {
		String path = authServiceUrl + "/" + request.getPermission().getPath();
		if (request.getParam() != null) {
			path = path + "/" + request.getParam();
		}

		return path;
	}

}
