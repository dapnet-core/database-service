package de.hampager.dapnet.service.database.controller;

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

@RestController
@RequestMapping("nodes")
public class NodeController extends AbstractController {

	private static final Logger logger = LoggerFactory.getLogger(NodeController.class);
	private static final Set<String> VALID_KEYS_UPDATE = Set.of("auth_key", "coordinates", "hamcloud", "owners",
			"description");
	private static final String[] REQUIRED_KEYS_CREATE = { "_id", "auth_key", "coordinates", "hamcloud", "owners" };
	private static final String NODE_LIST = "node.list";
	private static final String NODE_READ = "node.read";
	private static final String NODE_UPDATE = "node.update";
	private static final String NODE_CREATE = "node.create";
	private static final String NODE_DELETE = "node.delete";
	private final String namePath;
	private final String descriptionPath;

	@Autowired
	public NodeController(DbConfig config, RestTemplateBuilder builder) {
		super(config, builder, "nodes");

		this.namePath = basePath.concat("_design/nodes/_list/names/byId");
		this.descriptionPath = basePath.concat("_design/nodes/_list/descriptions/descriptions");
	}

	@GetMapping
	public ResponseEntity<JsonNode> getAll(@RequestParam Map<String, String> params) {
		final PermissionValue permission = requirePermission(NODE_READ);
		final boolean hideFields = permission != PermissionValue.ALL;

		URI path = buildViewPath("byId", params);
		JsonNode in = restTemplate.getForObject(path, JsonNode.class);
		ObjectNode out = mapper.createObjectNode();

		out.put("total_rows", in.get("total_rows").asInt());
		ArrayNode rows = out.putArray("rows");
		for (JsonNode n : in.get("rows")) {
			JsonNode doc = n.get("doc");
			if (hideFields) {
				((ObjectNode) doc).remove("auth_key");
			}
			rows.add(doc);
		}

		return ResponseEntity.ok(out);
	}

	@GetMapping("_names")
	public ResponseEntity<JsonNode> getNodenames() {
		requirePermission(NODE_LIST, PermissionValue.ALL);

		JsonNode in = restTemplate.getForObject(namePath, JsonNode.class);
		return ResponseEntity.ok(in);
	}

	@GetMapping("_descriptions")
	public ResponseEntity<JsonNode> getNodenamesDescription() {
		requirePermission(NODE_LIST, PermissionValue.ALL);

		JsonNode in = restTemplate.getForObject(descriptionPath, JsonNode.class);
		return ResponseEntity.ok(in);
	}

	@GetMapping("{nodename}")
	public ResponseEntity<JsonNode> getNode(@PathVariable String nodename) {
		final AppUser appUser = getCurrentUser();
		final PermissionValue permission = appUser.getPermissions().getOrDefault(NODE_READ, PermissionValue.NONE);
		if (permission == PermissionValue.NONE) {
			throw new HttpServerErrorException(HttpStatus.FORBIDDEN);
		}

		JsonNode in = restTemplate.getForObject(paramPath, JsonNode.class, nodename);
		if ((permission == PermissionValue.IF_OWNER && !JsonUtils.isOwner(in, appUser.getUsername()))
				|| permission != PermissionValue.ALL) {
			((ObjectNode) in).remove("auth_key");
		}

		return ResponseEntity.ok(in);
	}

	@PutMapping
	public ResponseEntity<JsonNode> putNode(@RequestBody JsonNode node) {
		if (node.has("_rev")) {
			return updateNode(node);
		} else {
			return createNode(node);
		}
	}

	private ResponseEntity<JsonNode> createNode(JsonNode node) {
		requirePermission(NODE_CREATE, PermissionValue.ALL);
		final AppUser appUser = getCurrentUser();

		try {
			JsonUtils.checkRequiredFields(node, REQUIRED_KEYS_CREATE);
		} catch (MissingFieldException ex) {
			throw new HttpServerErrorException(HttpStatus.BAD_REQUEST, ex.getMessage());
		}

		node = JsonUtils.trimValues(node);

		// Check if owners field is populated
		final JsonNode ownersNode = node.get("owners");
		if (!ownersNode.isArray() || !ownersNode.elements().hasNext()) {
			final String nodeId = node.get("_id").asText();
			throw new HttpServerErrorException(HttpStatus.BAD_REQUEST,
					"Owners list is empty or missing for node id " + nodeId);
		}

		ObjectNode modNode;
		try {
			modNode = (ObjectNode) node;
		} catch (ClassCastException ex) {
			logger.error("Failed to cast JsonNode to ObjectNode");
			throw new HttpServerErrorException(HttpStatus.BAD_REQUEST);
		}

		modNode.put("_id", modNode.get("_id").asText().toLowerCase());

		final String ts = Instant.now().toString();
		modNode.put("created_on", ts);
		modNode.put("created_by", appUser.getUsername());
		modNode.put("changed_on", ts);
		modNode.put("changed_by", appUser.getUsername());

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setAccept(List.of(MediaType.APPLICATION_JSON));
		HttpEntity<JsonNode> request = new HttpEntity<JsonNode>(modNode, headers);
		return restTemplate.exchange(paramPath, HttpMethod.PUT, request, JsonNode.class, modNode.get("_id").asText());
	}

	private ResponseEntity<JsonNode> updateNode(JsonNode nodeUpdate) {
		final PermissionValue permission = requirePermission(NODE_UPDATE, PermissionValue.ALL,
				PermissionValue.IF_OWNER);
		final AppUser appUser = getCurrentUser();

		nodeUpdate = JsonUtils.trimValues(nodeUpdate);

		final String nodeId = nodeUpdate.get("_id").asText();

		final JsonNode ownersNode = nodeUpdate.get("owners");
		if (ownersNode != null && (!ownersNode.isArray() || !ownersNode.elements().hasNext())) {
			throw new HttpServerErrorException(HttpStatus.BAD_REQUEST, "Owners list is empty " + nodeId);
		}

		final JsonNode oldNode = restTemplate.getForObject(paramPath, JsonNode.class, nodeId);
		if (permission == PermissionValue.IF_OWNER && !JsonUtils.isOwner(oldNode, appUser.getUsername())) {
			throw new HttpServerErrorException(HttpStatus.FORBIDDEN);
		}

		ObjectNode modNode;
		try {
			modNode = (ObjectNode) oldNode;
		} catch (ClassCastException ex) {
			logger.error("Failed to cast JsonNode to ObjectNode");
			throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
		}

		nodeUpdate.fields().forEachRemaining(e -> {
			if (VALID_KEYS_UPDATE.contains(e.getKey())) {
				modNode.set(e.getKey(), e.getValue());
			}
		});

		modNode.put("changed_on", Instant.now().toString());
		modNode.put("changed_by", appUser.getUsername());

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setAccept(List.of(MediaType.APPLICATION_JSON));
		HttpEntity<JsonNode> request = new HttpEntity<JsonNode>(modNode, headers);
		return restTemplate.exchange(paramPath, HttpMethod.PUT, request, JsonNode.class, nodeId);
	}

	@DeleteMapping("{nodename}")
	public ResponseEntity<String> deleteNode(Authentication authentication, @PathVariable String nodename,
			@RequestParam String rev) {
		final AppUser user = (AppUser) authentication.getPrincipal();
		final PermissionValue permission = user.getPermissions().getOrDefault(NODE_DELETE, PermissionValue.NONE);
		boolean canDelete = permission == PermissionValue.ALL;
		if (permission == PermissionValue.IF_OWNER) {
			final JsonNode oldNode = restTemplate.getForObject(paramPath, JsonNode.class, nodename);
			canDelete = JsonUtils.isOwner(oldNode, authentication.getName());
		}

		if (!canDelete) {
			throw new HttpServerErrorException(HttpStatus.FORBIDDEN);
		}

		// TODO Delete referenced objects
		return restTemplate.exchange(paramPath, HttpMethod.DELETE, null, String.class, nodename);
	}

}
