package de.hampager.dapnet.service.database.controller;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

import de.hampager.dapnet.service.database.AppUser;
import de.hampager.dapnet.service.database.DbConfig;
import de.hampager.dapnet.service.database.JsonUtils;
import de.hampager.dapnet.service.database.MissingFieldException;
import de.hampager.dapnet.service.database.model.PermissionValue;

/**
 * This class implements the subscribers REST endpoint.
 *
 * @author Philipp Thiel & Ralf Wilke
 */
// TODO: UNTESTED
@CrossOrigin
@RestController
@RequestMapping("subscribers")
class SubscriberController extends AbstractController {

	private static final Logger logger = LoggerFactory.getLogger(SubscriberController.class);

	// TODO Define keys in List "pagers"

	private static final Set<String> VALID_KEYS_UPDATE = Set.of("description", "pagers", "owners");
	private static final String[] REQUIRED_KEYS_CREATE = { "_id", "pagers", "owners" };
	private static final String SUBSCRIBER_LIST = "subscriber.list";
	private static final String SUBSCRIBER_READ = "subscriber.read";
	private static final String SUBSCRIBER_UPDATE = "subscriber.update";
	private static final String SUBSCRIBER_CREATE = "subscriber.create";
	private static final String SUBSCRIBER_DELETE = "subscriber.delete";
	private final String namesPath;
	private final String groupsPath;
	private final String descriptionPath;

	@Autowired
	public SubscriberController(DbConfig config, RestTemplateBuilder builder) {
		super(config, builder, "subscribers");
		this.namesPath = basePath.concat("_design/subscribers/_list/names/byId");
		this.groupsPath = basePath.concat("_design/subscribers/_list/groups/byGroup?group_level=5");
		this.descriptionPath = basePath.concat("_design/subscribers/_list/descriptions/descriptions");
	}

	@GetMapping
	public ResponseEntity<JsonNode> getAll(@RequestParam Map<String, String> params) {
		requirePermission(SUBSCRIBER_READ);

		URI path = buildViewPath("byId", params);
		JsonNode in = restTemplate.getForObject(path, JsonNode.class);
		ObjectNode out = mapper.createObjectNode();

		out.put("total_rows", in.get("total_rows").asInt());
		ArrayNode rows = out.putArray("rows");
		for (JsonNode n : in.get("rows")) {
			JsonNode doc = n.get("doc");
			rows.add(doc);
		}

		return ResponseEntity.ok(out);
	}

	// TODO: Add view to CouchDB ?
	@GetMapping("_names")
	public ResponseEntity<JsonNode> getNames() {
		requirePermission(SUBSCRIBER_LIST);

		JsonNode in = restTemplate.getForObject(namesPath, JsonNode.class);
		return ResponseEntity.ok(in);
	}

	@GetMapping("_groups")
	public ResponseEntity<JsonNode> getGroups() {
		requirePermission(SUBSCRIBER_LIST);

		JsonNode in = restTemplate.getForObject(groupsPath, JsonNode.class);
		return ResponseEntity.ok(in);
	}

	// TODO: Add view to CouchDB
	@GetMapping("_descriptions")
	public ResponseEntity<JsonNode> getDescription() {
		requirePermission(SUBSCRIBER_LIST);

		JsonNode in = restTemplate.getForObject(descriptionPath, JsonNode.class);
		return ResponseEntity.ok(in);
	}

	// TODO: Add view to CouchDB
	@GetMapping("_view/byRIC")
	public ResponseEntity<JsonNode> getbyRIC(@RequestParam Map<String, String> params) {
		requirePermission(SUBSCRIBER_READ);

		URI path = buildViewPath("byRIC", params);
		JsonNode in = restTemplate.getForObject(path, JsonNode.class);
		ObjectNode out = mapper.createObjectNode();

		out.put("total_rows", in.get("total_rows").asInt());
		ArrayNode rows = out.putArray("rows");
		for (JsonNode n : in.get("rows")) {
			JsonNode doc = n.get("doc");
			rows.add(doc);
		}

		return ResponseEntity.ok(out);
	}

	@GetMapping("{name}")
	public ResponseEntity<JsonNode> getSubscriber(@PathVariable String subscriber) {
		final AppUser appUser = getCurrentUser();
		final PermissionValue permission = appUser.getPermissions().getOrDefault(SUBSCRIBER_READ, PermissionValue.NONE);
		if (permission == PermissionValue.NONE || permission == PermissionValue.LIMITED) {
			throw new HttpServerErrorException(HttpStatus.FORBIDDEN);
		}

		JsonNode in = restTemplate.getForObject(paramPath, JsonNode.class, subscriber);
		if (permission == PermissionValue.ALL
				|| (permission == PermissionValue.IF_OWNER && JsonUtils.isOwner(in, appUser.getUsername()))) {
			return ResponseEntity.ok(in);
		} else {
			throw new HttpServerErrorException(HttpStatus.FORBIDDEN);
		}
	}

	@PutMapping
	public ResponseEntity<JsonNode> putSubscriber(@RequestBody JsonNode subscriber) {
		if (subscriber.has("_rev")) {
			return updateSubscriber(subscriber);
		} else {
			return createSubscriber(subscriber);
		}
	}

	// UNTESTED
	private ResponseEntity<JsonNode> createSubscriber(JsonNode subscriber) {
		requirePermission(SUBSCRIBER_CREATE, PermissionValue.ALL);
		final AppUser appUser = getCurrentUser();

		try {
			JsonUtils.checkRequiredFields(subscriber, REQUIRED_KEYS_CREATE);
		} catch (MissingFieldException ex) {
			throw new HttpServerErrorException(HttpStatus.BAD_REQUEST, ex.getMessage());
		}

		final ObjectNode modSubscriber = (ObjectNode) JsonUtils.trimValues(subscriber);
		final String subsriberId = modSubscriber.get("_id").asText().toLowerCase();
		modSubscriber.put("_id", subsriberId);

		final String ts = Instant.now().toString();
		modSubscriber.put("created_on", ts);
		modSubscriber.put("created_by", appUser.getUsername());
		modSubscriber.put("changed_on", ts);
		modSubscriber.put("changed_by", appUser.getUsername());

		final ResponseEntity<JsonNode> db = performPut(subsriberId, modSubscriber);
		if (db.getStatusCode() == HttpStatus.CREATED) {
			final URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("{id}")
					.buildAndExpand(subsriberId).toUri();
			return ResponseEntity.created(location).body(db.getBody());
		} else {
			return ResponseEntity.status(db.getStatusCode()).body(db.getBody());
		}
	}

	// UNTESTED
	private ResponseEntity<JsonNode> updateSubscriber(JsonNode subscriberUpdate) {
		final PermissionValue permission = requirePermission(SUBSCRIBER_UPDATE, PermissionValue.ALL,
				PermissionValue.IF_OWNER);
		final AppUser appUser = getCurrentUser();

		subscriberUpdate = JsonUtils.trimValues(subscriberUpdate);

		final String subscriberId = subscriberUpdate.get("_id").asText().toLowerCase();
		final JsonNode oldSubscriber = restTemplate.getForObject(paramPath, JsonNode.class, subscriberId);
		if (permission == PermissionValue.IF_OWNER && !JsonUtils.isOwner(oldSubscriber, appUser.getUsername())) {
			throw new HttpServerErrorException(HttpStatus.FORBIDDEN);
		}

		ObjectNode modSubscriber;
		try {
			modSubscriber = (ObjectNode) oldSubscriber;
		} catch (ClassCastException ex) {
			logger.error("Failed to cast JsonNode to ObjectNode");
			throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
		}

		subscriberUpdate.fields().forEachRemaining(e -> {
			if (VALID_KEYS_UPDATE.contains(e.getKey())) {
				modSubscriber.set(e.getKey(), e.getValue());
			}
		});

		modSubscriber.put("changed_on", Instant.now().toString());
		modSubscriber.put("changed_by", appUser.getUsername());

		final ResponseEntity<JsonNode> db = performPut(subscriberId, modSubscriber);
		return ResponseEntity.status(db.getStatusCode()).body(db.getBody());
	}

	// UNTESTED
	@DeleteMapping("{name}")
	public ResponseEntity<JsonNode> deleteSubscriber(@PathVariable String name, @RequestParam String revision) {
		final AppUser user = getCurrentUser();
		final PermissionValue permission = user.getPermissions().getOrDefault(SUBSCRIBER_DELETE, PermissionValue.NONE);

		boolean canDelete = permission == PermissionValue.ALL;
		if (permission == PermissionValue.IF_OWNER) {
			// TODO Handle revision
			final JsonNode oldRubric = restTemplate.getForObject(paramPath, JsonNode.class, name);
			canDelete = JsonUtils.isOwner(oldRubric, user.getUsername());
		}

		if (!canDelete) {
			throw new HttpServerErrorException(HttpStatus.FORBIDDEN);
		}

		// TODO Delete referenced objects
		final ResponseEntity<JsonNode> db = performDelete(name, revision);
		return ResponseEntity.status(db.getStatusCode()).body(db.getBody());
	}
}
