package de.hampager.dapnet.service.database;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.WebRequest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

abstract class AbstractController {

	@Autowired
	protected ObjectMapper mapper;
	@Autowired
	private AuthService auth;

	protected final RestTemplate restTemplate;
	private final String basePath;

	protected AbstractController(DbConfig config, RestTemplateBuilder builder, String basePath) {
		if (config.getUser() == null || config.getUser().isEmpty() || config.getPassword() == null
				|| config.getPassword().isEmpty()) {
			restTemplate = builder.build();
		} else {
			restTemplate = builder.basicAuthorization(config.getUser(), config.getPassword()).build();
		}

		this.basePath = String.format("%s/%s/", config.getHost(), basePath);
	}

	protected String getBasePath() {
		return basePath;
	}

	protected String getPath(String path) {
		return basePath + path;
	}

	@ExceptionHandler(HttpClientErrorException.class)
	public ResponseEntity<JsonNode> handleClientError(HttpClientErrorException ex, WebRequest request) {
		ObjectNode n = mapper.createObjectNode();
		switch (ex.getStatusCode()) {
		case NOT_FOUND:
			n.put("error", "Object or endpoint not found.");
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(n);
		case FORBIDDEN:
			n.put("error", "Unauthorized or access forbidden.");
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(n);
		default:
			n.put("error", ex.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(n);
		}
	}

	protected void ensureAuthenticated(Authentication authentication, AuthPermission permission) {
		ensureAuthenticated(authentication, permission, null);
	}

	protected void ensureAuthenticated(Authentication authentication, AuthPermission permission, String param) {
		AuthResponse response = authenticate(authentication, permission, param);
		if (response == null || !response.isAllowed()) {
			throw new HttpClientErrorException(HttpStatus.FORBIDDEN);
		}
	}

	protected AuthResponse authenticate(Authentication authentication, AuthPermission permission) {
		return authenticate(authentication, permission, null);
	}

	protected AuthResponse authenticate(Authentication authentication, AuthPermission permission, String param) {
		UserDetails user = (UserDetails) authentication.getPrincipal();
		AuthRequest authreq = new AuthRequest(user.getUsername(), user.getPassword(), permission, param);
		return auth.authenticate(authreq);
	}

}
