package de.hampager.dapnet.service.database;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Arrays;

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

	@GetMapping()
	public ResponseEntity<JsonNode> getAll(Authentication authentication, HttpServletRequest request) {
		ensureAuthenticated(authentication, USER_READ);

		String[] allowed_params = {"limit", "skip", "startkey", "endkey"};
		MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
		params.add("limit", "20");
		request.getParameterMap().entrySet().stream().forEach(param -> {
			if (Arrays.asList(allowed_params).contains(param.getKey())) {
				params.put(param.getKey(), Arrays.asList(param.getValue()));
			}
		});

		URI allDocsUri = UriComponentsBuilder.fromUriString(allDocsPath).queryParams(params).build().toUri();

		JsonNode in = restTemplate.getForObject(allDocsUri, JsonNode.class);
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
		// restTemplate.delete(queryPath, username);
	}
}
