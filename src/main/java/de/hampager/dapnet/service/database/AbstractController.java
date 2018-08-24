package de.hampager.dapnet.service.database;

import java.net.URI;
import java.util.Map;
import java.util.Set;

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
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Base class for REST controllers.
 * 
 * @author Philipp Thiel
 */
abstract class AbstractController {

	@Autowired
	protected ObjectMapper mapper;
	@Autowired
	private AuthService auth;

	private static final Set<String> VALID_PARAMS = Set.of("limit", "skip", "startkey", "endkey");
	protected final RestTemplate restTemplate;
	protected final String basePath;
	protected final String queryPath;

	protected AbstractController(DbConfig config, RestTemplateBuilder builder, String path) {
		if (config.getUser() == null || config.getUser().isEmpty() || config.getPassword() == null
				|| config.getPassword().isEmpty()) {
			restTemplate = builder.build();
		} else {
			restTemplate = builder.basicAuthorization(config.getUser(), config.getPassword()).build();
		}

		basePath = String.format("%s/%s/", config.getHost(), path);
		queryPath = basePath.concat("{query}");
	}

	@ExceptionHandler(HttpClientErrorException.class)
	public ResponseEntity<JsonNode> handleClientError(HttpClientErrorException ex, WebRequest request) {
		ObjectNode n = mapper.createObjectNode();
		switch (ex.getStatusCode()) {
		case NOT_FOUND:
			n.put("error", "Object or endpoint not found.");
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(n);
		case UNAUTHORIZED:
			n.put("error", "Unauthorized");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(n);
		case FORBIDDEN:
			n.put("error", "Access forbidden.");
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(n);
		default:
			n.put("error", ex.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(n);
		}
	}

	protected void ensureAuthenticated(Authentication authentication, String permission) {
		ensureAuthenticated(authentication, permission, null);
	}

	protected void ensureAuthenticated(Authentication authentication, String permission, String param) {
		AuthResponse response = authenticate(authentication, permission, param);
		if (response == null || !response.isAuthenticated()) {
			throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED);
		} else if (!response.isAllowed()) {
			throw new HttpClientErrorException(HttpStatus.FORBIDDEN);
		}
	}

	protected AuthResponse authenticate(Authentication authentication, String permission) {
		return authenticate(authentication, permission, null);
	}

	protected AuthResponse authenticate(Authentication authentication, String permission, String param) {
		UserDetails user = (UserDetails) authentication.getPrincipal();
		AuthRequest authreq = new AuthRequest(user.getUsername(), user.getPassword(), permission, param);
		return auth.authenticate(authreq);
	}

	protected URI buildAllDocsPath(Map<String, String> requestParams) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(basePath);
		builder.path("_all_docs").queryParam("include_docs", "true").queryParam("limit", "20");

		if (requestParams.containsKey("startswith")) {
			String value = requestParams.remove("startswith");
			if (value != null) {
				requestParams.put("startkey", value);
				requestParams.put("endkey", String.format("\"%s\\ufff0\"", value.replaceAll("\"", "")));
			}
		}

		requestParams.forEach((p, v) -> {
			if (VALID_PARAMS.contains(p)) {
				builder.replaceQueryParam(p, v);
			}
		});

		return builder.build().toUri();
	}

}
