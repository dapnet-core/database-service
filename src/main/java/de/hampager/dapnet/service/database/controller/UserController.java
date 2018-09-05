package de.hampager.dapnet.service.database.controller;

import java.net.URI;
import java.time.Instant;
import java.util.Iterator;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpServerErrorException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.hampager.dapnet.service.database.AppUser;
import de.hampager.dapnet.service.database.DbConfig;
import de.hampager.dapnet.service.database.JsonUtils;
import de.hampager.dapnet.service.database.MissingFieldException;
import de.hampager.dapnet.service.database.model.PermissionValue;

/**
 * This class implements the users REST endpoint.
 * 
 * @author Philipp Thiel
 */
@RestController
@RequestMapping("users")
public class UserController extends AbstractController {

	private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);
	private static final Set<String> KEYS_GET_LIMITED = Set.of("_id", "roles", "enabled");
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

		this.usernamesPath = basePath.concat("_design/users/_list/usernames/_all_docs");
	}

	@GetMapping
	public ResponseEntity<JsonNode> getAll(@RequestParam Map<String, String> params) {
		final PermissionValue permission = requirePermission(USER_READ);
		final AppUser appUser = getCurrentUser();

		final URI path = buildViewPath("byId", params);
		final JsonNode in = restTemplate.getForObject(path, JsonNode.class);
		final ObjectNode out = mapper.createObjectNode();

		out.put("total_rows", in.get("total_rows").asInt());
		ArrayNode rows = out.putArray("rows");
		for (JsonNode n : in.get("rows")) {
			final ObjectNode doc = (ObjectNode) n.get("doc");
			doc.remove("password");

			if (permission != PermissionValue.ALL
					&& !doc.get("_id").asText("").equalsIgnoreCase(appUser.getUsername())) {
				JsonUtils.keepFields(doc, KEYS_GET_LIMITED);
			}

			rows.add(doc);
		}

		return ResponseEntity.ok(out);

	}

	@GetMapping("_usernames")
	public ResponseEntity<JsonNode> getUsernames() {
		requirePermission(USER_LIST, PermissionValue.ALL);

		JsonNode in = restTemplate.getForObject(usernamesPath, JsonNode.class);
		return ResponseEntity.ok(in);
	}

	@GetMapping("{username}")
	public ResponseEntity<JsonNode> getUser(@PathVariable String username) {
		final PermissionValue permission = requirePermission(USER_READ);
		final AppUser user = getCurrentUser();

		final ObjectNode in = restTemplate.getForObject(paramPath, ObjectNode.class, username);
		in.remove("password");
		if (permission != PermissionValue.ALL && !in.get("_id").asText("").equalsIgnoreCase(user.getUsername())) {
			JsonUtils.keepFields(in, KEYS_GET_LIMITED);
		}

		return ResponseEntity.ok(in);
	}

	@PutMapping
	public ResponseEntity<JsonNode> putUser(@RequestBody JsonNode user) {
		if (user.has("_rev")) {
			return updateUser(user);
		} else {
			return createUser(user);
		}
	}

	private ResponseEntity<JsonNode> createUser(JsonNode user) {
		requirePermission(USER_CREATE, PermissionValue.ALL);
		final AppUser appUser = getCurrentUser();

		try {
			JsonUtils.checkRequiredFields(user, REQUIRED_KEYS_CREATE);
		} catch (MissingFieldException ex) {
			throw new HttpServerErrorException(HttpStatus.BAD_REQUEST, ex.getMessage());
		}

		final ObjectNode modUser = (ObjectNode) JsonUtils.trimValues(user);

		modUser.put("_id", modUser.get("_id").asText().toLowerCase());

		final String ts = Instant.now().toString();
		modUser.put("created_on", ts);
		modUser.put("created_by", appUser.getUsername());
		modUser.put("changed_on", ts);
		modUser.put("changed_by", appUser.getUsername());

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setAccept(List.of(MediaType.APPLICATION_JSON));
		HttpEntity<JsonNode> request = new HttpEntity<JsonNode>(modUser, headers);
		return restTemplate.exchange(paramPath, HttpMethod.PUT, request, JsonNode.class, modUser.get("_id").asText());
	}

	private ResponseEntity<JsonNode> updateUser(JsonNode userUpdate) {
		userUpdate = JsonUtils.trimValues(userUpdate);
		final AppUser appUser = getCurrentUser();

		final String userId = userUpdate.get("_id").asText();
		if (isOnlyRoleUpdate(userUpdate)) {
			requireAdminOrOwner(USER_CHANGE_ROLE, userId);
		} else {
			requireAdminOrOwner(USER_UPDATE, userId);
		}

		final ObjectNode oldUser = restTemplate.getForObject(paramPath, ObjectNode.class, userId);

		userUpdate.fields().forEachRemaining(e -> {
			if (VALID_KEYS_UPDATE.contains(e.getKey())) {
				oldUser.set(e.getKey(), e.getValue());
			}
		});

		oldUser.put("updated_on", Instant.now().toString());
		oldUser.put("updated_by", appUser.getUsername());

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setAccept(List.of(MediaType.APPLICATION_JSON));
		HttpEntity<JsonNode> request = new HttpEntity<JsonNode>(oldUser, headers);
		return restTemplate.exchange(paramPath, HttpMethod.PUT, request, JsonNode.class, userId);
	}

	@DeleteMapping("{username}")
	public ResponseEntity<String> deleteUser(@PathVariable String username, @RequestParam String rev) {
		requireAdminOrOwner(USER_DELETE, username);
		// TODO Delete referenced objects
		return restTemplate.exchange(paramPath, HttpMethod.DELETE, null, String.class, username);
	}

	private static boolean isOnlyRoleUpdate(JsonNode node) {
		final Iterator<String> it = node.fieldNames();
		while (it.hasNext()) {
			if (!it.next().equalsIgnoreCase("roles")) {
				return false;
			}
		}

		return true;
	}

}
