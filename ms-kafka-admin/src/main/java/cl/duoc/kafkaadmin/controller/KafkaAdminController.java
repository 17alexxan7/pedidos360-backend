package cl.duoc.kafkaadmin.controller;

import cl.duoc.kafkaadmin.dto.TopicInfoResponse;
import cl.duoc.kafkaadmin.dto.TopicRequest;
import cl.duoc.kafkaadmin.service.KafkaAdminService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST para administrar topicos de Kafka. No contiene logica:
 * todo se delega en KafkaAdminService.
 */
@RestController
@RequestMapping("/api/kafka")
public class KafkaAdminController {

    private final KafkaAdminService service;

    public KafkaAdminController(KafkaAdminService service) {
        this.service = service;
    }

    @PostMapping("/topics")
    public ResponseEntity<TopicInfoResponse> createTopic(@Valid @RequestBody TopicRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createTopic(request));
    }

    @GetMapping("/topics")
    public ResponseEntity<List<String>> listTopics() {
        return ResponseEntity.ok(service.listTopics());
    }

    @GetMapping("/topics/{name}")
    public ResponseEntity<TopicInfoResponse> getTopic(@PathVariable String name) {
        return ResponseEntity.ok(service.getTopic(name));
    }

    @DeleteMapping("/topics/{name}")
    public ResponseEntity<Void> deleteTopic(@PathVariable String name) {
        service.deleteTopic(name);
        return ResponseEntity.noContent().build();
    }
}