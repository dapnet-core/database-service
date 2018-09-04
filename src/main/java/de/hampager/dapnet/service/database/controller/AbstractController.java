package de.hampager.dapnet.service.database.controller;

import java.net.URI;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.hampager.dapnet.service.database.AppUser;
import de.hampager.dapnet.service.database.DbConfig;
import de.hampager.dapnet.service.database.model.PermissionValue;

/**
 * Base class for REST controllers.
 * 
 * @author Philipp Thiel
 */
public abstract class AbstractController {

	@Autowired
	protected ObjectMapper mapper;

	private static final Set<String> VALID_PARAMS = Set.of("limit", "skip", "startkey", "endkey", "key");
	protected final RestTemplate restTemplate;
	protected final String basePath;
	protected final String paramPath;
	protected final String viewBasePath;

	protected AbstractController(DbConfig config, RestTemplateBuilder builder, String path) {
		if (config.getUser() == null || config.getUser().isEmpty() || config.getPassword() == null
				|| config.getPassword().isEmpty()) {
			restTemplate = builder.build();
		} else {
			restTemplate = builder.basicAuthorization(config.getUser(), config.getPassword()).build();
		}

		basePath = String.format("%s/%s/", config.getHost(), path);
		paramPath = basePath.concat("{param}");
		viewBasePath = String.format("%s/_design/%s/_view/", basePath, path);
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

	protected URI buildViewPath(String viewName, Map<String, String> requestParams) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(viewBasePath);
		builder.path(viewName);
		builder.queryParam("include_docs", "true").queryParam("limit", "20");

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

	protected static PermissionValue requirePermissionValue(Authentication authentication, String permission,
			PermissionValue... validValues) {
		final PermissionValue current = ((AppUser) authentication.getPrincipal()).getPermissions()
				.getOrDefault(permission, PermissionValue.NONE);
		final boolean notNone = current != PermissionValue.NONE;

		boolean hasPermission = true;
		if (notNone && validValues != null && validValues.length > 1) {
			hasPermission = false;
			for (PermissionValue v : validValues) {
				if (current == v) {
					hasPermission = true;
					break;
				}
			}
		}

		if (notNone && hasPermission) {
			return current;
		} else {
			throw new HttpServerErrorException(HttpStatus.FORBIDDEN);
		}
	}

	protected static PermissionValue requireAdminOrOwner(Authentication authentication, String permission,
			String ownerName) {
		final PermissionValue current = ((AppUser) authentication.getPrincipal()).getPermissions()
				.getOrDefault(permission, PermissionValue.NONE);
		final boolean notNone = current != PermissionValue.NONE;

		boolean hasPermission = current == PermissionValue.ALL;
		if (notNone && !hasPermission) {
			hasPermission = ownerName.equalsIgnoreCase(authentication.getName());
		}

		if (notNone && hasPermission) {
			return current;
		} else {
			throw new HttpServerErrorException(HttpStatus.FORBIDDEN);
		}
	}

}