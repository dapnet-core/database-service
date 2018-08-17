package de.hampager.dapnet.service.database;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@RestController
@RequestMapping("users")
class UserController extends AbstractController {

	@Autowired
	public UserController(DbConfig config, RestTemplateBuilder builder) {
		super(config, builder, "users");
	}

	@GetMapping
	public ResponseEntity<JsonNode> getAll() {
		ensureAuthenticated(AuthPermission.USER_READ_LIST);

		JsonNode in = restTemplate.getForObject(getPath("_all_docs?include_docs=true"), JsonNode.class);
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
	public ResponseEntity<JsonNode> getUsernames() {
		ensureAuthenticated(AuthPermission.USER_LIST);

		JsonNode in = restTemplate.getForObject(getPath("_design/users/_list/usernames/_all_docs"), JsonNode.class);
		return ResponseEntity.ok(in);
	}

	@GetMapping("{username}")
	public ResponseEntity<JsonNode> getUser(@PathVariable String username) {
		ensureAuthenticated(AuthPermission.USER_READ, username);

		JsonNode in = restTemplate.getForObject(getPath(username), JsonNode.class);
		((ObjectNode) in).remove("password");
		return ResponseEntity.ok(in);
	}

}
