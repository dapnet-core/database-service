package de.hampager.dapnet.service.database;

import java.util.Base64;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
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

	protected void ensureAuthenticated(AuthPermission permission) {
		ensureAuthenticated(permission, null);
	}

	protected void ensureAuthenticated(AuthPermission permission, String param) {
		AuthResponse response = authenticate(permission, param);
		if (response == null || !response.isAllowed()) {
			throw new HttpClientErrorException(HttpStatus.FORBIDDEN);
		}
	}

	protected AuthResponse authenticate(AuthPermission permission) {
		return authenticate(permission, null);
	}

	protected AuthResponse authenticate(AuthPermission permission, String param) {
		try {
			final HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
					.currentRequestAttributes()).getRequest();
			String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
			if (authHeader == null || authHeader.isEmpty() || !authHeader.startsWith("Basic")) {
				return null;
			}

			authHeader = authHeader.substring("Basic".length()).trim();

			final byte[] decodedBytes = Base64.getDecoder().decode(authHeader);
			if (decodedBytes == null || decodedBytes.length == 0) {
				return null;
			}

			final String userAndPassword = new String(decodedBytes, "UTF-8");
			final String[] creds = userAndPassword.split(":", 2);
			if (creds[0] != null && creds[1] == null) {
				return auth.authenticate(new AuthRequest(creds[0], creds[1], permission, param));
			} else {
				return null;
			}
		} catch (Exception ex) {
			return null;
		}
	}

}
