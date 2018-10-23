package de.hampager.dapnet.service.database;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * This class implements the users REST endpoint.
 * 
 * @author Philipp Thiel
 */
@CrossOrigin
@RestController
@RequestMapping("users")
class UserController extends AbstractController {

	private static final Logger logger = LoggerFactory.getLogger(UserController.class);
	private static final Set<String> VALID_KEYS_UPDATE = Set.of("email", "enabled", "password", "roles");
	private static final String[] REQUIRED_KEYS_CREATE = { "_id", "password", "email", "roles", "enabled",
			"email_valid" };
	private static final String USER_LIST = "user.list";
	private static final String USER_READ = "user.read";
	private static final String USER_UPDATE = "user.update";
	private static final String USER_CREATE = "user.create";
	private static final String USER_DELETE = "user.delete";
	private static final String USER_CHANGE_ROLE = "user.change_role";
	private final String usernamesPath;

	@Autowired
	public UserController(DbConfig config, RestTemplateBuilder builder) {
		super(config, builder, "users");

		this.usernamesPath = basePath.concat("_design/users/_list/usernames/byId");
	}

	@GetMapping
	public ResponseEntity<JsonNode> getAll(Authentication authentication, @RequestParam Map<String, String> params) {
		ensureAuthenticated(authentication, USER_READ);

		URI path = buildViewPath("byId", params);
		JsonNode in = restTemplate.getForObject(path, JsonNode.class);
		ObjectNode out = mapper.createObjectNode();

		out.put("total_rows", in.get("total_rows").asInt());
		ArrayNode rows = out.putArray("rows");
		for (JsonNode n : in.get("rows")) {
			JsonNode doc = n.get("doc");
			((ObjectNode) doc).remove("password");
			rows.add(doc);
		}

		return ResponseEntity.ok(out);
	}

	@GetMapping("_usernames")
	public ResponseEntity<JsonNode> getUsernames(Authentication authentication) {
		ensureAuthenticated(authentication, USER_LIST);

		JsonNode in = restTemplate.getForObject(usernamesPath, JsonNode.class);
		return ResponseEntity.ok(in);
	}

	@GetMapping("{username}")
	public ResponseEntity<JsonNode> getUser(Authentication authentication, @PathVariable String username) {
		ensureAuthenticated(authentication, USER_READ, username);

		JsonNode in = restTemplate.getForObject(paramPath, JsonNode.class, username);
		((ObjectNode) in).remove("password");
		return ResponseEntity.ok(in);
	}

	@PutMapping
	public ResponseEntity<JsonNode> putUser(Authentication authentication, @RequestBody JsonNode user) {
		if (user.has("_rev")) {
			return updateUser(authentication, user);
		} else {
			return createUser(authentication, user);
		}
	}

	private ResponseEntity<JsonNode> createUser(Authentication auth, JsonNode user) {
		ensureAuthenticated(auth, USER_CREATE);

		try {
			JsonUtils.validateRequiredFields(user, REQUIRED_KEYS_CREATE);
		} catch (MissingFieldException ex) {
			throw new HttpServerErrorException(HttpStatus.BAD_REQUEST, ex.getMessage());
		}

		ObjectNode modUser;
		try {
			modUser = (ObjectNode) user;
		} catch (ClassCastException ex) {
			logger.error("Failed to cast JsonNode to ObjectNode");
			throw new HttpServerErrorException(HttpStatus.BAD_REQUEST);
		}

		final String userId = modUser.get("_id").asText().trim().toLowerCase();
		// Convert _id to lowercase and remove all whitespaces
		modUser.put("_id", userId);

		// Remove all whitespaces from email
		modUser.put("email", modUser.get("email").asText().trim());

		final String ts = Instant.now().toString();
		modUser.put("created_on", ts);
		modUser.put("created_by", auth.getName());
		modUser.put("changed_on", ts);
		modUser.put("changed_by", auth.getName());

//		final HttpHeaders headers = new HttpHeaders();
//		headers.setContentType(MediaType.APPLICATION_JSON);
//		headers.setAccept(List.of(MediaType.APPLICATION_JSON));
//		final HttpEntity<JsonNode> request = new HttpEntity<JsonNode>(modUser, headers);
//		return restTemplate.exchange(paramPath, HttpMethod.PUT, request, JsonNode.class, modUser.get("_id").asText());
		final ResponseEntity<JsonNode> db = putRequest(paramPath, userId, modUser);
		if (db.getStatusCode() == HttpStatus.CREATED) {
			final URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("{id}").buildAndExpand(userId)
					.toUri();
			return ResponseEntity.created(location).body(db.getBody());
		} else {
			return ResponseEntity.status(db.getStatusCode()).body(db.getBody());
		}
	}

	private ResponseEntity<JsonNode> updateUser(Authentication auth, JsonNode userUpdate) {
		ensureAuthenticated(auth, USER_UPDATE, auth.getName());

		if (userUpdate.has("roles")) {
			ensureAuthenticated(auth, USER_CHANGE_ROLE, auth.getName());
		}

		final String userId = userUpdate.get("_id").asText().trim().toLowerCase();

		JsonNode oldUser = restTemplate.getForObject(paramPath, JsonNode.class, userId);
		ObjectNode modUser;
		try {
			modUser = (ObjectNode) oldUser;
		} catch (ClassCastException ex) {
			logger.error("Failed to cast JsonNode to ObjectNode");
			throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
		}

		userUpdate.fields().forEachRemaining(e -> {
			if (VALID_KEYS_UPDATE.contains(e.getKey())) {
				modUser.set(e.getKey(), e.getValue());
			}
		});

		if (modUser.has("email")) {
			modUser.put("email", modUser.get("email").asText().trim());
		}

		modUser.put("changed_on", Instant.now().toString());
		modUser.put("changed_by", auth.getName());

//		HttpHeaders headers = new HttpHeaders();
//		headers.setContentType(MediaType.APPLICATION_JSON);
//		headers.setAccept(List.of(MediaType.APPLICATION_JSON));
//		HttpEntity<JsonNode> request = new HttpEntity<JsonNode>(modUser, headers);
//		return restTemplate.exchange(paramPath, HttpMethod.PUT, request, JsonNode.class, userId);
		final ResponseEntity<JsonNode> db = putRequest(paramPath, userId, modUser);
		return ResponseEntity.status(db.getStatusCode()).body(db.getBody());
	}

	@DeleteMapping("{username}")
	public ResponseEntity<String> deleteUser(Authentication authentication, @PathVariable String username,
			@RequestParam String rev) {
		ensureAuthenticated(authentication, USER_DELETE, username);

		// TODO Delete referenced objects
		return restTemplate.exchange(paramPath, HttpMethod.DELETE, null, String.class, username);
	}

}
