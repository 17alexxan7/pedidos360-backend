package cl.duoc.rabbitadmin.controller;

import cl.duoc.rabbitadmin.dto.BindingRequest;
import cl.duoc.rabbitadmin.dto.ExchangeRequest;
import cl.duoc.rabbitadmin.dto.QueueInfoResponse;
import cl.duoc.rabbitadmin.dto.QueueRequest;
import cl.duoc.rabbitadmin.service.RabbitAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rabbitmq")
@Validated
public class RabbitAdminController {

    private final RabbitAdminService service;

    public RabbitAdminController(RabbitAdminService service) {
        this.service = service;
    }

    @PostMapping("/queues")
    public ResponseEntity<Void> createQueue(@Valid @RequestBody QueueRequest request) {
        service.createQueue(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/queues/{name}")
    public ResponseEntity<QueueInfoResponse> getQueue(@PathVariable String name) {
        return ResponseEntity.ok(service.getQueue(name));
    }

    @DeleteMapping("/queues/{name}")
    public ResponseEntity<Void> deleteQueue(@PathVariable String name) {
        service.deleteQueue(name);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/exchanges")
    public ResponseEntity<Void> createExchange(@Valid @RequestBody ExchangeRequest request) {
        service.createExchange(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/exchanges/{name}")
    public ResponseEntity<Void> deleteExchange(@PathVariable String name) {
        service.deleteExchange(name);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bindings")
    public ResponseEntity<Void> createBinding(@Valid @RequestBody BindingRequest request) {
        service.createBinding(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/bindings")
    public ResponseEntity<Void> deleteBinding(
            @RequestParam @NotBlank String queue,
            @RequestParam @NotBlank String exchange,
            @RequestParam @NotBlank String routingKey) {
        service.deleteBinding(queue, exchange, routingKey);
        return ResponseEntity.noContent().build();
    }
}
