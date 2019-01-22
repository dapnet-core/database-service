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
import org.springframework.web.bind.annotation.*;
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
 * This class implements the rubrics REST endpoint.
 *
 * @author Philipp Thiel & Ralf Wilke
 */
@CrossOrigin
@RestController
@RequestMapping("rubrics")
class RubricController extends AbstractController {

	private static final Logger logger = LoggerFactory.getLogger(RubricController.class);
	private static final Set<String> VALID_KEYS_UPDATE = Set.of("number", "description", "label",
			"transmitter_groups", "transmitters", "cyclic_transmit", "cyclic_transmit_interval",
			"owners", "function", "default_expiration", "default_priority", "content", "data",
			"expires_on", "priority");
	private static final String[] REQUIRED_KEYS_CREATE = {"_id", "number", "label", "description",
			"transmitter_groups", "transmitters", "owners", "default_priority", "default_expiration"};
	private static final String[] REQUIRED_KEYS_POST_CONTENT_FIRST = {"_id", "data"};
	private static final String[] REQUIRED_KEYS_POST_CONTENT_RANDOM = {"_id", "data", "slot"};
	private static final String RUBRIC_LIST = "rubric.list";
	private static final String RUBRIC_READ = "rubric.read";
	private static final String RUBRIC_UPDATE = "rubric.update";
	private static final String RUBRIC_CREATE = "rubric.create";
	private static final String RUBRIC_DELETE = "rubric.delete";
	private static final int RUBRIC_MAX_NUMBER = 95;
	private final String namesPath;
	private final String descriptionPath;
	private final String labelPath;
	private final String fullMetaPath;

	@Autowired
	public RubricController(DbConfig config, RestTemplateBuilder builder) {
		super(config, builder, "rubrics");
		this.namesPath = basePath.concat("_design/rubrics/_list/names/byId?reduce=false");
		this.descriptionPath = basePath.concat("_design/rubrics/_list/descriptions/descriptions?reduce=false");
		this.labelPath = basePath.concat("_design/rubrics/_list/labels/labels?reduce=false");
		this.fullMetaPath = basePath.concat("_design/rubrics/_list/fullmeta/fullmeta?reduce=false");
	}

	@GetMapping
	public ResponseEntity<JsonNode> getAll(@RequestParam Map<String, String> params) {
		requirePermission(RUBRIC_READ);

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

	@GetMapping("_names")
	public ResponseEntity<JsonNode> getNames() {
		requirePermission(RUBRIC_LIST);

		JsonNode in = restTemplate.getForObject(namesPath, JsonNode.class);
		return ResponseEntity.ok(in);
	}

	@GetMapping("_descriptions")
	public ResponseEntity<JsonNode> getDescription() {
		requirePermission(RUBRIC_LIST);

		JsonNode in = restTemplate.getForObject(descriptionPath, JsonNode.class);
		return ResponseEntity.ok(in);
	}

	@GetMapping("_labels")
	public ResponseEntity<JsonNode> getLabels() {
		requirePermission(RUBRIC_LIST);

		JsonNode in = restTemplate.getForObject(labelPath, JsonNode.class);
		return ResponseEntity.ok(in);
	}

	@GetMapping("_fullmeta")
	public ResponseEntity<JsonNode> getFullMeta() {
		requirePermission(RUBRIC_LIST);

		JsonNode in = restTemplate.getForObject(fullMetaPath, JsonNode.class);
		return ResponseEntity.ok(in);
	}

	@GetMapping("_view/byNumber")
	public ResponseEntity<JsonNode> getbyNumber(@RequestParam Map<String, String> params) {
		requirePermission(RUBRIC_READ);

		URI path = buildViewPath("byNumber", params);
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

	@GetMapping("_view/byTransmitter")
	public ResponseEntity<JsonNode> getbyTransmitter(@RequestParam Map<String, String> params) {
		requirePermission(RUBRIC_READ);

		URI path = buildViewPath("byTransmitter", params);
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

	@GetMapping("_view/byTransmitterGroup")
	public ResponseEntity<JsonNode> getbyTransmitterGroup(@RequestParam Map<String, String> params) {
		requirePermission(RUBRIC_READ);

		URI path = buildViewPath("byTransmitterGroup", params);
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

	@GetMapping("{rubricname}")
	public ResponseEntity<JsonNode> getRubric(@PathVariable String rubricname) {
		final AppUser appUser = getCurrentUser();
		final PermissionValue permission = appUser.getPermissions().getOrDefault(RUBRIC_READ, PermissionValue.NONE);
		if (permission == PermissionValue.NONE || permission == PermissionValue.LIMITED) {
			throw new HttpServerErrorException(HttpStatus.FORBIDDEN);
		}

		JsonNode in = restTemplate.getForObject(paramPath, JsonNode.class, rubricname);
		if (permission == PermissionValue.ALL
				|| (permission == PermissionValue.IF_OWNER && JsonUtils.isOwner(in, appUser.getUsername()))) {
			return ResponseEntity.ok(in);
		} else {
			throw new HttpServerErrorException(HttpStatus.FORBIDDEN);
		}
	}

	@PutMapping
	public ResponseEntity<JsonNode> putRubric(@RequestBody JsonNode rubric) {
		if (rubric.has("number")) {
			final JsonNode rubricNumber = rubric.get("number");
			if (!rubricNumber.isInt()) {
				throw new HttpServerErrorException(HttpStatus.BAD_REQUEST, "Field number is not an integer.");
			} else if (rubricNumber.asInt() > RUBRIC_MAX_NUMBER) {
				throw new HttpServerErrorException(HttpStatus.BAD_REQUEST,
						"Rubric number is greater than " + RUBRIC_MAX_NUMBER);
			}
		}

		if (rubric.has("_rev")) {
			return updateRubric(rubric);
		} else {
			return createRubric(rubric);
		}
	}

	// UNTESTED
	private ResponseEntity<JsonNode> createRubric(JsonNode rubric) {
		requirePermission(RUBRIC_CREATE, PermissionValue.ALL);
		final AppUser appUser = getCurrentUser();

		try {
			JsonUtils.checkRequiredFields(rubric, REQUIRED_KEYS_CREATE);
		} catch (MissingFieldException ex) {
			throw new HttpServerErrorException(HttpStatus.BAD_REQUEST, ex.getMessage());
		}

		final ObjectNode modRubric = (ObjectNode) JsonUtils.trimValues(rubric);
		final String rubricId = modRubric.get("_id").asText().toLowerCase();
		modRubric.put("_id", rubricId);

		final String ts = Instant.now().toString();
		modRubric.put("created_on", ts);
		modRubric.put("created_by", appUser.getUsername());
		modRubric.put("changed_on", ts);
		modRubric.put("changed_by", appUser.getUsername());

		// UNSTESTED
		// Check if optional fields for cyclic transmit are present or generate default
		// entries, or repair request
		if ((!modRubric.has("cyclic_transmit")) && (!modRubric.has("cyclic_transmit_interval"))) {
			// None given
			modRubric.put("cyclic_transmit", false);
			modRubric.put("cyclic_transmit_interval", 0);
		} else if ((!modRubric.has("cyclic_transmit")) && (modRubric.has("cyclic_transmit_interval"))) {
			// Just interval given, so set boolean flag
			modRubric.put("cyclic_transmit", true);
		} else if ((modRubric.has("cyclic_transmit")) && (!modRubric.has("cyclic_transmit_interval"))) {
			// Just boolean flag given, but no interval, disable and set interval to 0
			modRubric.put("cyclic_transmit", false);
			modRubric.put("cyclic_transmit_interval", 0);
		}

		final ResponseEntity<JsonNode> db = performPut(rubricId, modRubric);
		if (db.getStatusCode() == HttpStatus.CREATED) {
			final URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("{id}").buildAndExpand(rubricId)
					.toUri();
			return ResponseEntity.created(location).body(db.getBody());
		} else {
			return ResponseEntity.status(db.getStatusCode()).body(db.getBody());
		}
	}

	// UNTESTED
	private ResponseEntity<JsonNode> updateRubric(JsonNode rubricUpdate) {
		final PermissionValue permission = requirePermission(RUBRIC_UPDATE, PermissionValue.ALL,
				PermissionValue.IF_OWNER);
		final AppUser appUser = getCurrentUser();

		rubricUpdate = JsonUtils.trimValues(rubricUpdate);

		final String rubricId = rubricUpdate.get("_id").asText().toLowerCase();
		final JsonNode oldRubric = restTemplate.getForObject(paramPath, JsonNode.class, rubricId);
		// Test if fetch rubric contains current user in owner array
		if (permission == PermissionValue.IF_OWNER && !JsonUtils.isOwner(oldRubric, appUser.getUsername())) {
			throw new HttpServerErrorException(HttpStatus.FORBIDDEN);
		}

		ObjectNode modRubric;
		try {
			modRubric = (ObjectNode) oldRubric;
		} catch (ClassCastException ex) {
			logger.error("Failed to cast JsonNode to ObjectNode");
			throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
		}

		rubricUpdate.fields().forEachRemaining(e -> {
			if (VALID_KEYS_UPDATE.contains(e.getKey())) {
				modRubric.set(e.getKey(), e.getValue());
			}
		});

		modRubric.put("changed_on", Instant.now().toString());
		modRubric.put("changed_by", appUser.getUsername());

		final ResponseEntity<JsonNode> db = performPut(rubricId, modRubric);
		return ResponseEntity.status(db.getStatusCode()).body(db.getBody());
	}

	// Get all documents that have the current user name in the owners array
	@GetMapping("_my")
	public ResponseEntity<JsonNode> getMy() {
		requirePermission(RUBRIC_READ);

		URI path = buildOwnersViewPath(false);
		JsonNode in = restTemplate.getForObject(path, JsonNode.class);
		ObjectNode out = mapper.createObjectNode();

		// Not sure if this works always, to let's count the rows manually
		// out.put("total_rows",
		// in.get("total_rows").asInt() - in.get("offset").asInt()
		// );

		Integer total_rows = 0;

		ArrayNode rows = out.putArray("rows");
		for (JsonNode n : in.get("rows")) {
			JsonNode doc = n.get("doc");
			rows.add(doc);
			total_rows++;
		}
		out.put("total_rows", total_rows);
		return ResponseEntity.ok(out);
	}

	// Get the number of documents that have the current user name in the owners
	// array
	@GetMapping("_my_count")
	public ResponseEntity<JsonNode> getMyCount() {
		requirePermission(RUBRIC_READ);

		URI path = buildOwnersViewPath(true);
		JsonNode in = restTemplate.getForObject(path, JsonNode.class);
		ObjectNode out = mapper.createObjectNode();

		Integer total_items = 0;
		if (in.has("rows") && in.get("rows").has(0) && in.get("rows").get(0).has("value")) {
			total_items = in.get("rows").get(0).get("value").asInt();
		}
		out.put("count", total_items);
		return ResponseEntity.ok(out);
	}

	// Get the number of all documents in this database
	@GetMapping("_count")
	public ResponseEntity<JsonNode> getCount() {
		requirePermission(RUBRIC_READ);

		URI path = buildCountViewPath();
		JsonNode in = restTemplate.getForObject(path, JsonNode.class);
		ObjectNode out = mapper.createObjectNode();

		Integer total_items = 0;
		if (in.has("rows") && in.get("rows").has(0) && in.get("rows").get(0).has("value")) {
			total_items = in.get("rows").get(0).get("value").asInt();
		}
		out.put("count", total_items);
		return ResponseEntity.ok(out);
	}

	// UNTESTED
	@DeleteMapping("{name}")
	public ResponseEntity<JsonNode> deleteRubric(@PathVariable String name, @RequestParam String revision) {
		final AppUser user = getCurrentUser();
		final PermissionValue permission = user.getPermissions().getOrDefault(RUBRIC_DELETE, PermissionValue.NONE);
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

	// UNTESTED
	@PostMapping("/content/first")
	public ResponseEntity<JsonNode> postRubricContent(@RequestBody JsonNode body) {
		final PermissionValue permission = requirePermission(RUBRIC_UPDATE, PermissionValue.ALL,
				PermissionValue.IF_OWNER);
		final AppUser appUser = getCurrentUser();

		body = JsonUtils.trimValues(body);

		if (body.get("_id").asText().isEmpty()) {
			// _id empty
			throw new HttpServerErrorException(HttpStatus.BAD_REQUEST,
					"Field _id is empty, don't know which rubric you are talking about.");
		}

		if (body.get("data").asText().isEmpty()) {
			// _id empty
			throw new HttpServerErrorException(HttpStatus.BAD_REQUEST,
					"Field data is empty, so no content provided to update the rubric");
		}

		final String rubricId = body.get("_id").asText().toLowerCase();
		final JsonNode oldRubric = restTemplate.getForObject(paramPath, JsonNode.class, rubricId);
		// Test if fetch rubric contains current user in owner array
		if (permission == PermissionValue.IF_OWNER && !JsonUtils.isOwner(oldRubric, appUser.getUsername())) {
			throw new HttpServerErrorException(HttpStatus.FORBIDDEN);
		}

		// New object to contain the changes
		ObjectNode updatedRubric = mapper.createObjectNode();

		// Generate mandatory fields
		updatedRubric.put("changed_on", Instant.now().toString());
		updatedRubric.put("changed_by", appUser.getUsername());
		updatedRubric.put("_id", rubricId);
		updatedRubric.put("_rev", oldRubric.get("_rev").asText());

		ObjectNode newContent;
		try {
			newContent = (ObjectNode) oldRubric.get("content");
		} catch (ClassCastException ex) {
			logger.error("Failed to cast JsonNode to ObjectNode");
			throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
		}

		ObjectNode newContentObject = mapper.createObjectNode();
		newContentObject.put("data", body.get("data").asText());
		if (body.has("expires_on") && !body.get("expires_on").asText().isEmpty()) {
			newContentObject.put("expires_on", body.get("expires_on").asText());
		}
		if (body.has("priority") && !body.get("priority").isInt()) {
			newContentObject.put("priority", body.get("priority").asText());
		}

		// Get an Array with the current content items
		ArrayNode contentArray;
		try {
			contentArray = (ArrayNode) newContent.get("content");
		} catch (ClassCastException ex) {
			logger.error("Failed to cast JsonNode to ArrayNode");
			throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
		}

		// Add new Content to the beginning of the Array
		contentArray.insert(0, newContentObject);

		// If after adding there are more than 10 contents, delete the last one.
		if (contentArray.size() >= 10) {
			contentArray.remove(10);
		}

		// Insert the new array into the request payload to the CouchDB
		newContent.set("content", contentArray);

		updatedRubric.set("content", newContent);
		// Write to CouchDB
		final ResponseEntity<JsonNode> db = performPut(rubricId, updatedRubric);
		// Return the CouchDB response
		return ResponseEntity.status(db.getStatusCode()).body(db.getBody());

	}

	// UNTESTED
	@PostMapping("/content/{slot}")
	public ResponseEntity<JsonNode> postRubricContent(@PathVariable int slot, @RequestBody JsonNode body) {
		final PermissionValue permission = requirePermission(RUBRIC_UPDATE, PermissionValue.ALL,
				PermissionValue.IF_OWNER);
		final AppUser appUser = getCurrentUser();

		body = JsonUtils.trimValues(body);

		if (body.get("_id").asText().isEmpty()) {
			// _id empty
			throw new HttpServerErrorException(HttpStatus.BAD_REQUEST,
					"Field _id is empty, don't know which rubric you are talking about.");
		}

		if (body.get("data").asText().isEmpty()) {
			// _id empty
			throw new HttpServerErrorException(HttpStatus.BAD_REQUEST,
					"Field data is empty, so no content provided to update the rubric");
		}

		if (slot < 1 || slot > 10) {
			// slot not between 1 and 10
			throw new HttpServerErrorException(HttpStatus.BAD_REQUEST,
					"Message slot in URL is not betwenn 1 and 10.");
		}


		final String rubricId = body.get("_id").asText().toLowerCase();
		final JsonNode oldRubric = restTemplate.getForObject(paramPath, JsonNode.class, rubricId);
		// Test if fetch rubric contains current user in owner array
		if (permission == PermissionValue.IF_OWNER && !JsonUtils.isOwner(oldRubric, appUser.getUsername())) {
			throw new HttpServerErrorException(HttpStatus.FORBIDDEN);
		}

		// New object to contain the changes
		ObjectNode updatedRubric = mapper.createObjectNode();

		// Generate mandatory fields
		updatedRubric.put("changed_on", Instant.now().toString());
		updatedRubric.put("changed_by", appUser.getUsername());
		updatedRubric.put("_id", rubricId);
		updatedRubric.put("_rev", oldRubric.get("_rev").asText());

		ObjectNode newContent;
		try {
			newContent = (ObjectNode) oldRubric.get("content");
		} catch (ClassCastException ex) {
			logger.error("Failed to cast JsonNode to ObjectNode");
			throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
		}

		ObjectNode newContentObject = mapper.createObjectNode();
		newContentObject.put("data", body.get("data").asText());
		if (body.has("expires_on") && !body.get("expires_on").asText().isEmpty()) {
			newContentObject.put("expires_on", body.get("expires_on").asText());
		}
		if (body.has("priority") && !body.get("priority").isInt()) {
			newContentObject.put("priority", body.get("priority").asText());
		}

		// Get an Array with the current content items
		ArrayNode contentArray;
		try {
			contentArray = (ArrayNode) newContent.get("content");
		} catch (ClassCastException ex) {
			logger.error("Failed to cast JsonNode to ArrayNode");
			throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
		}

		if (contentArray.size() < slot) {
			// if the requested slot is outside of the current population, add it to the end
			contentArray.insert(contentArray.size(), newContentObject);
		} else {
			// Add new content to the desired slot
			contentArray.insert(slot + 1, newContentObject);
		}

		// Insert the new array into the request payload to the CouchDB
		newContent.set("content", contentArray);

		updatedRubric.set("content", newContent);
		// Write to CouchDB
		final ResponseEntity<JsonNode> db = performPut(rubricId, updatedRubric);
		// Return the CouchDB response
		return ResponseEntity.status(db.getStatusCode()).body(db.getBody());

	}
}