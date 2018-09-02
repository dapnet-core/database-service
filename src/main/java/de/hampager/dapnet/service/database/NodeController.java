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

@RestController
@RequestMapping("nodes")
class NodeController extends AbstractController {

	private static final Logger logger = LoggerFactory.getLogger(NodeController.class);
	private static final Set<String> VALID_KEYS_UPDATE = Set.of("auth_key", "coordinates", "hamcloud", "owners",
			"description");
	private static final String[] REQUIRED_KEYS_CREATE = { "_id", "auth_key", "coordinates", "hamcloud", "owners" };
	private static final String NODE_LIST = "node.list";
	private static final String NODE_READ = "node.read";
	private static final String NODE_READ_FULL = "node.read_full";
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
	public ResponseEntity<JsonNode> getAll(Authentication authentication, @RequestParam Map<String, String> params) {
		boolean readFullAllowed = isAuthenticated(authentication, NODE_READ_FULL);
		if (!readFullAllowed) {
			ensureAuthenticated(authentication, NODE_READ);
		}

		URI path = buildViewPath("byId", params);
		JsonNode in = restTemplate.getForObject(path, JsonNode.class);
		ObjectNode out = mapper.createObjectNode();

		out.put("total_rows", in.get("total_rows").asInt());
		ArrayNode rows = out.putArray("rows");
		for (JsonNode n : in.get("rows")) {
			JsonNode doc = n.get("doc");
			if (!readFullAllowed) {
				((ObjectNode) doc).remove("auth_key");
			}
			rows.add(doc);
		}

		return ResponseEntity.ok(out);
	}

	@GetMapping("_names")
	public ResponseEntity<JsonNode> getNodenames(Authentication authentication) {
		ensureAuthenticated(authentication, NODE_LIST);

		JsonNode in = restTemplate.getForObject(namePath, JsonNode.class);
		return ResponseEntity.ok(in);
	}

	@GetMapping("_descriptions")
	public ResponseEntity<JsonNode> getNodenamesDescription(Authentication authentication) {
		ensureAuthenticated(authentication, NODE_LIST);

		JsonNode in = restTemplate.getForObject(descriptionPath, JsonNode.class);
		return ResponseEntity.ok(in);
	}

	@GetMapping("{nodename}")
	public ResponseEntity<JsonNode> getNode(Authentication authentication, @PathVariable String nodename) {
		boolean readFullAllowed = isAuthenticated(authentication, NODE_READ_FULL, nodename);
		if (!readFullAllowed) {
			ensureAuthenticated(authentication, NODE_READ, nodename);
		}

		JsonNode in = restTemplate.getForObject(paramPath, JsonNode.class, nodename);
		if (!readFullAllowed) {
			((ObjectNode) in).remove("auth_key");
		}

		return ResponseEntity.ok(in);
	}

	@PutMapping
	public ResponseEntity<JsonNode> putNode(Authentication authentication, @RequestBody JsonNode node) {
		if (node.has("_rev")) {
			return updateNode(authentication, node);
		} else {
			return createNode(authentication, node);
		}
	}

	private ResponseEntity<JsonNode> createNode(Authentication auth, JsonNode node) {
		ensureAuthenticated(auth, NODE_CREATE);

		try {
			JsonUtils.validateRequiredFields(node, REQUIRED_KEYS_CREATE);
		} catch (MissingFieldException ex) {
			throw new HttpServerErrorException(HttpStatus.BAD_REQUEST, ex.getMessage());
		}

		// Check if owners field is populated
		JsonNode ownersNode = node.get("owners");
		if (!ownersNode.isArray() || !ownersNode.elements().hasNext()) {
			String nodeId = node.get("_id").asText();
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

        // Remove whitespaces from owner array entries
        // TODO: Make it work
		//modNode.put(SanitizeUtils.removeWhiteSpacefromArray(modNode.get("owners"));


		final String ts = Instant.now().toString();
		modNode.put("created_on", ts);
		modNode.put("created_by", auth.getName());
		modNode.put("changed_on", ts);
		modNode.put("changed_by", auth.getName());

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setAccept(List.of(MediaType.APPLICATION_JSON));
		HttpEntity<JsonNode> request = new HttpEntity<JsonNode>(modNode, headers);
		return restTemplate.exchange(paramPath, HttpMethod.PUT, request, JsonNode.class, modNode.get("_id").asText());
	}

	private ResponseEntity<JsonNode> updateNode(Authentication auth, JsonNode nodeUpdate) {
		ensureAuthenticated(auth, NODE_UPDATE, auth.getName());

		JsonNode ownersNode = nodeUpdate.get("owners");
		if (ownersNode == null || !ownersNode.isArray() || !ownersNode.elements().hasNext()) {
			String nodeId = nodeUpdate.get("_id").asText();
			throw new HttpServerErrorException(HttpStatus.BAD_REQUEST, "Owners list is empty " + nodeId);
		}

		final String nodeId = nodeUpdate.get("_id").asText();

		JsonNode oldNode = restTemplate.getForObject(paramPath, JsonNode.class, nodeId);
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
		modNode.put("changed_by", auth.getName());

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setAccept(List.of(MediaType.APPLICATION_JSON));
		HttpEntity<JsonNode> request = new HttpEntity<JsonNode>(modNode, headers);
		return restTemplate.exchange(paramPath, HttpMethod.PUT, request, JsonNode.class, nodeId);
	}

	@DeleteMapping("{nodename}")
	public ResponseEntity<String> deleteNode(Authentication authentication, @PathVariable String nodename,
			@RequestParam String rev) {
		ensureAuthenticated(authentication, NODE_DELETE, nodename);

		// TODO Delete referenced objects
		return restTemplate.exchange(paramPath, HttpMethod.DELETE, null, String.class, nodename);
	}

}
