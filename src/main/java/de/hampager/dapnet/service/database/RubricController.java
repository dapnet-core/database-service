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
 * This class implements the rubrics REST endpoint.
 *
 * @author Philipp Thiel & Ralf Wilke
 */
@CrossOrigin
@RestController
@RequestMapping("rubrics")
class RubricController extends AbstractController {

	private static final Logger logger = LoggerFactory.getLogger(RubricController.class);
	private static final Set<String> VALID_KEYS_UPDATE = Set.of("number", "description", "label", "transmitter_groups",
			"transmitters", "cyclic_transmit", "cyclic_transmit_interval", "owners");
	private static final String[] REQUIRED_KEYS_CREATE = { "_id", "number", "label", "description",
			"transmitter_groups", "transmitters", "owners" };
	private static final String RUBRIC_LIST = "rubric.list";
	private static final String RUBRIC_READ = "rubric.read";
	private static final String RUBRIC_UPDATE = "rubric.update";
	private static final String RUBRIC_CREATE = "rubric.create";
	private static final String RUBRIC_DELETE = "rubric.delete";
	private static final int RUBIC_MAX_NUMBER = 95;
	private final String namesPath;
	private final String descriptionPath;
	private final String labelPath;
	private final String fullmetaPath;

	@Autowired
	public RubricController(DbConfig config, RestTemplateBuilder builder) {
		super(config, builder, "rubrics");
		this.namesPath = basePath.concat("_design/rubrics/_list/names/byId");
		this.descriptionPath = basePath.concat("_design/rubrics/_list/descriptions/descriptions");
		this.labelPath = basePath.concat("_design/rubrics/_list/labels/labels");
		this.fullmetaPath = basePath.concat("_design/rubrics/_list/fullmeta/fullmeta");
	}

	@GetMapping
	public ResponseEntity<JsonNode> getAll(Authentication authentication, @RequestParam Map<String, String> params) {
		ensureAuthenticated(authentication, RUBRIC_READ);

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
	public ResponseEntity<JsonNode> getNames(Authentication authentication) {
		ensureAuthenticated(authentication, RUBRIC_LIST);

		JsonNode in = restTemplate.getForObject(namesPath, JsonNode.class);
		return ResponseEntity.ok(in);
	}

	@GetMapping("_descriptions")
	public ResponseEntity<JsonNode> getDescription(Authentication authentication) {
		ensureAuthenticated(authentication, RUBRIC_LIST);

		JsonNode in = restTemplate.getForObject(descriptionPath, JsonNode.class);
		return ResponseEntity.ok(in);
	}

	@GetMapping("_labels")
	public ResponseEntity<JsonNode> getLabels(Authentication authentication) {
		ensureAuthenticated(authentication, RUBRIC_LIST);

		JsonNode in = restTemplate.getForObject(labelPath, JsonNode.class);
		return ResponseEntity.ok(in);
	}

	@GetMapping("_fullmeta")
	public ResponseEntity<JsonNode> getFullMeta(Authentication authentication) {
		ensureAuthenticated(authentication, RUBRIC_LIST);

		JsonNode in = restTemplate.getForObject(fullmetaPath, JsonNode.class);
		return ResponseEntity.ok(in);
	}

	@GetMapping("_view/byNumber")
	public ResponseEntity<JsonNode> getbyNumber(Authentication authentication,
			@RequestParam Map<String, String> params) {
		ensureAuthenticated(authentication, RUBRIC_READ);

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
	public ResponseEntity<JsonNode> getbyTransmitter(Authentication authentication,
			@RequestParam Map<String, String> params) {
		ensureAuthenticated(authentication, RUBRIC_READ);

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
	public ResponseEntity<JsonNode> getbyTransmitterGroup(Authentication authentication,
			@RequestParam Map<String, String> params) {
		ensureAuthenticated(authentication, RUBRIC_READ);

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
	public ResponseEntity<JsonNode> getRubric(Authentication authentication, @PathVariable String rubricname) {
		ensureAuthenticated(authentication, RUBRIC_READ, rubricname);

		JsonNode in = restTemplate.getForObject(paramPath, JsonNode.class, rubricname);
		return ResponseEntity.ok(in);
	}

	@PutMapping
	public ResponseEntity<JsonNode> putRubric(Authentication authentication, @RequestBody JsonNode rubric) {
		if ((rubric.has("number")) && (rubric.get("number").isInt()) && (rubric.get("number").intValue() > 95)) {
			// TODO: Throw error, as rubric number is to high to fit on Skypers
		}

		if (rubric.has("_rev")) {
			return updateRubric(authentication, rubric);
		} else {
			return createRubric(authentication, rubric);
		}
	}

	// UNTESTED
	private ResponseEntity<JsonNode> createRubric(Authentication auth, JsonNode rubric) {
		ensureAuthenticated(auth, RUBRIC_CREATE);

		try {
			JsonUtils.validateRequiredFields(rubric, REQUIRED_KEYS_CREATE);
		} catch (MissingFieldException ex) {
			throw new HttpServerErrorException(HttpStatus.BAD_REQUEST, ex.getMessage());
		}

		ObjectNode modRubric;
		try {
			modRubric = (ObjectNode) rubric;
		} catch (ClassCastException ex) {
			logger.error("Failed to cast JsonNode to ObjectNode");
			throw new HttpServerErrorException(HttpStatus.BAD_REQUEST);
		}

		final String rubricId = modRubric.get("_id").asText().trim().toLowerCase();
		modRubric.put("_id", rubricId);

		final String ts = Instant.now().toString();
		modRubric.put("created_on", ts);
		modRubric.put("created_by", auth.getName());
		modRubric.put("changed_on", ts);
		modRubric.put("changed_by", auth.getName());

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

		final ResponseEntity<JsonNode> db = performPut(paramPath, rubricId, modRubric);
		if (db.getStatusCode() == HttpStatus.CREATED) {
			final URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("{id}").buildAndExpand(rubricId)
					.toUri();
			return ResponseEntity.created(location).body(db.getBody());
		} else {
			return ResponseEntity.status(db.getStatusCode()).body(db.getBody());
		}
	}

	// UNTESTED
	private ResponseEntity<JsonNode> updateRubric(Authentication auth, JsonNode rubricUpdate) {
		ensureAuthenticated(auth, RUBRIC_UPDATE, auth.getName());

		final String rubricId = rubricUpdate.get("_id").asText().trim().toLowerCase();
		final JsonNode oldRubric = restTemplate.getForObject(paramPath, JsonNode.class, rubricId);
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
		modRubric.put("changed_by", auth.getName());

		final ResponseEntity<JsonNode> db = performPut(paramPath, rubricId, modRubric);
		return ResponseEntity.status(db.getStatusCode()).body(db.getBody());
	}

	// UNTESTED
	@DeleteMapping("{name}")
	public ResponseEntity<JsonNode> deleteRubric(Authentication authentication, @PathVariable String name,
			@RequestParam String rev) {
		ensureAuthenticated(authentication, RUBRIC_DELETE, name);

		// TODO Delete referenced objects
		final ResponseEntity<JsonNode> db = performDelete(paramPath, name);
		return ResponseEntity.status(db.getStatusCode()).body(db.getBody());
	}
}
