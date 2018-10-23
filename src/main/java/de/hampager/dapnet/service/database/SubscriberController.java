package de.hampager.dapnet.service.database;

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
	public ResponseEntity<JsonNode> getAll(Authentication authentication, @RequestParam Map<String, String> params) {
		ensureAuthenticated(authentication, SUBSCRIBER_READ);

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
	public ResponseEntity<JsonNode> getNames(Authentication authentication) {
		ensureAuthenticated(authentication, SUBSCRIBER_LIST);

		JsonNode in = restTemplate.getForObject(namesPath, JsonNode.class);
		return ResponseEntity.ok(in);
	}

	@GetMapping("_groups")
	public ResponseEntity<JsonNode> getGroups(Authentication authentication) {
		ensureAuthenticated(authentication, SUBSCRIBER_LIST);

		JsonNode in = restTemplate.getForObject(groupsPath, JsonNode.class);
		return ResponseEntity.ok(in);
	}

	// TODO: Add view to CouchDB
	@GetMapping("_descriptions")
	public ResponseEntity<JsonNode> getDescription(Authentication authentication) {
		ensureAuthenticated(authentication, SUBSCRIBER_LIST);

		JsonNode in = restTemplate.getForObject(descriptionPath, JsonNode.class);
		return ResponseEntity.ok(in);
	}

	// TODO: Add view to CouchDB
	@GetMapping("_view/byRIC")
	public ResponseEntity<JsonNode> getbyRIC(Authentication authentication, @RequestParam Map<String, String> params) {
		ensureAuthenticated(authentication, SUBSCRIBER_READ);

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
	public ResponseEntity<JsonNode> getSubscriber(Authentication authentication, @PathVariable String subscriber) {
		ensureAuthenticated(authentication, SUBSCRIBER_READ, subscriber);

		JsonNode in = restTemplate.getForObject(paramPath, JsonNode.class, subscriber);
		return ResponseEntity.ok(in);
	}

	@PutMapping
	public ResponseEntity<JsonNode> putSubscriber(Authentication authentication, @RequestBody JsonNode subscriber) {
		if (subscriber.has("_rev")) {
			return updateSubscriber(authentication, subscriber);
		} else {
			return createSubscriber(authentication, subscriber);
		}
	}

	// UNTESTED
	private ResponseEntity<JsonNode> createSubscriber(Authentication auth, JsonNode subscriber) {
		ensureAuthenticated(auth, SUBSCRIBER_CREATE);

		try {
			JsonUtils.validateRequiredFields(subscriber, REQUIRED_KEYS_CREATE);
		} catch (MissingFieldException ex) {
			throw new HttpServerErrorException(HttpStatus.BAD_REQUEST, ex.getMessage());
		}

		ObjectNode modSubscriber;
		try {
			modSubscriber = (ObjectNode) subscriber;
		} catch (ClassCastException ex) {
			logger.error("Failed to cast JsonNode to ObjectNode");
			throw new HttpServerErrorException(HttpStatus.BAD_REQUEST);
		}

		final String subsriberId = modSubscriber.get("_id").asText().trim().toLowerCase();
		modSubscriber.put("_id", subsriberId);
		// Convert _id to lowercase and remove all whitespaces

		final String ts = Instant.now().toString();
		modSubscriber.put("created_on", ts);
		modSubscriber.put("created_by", auth.getName());
		modSubscriber.put("changed_on", ts);
		modSubscriber.put("changed_by", auth.getName());

		final ResponseEntity<JsonNode> db = performPut(paramPath, subsriberId, modSubscriber);
		if (db.getStatusCode() == HttpStatus.CREATED) {
			final URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("{id}")
					.buildAndExpand(subsriberId).toUri();
			return ResponseEntity.created(location).body(db.getBody());
		} else {
			return ResponseEntity.status(db.getStatusCode()).body(db.getBody());
		}
	}

	// UNTESTED
	private ResponseEntity<JsonNode> updateSubscriber(Authentication auth, JsonNode subscriberUpdate) {
		ensureAuthenticated(auth, SUBSCRIBER_UPDATE, auth.getName());

		final String subscriberId = subscriberUpdate.get("_id").asText().trim().toLowerCase();
		JsonNode oldSubscriber = restTemplate.getForObject(paramPath, JsonNode.class, subscriberId);
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
		modSubscriber.put("changed_by", auth.getName());

		final ResponseEntity<JsonNode> db = performPut(paramPath, subscriberId, modSubscriber);
		return ResponseEntity.status(db.getStatusCode()).body(db.getBody());
	}

	// UNTESTED
	@DeleteMapping("{name}")
	public ResponseEntity<JsonNode> deleteSubscriber(Authentication authentication, @PathVariable String name,
			@RequestParam String rev) {
		ensureAuthenticated(authentication, SUBSCRIBER_DELETE, name);

		// TODO Delete referenced objects
		final ResponseEntity<JsonNode> db = performDelete(paramPath, name);
		return ResponseEntity.status(db.getStatusCode()).body(db.getBody());
	}
}
