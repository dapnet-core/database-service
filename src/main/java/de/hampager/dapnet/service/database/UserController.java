package de.hampager.dapnet.service.database;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("users")
class UserController {

    private static final String BASE_URL = "http://dapnetdc2.db0sda.ampr.org:5984/users/_all_docs?include_docs=true";
    private static final String DB_USER = "admin";
    private static final String DB_PASS = "supersecret";
    private final RestTemplate users;

    @Autowired
    public UserController(RestTemplateBuilder builder) {
        users = builder.basicAuthorization(DB_USER, DB_PASS).build();
    }

    @GetMapping
    public ResponseEntity<JsonNode> getAll() {
        return users.getForEntity(BASE_URL, JsonNode.class);
        //return ResponseEntity.ok(n);
    }

}
