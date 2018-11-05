package de.hampager.dapnet.service.database.controller;

import java.net.URI;
import java.time.Instant;
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
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.hampager.dapnet.service.database.AppUser;
import de.hampager.dapnet.service.database.DbConfig;
import de.hampager.dapnet.service.database.JsonUtils;
import de.hampager.dapnet.service.database.MissingFieldException;
import de.hampager.dapnet.service.database.model.PermissionValue;

/**
 * This class implements the transmitters REST endpoint.
 *
 * @author Philipp Thiel & Ralf Wilke
 */
@CrossOrigin
@RestController
@RequestMapping("transmitters")
class TransmitterController extends AbstractController {
	private static final Logger logger = LoggerFactory.getLogger(TransmitterController.class);

	private static final Set<String> VALID_KEYS_UPDATE = Set.of("usage", "timeslots", "power", "owners", "groups",
			"coordinates", "aprs_broadcast", "enabled", "auth_key", "antenna", "type", "gain", "direction", "agl");

	private static final String[] REQUIRED_KEYS_CREATE = { "_id", "usage", "timeslots", "power", "owners", "groups",
			"coordinates", "aprs_broadcast", "enabled", "auth_key", "antenna", "type", "gain", "direction", "agl" };

	private static final String TRANSMITTER_LIST = "transmitter.list";
	private static final String TRANSMITTER_READ = "transmitter.read";
	private static final String TRANSMITTER_UPDATE = "transmitter.update";
	private static final String TRANSMITTER_CREATE = "transmitter.create";
	private static final String TRANSMITTER_DELETE = "transmitter.delete";
	private final String namesPath;
	private final String groupsPath;

	@Autowired
	public TransmitterController(DbConfig config, RestTemplateBuilder builder) {
		super(config, builder, "transmitters");
		this.namesPath = basePath.concat("_design/transmitters/_list/names/byId");
		this.groupsPath = basePath.concat("_design/transmitters/_list/groups/byGroup?group_level=5");
	}

	@GetMapping("_names")
	public ResponseEntity<JsonNode> getNames() {
		requirePermission(TRANSMITTER_LIST);

		JsonNode in = restTemplate.getForObject(namesPath, JsonNode.class);
		return ResponseEntity.ok(in);
	}

	@GetMapping("_groups")
	public ResponseEntity<JsonNode> getGroups() {
		requirePermission(TRANSMITTER_LIST);

		JsonNode in = restTemplate.getForObject(groupsPath, JsonNode.class);
		return ResponseEntity.ok(in);
	}

	@PutMapping
	public ResponseEntity<JsonNode> putTransmitter(@RequestBody JsonNode transmitter) {
		if (transmitter.has("_rev")) {
			return updateTransmitter(transmitter);
		} else {
			return createTransmitter(transmitter);
		}
	}

	// UNTESTED
	private ResponseEntity<JsonNode> createTransmitter(JsonNode transmitter) {
		requirePermission(TRANSMITTER_CREATE, PermissionValue.ALL);
		final AppUser appUser = getCurrentUser();

		try {
			JsonUtils.checkRequiredFields(transmitter, REQUIRED_KEYS_CREATE);
		} catch (MissingFieldException ex) {
			throw new HttpServerErrorException(HttpStatus.BAD_REQUEST, ex.getMessage());
		}

		final ObjectNode modTransmitter = (ObjectNode) JsonUtils.trimValues(transmitter);
		final String transmitterId = modTransmitter.get("_id").asText().toLowerCase();
		modTransmitter.put("_id", transmitterId);

		final String ts = Instant.now().toString();
		modTransmitter.put("created_on", ts);
		modTransmitter.put("created_by", appUser.getUsername());
		modTransmitter.put("changed_on", ts);
		modTransmitter.put("changed_by", appUser.getUsername());

		final ResponseEntity<JsonNode> db = performPut(transmitterId, modTransmitter);
		if (db.getStatusCode() == HttpStatus.CREATED) {
			final URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("{id}")
					.buildAndExpand(transmitterId).toUri();
			return ResponseEntity.created(location).body(db.getBody());
		} else {
			return ResponseEntity.status(db.getStatusCode()).body(db.getBody());
		}
	}

	// UNTESTED
	private ResponseEntity<JsonNode> updateTransmitter(JsonNode transmitterUpdate) {
		final PermissionValue permission = requirePermission(TRANSMITTER_UPDATE, PermissionValue.ALL,
				PermissionValue.IF_OWNER);
		final AppUser appUser = getCurrentUser();

		transmitterUpdate = JsonUtils.trimValues(transmitterUpdate);

		final String transmitterId = transmitterUpdate.get("_id").asText().toLowerCase();
		final JsonNode oldTransmitter = restTemplate.getForObject(paramPath, JsonNode.class, transmitterId);
		if (permission == PermissionValue.IF_OWNER && !JsonUtils.isOwner(oldTransmitter, appUser.getUsername())) {
			throw new HttpServerErrorException(HttpStatus.FORBIDDEN);
		}

		ObjectNode modTransmitter;
		try {
			modTransmitter = (ObjectNode) oldTransmitter;
		} catch (ClassCastException ex) {
			logger.error("Failed to cast JsonNode to ObjectNode");
			throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
		}

		transmitterUpdate.fields().forEachRemaining(e -> {
			if (VALID_KEYS_UPDATE.contains(e.getKey())) {
				modTransmitter.set(e.getKey(), e.getValue());
			}
		});

		modTransmitter.put("changed_on", Instant.now().toString());
		modTransmitter.put("changed_by", appUser.getUsername());

		final ResponseEntity<JsonNode> db = performPut(transmitterId, modTransmitter);
		return ResponseEntity.status(db.getStatusCode()).body(db.getBody());
	}

	// UNTESTED
	@DeleteMapping("{name}")
	public ResponseEntity<JsonNode> deleteTransmitter(@PathVariable String name, @RequestParam String revision) {
		final AppUser user = getCurrentUser();
		final PermissionValue permission = user.getPermissions().getOrDefault(TRANSMITTER_DELETE, PermissionValue.NONE);
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
