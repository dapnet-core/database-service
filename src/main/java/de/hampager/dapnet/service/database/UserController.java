package de.hampager.dapnet.service.database;

import java.net.URI;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * This class implements the users REST endpoint.
 * 
 * @author Philipp Thiel
 */
@RestController
@RequestMapping("users")
class UserController extends AbstractController {

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
	public ResponseEntity<JsonNode> getAll(Authentication authentication, @RequestParam Map<String, String> params) {
		ensureAuthenticated(authentication, USER_READ);

		URI path = buildAllDocsPath(params);
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

		JsonNode in = restTemplate.getForObject(queryPath, JsonNode.class, username);
		((ObjectNode) in).remove("password");
		return ResponseEntity.ok(in);
	}

	@DeleteMapping("{username}")
	public void deleteUser(Authentication authentication, @PathVariable String username, @RequestParam String rev) {
		ensureAuthenticated(authentication, USER_DELETE, username);

		// Return CouchDB JSON response
		// delete() is void, doesn't return a value
		// restTemplate.delete(queryPath, username);
	}

}
