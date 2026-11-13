package cl.duoc.kafkaadmin.service;

import cl.duoc.kafkaadmin.dto.TopicInfoResponse;
import cl.duoc.kafkaadmin.dto.TopicRequest;
import cl.duoc.kafkaadmin.exception.KafkaOperationException;
import cl.duoc.kafkaadmin.exception.ResourceAlreadyExistsException;
import cl.duoc.kafkaadmin.exception.ResourceNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.TopicCollection;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.errors.InvalidReplicationFactorException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Service;

/**
 * Toda la logica de administracion de topicos vive aqui. El controller solo
 * delega. Se abre un Admin por operacion con la configuracion centralizada
 * de KafkaAdmin (bootstrap-servers del application.yml).
 */
@Service
public class KafkaAdminService {

    private static final Logger log = LoggerFactory.getLogger(KafkaAdminService.class);
    private static final long TIMEOUT_SECONDS = 10;

    private final KafkaAdmin kafkaAdmin;

    public KafkaAdminService(KafkaAdmin kafkaAdmin) {
        this.kafkaAdmin = kafkaAdmin;
    }

    public TopicInfoResponse createTopic(TopicRequest req) {
        Map<String, String> configs = new HashMap<>();
        configs.put(TopicConfig.CLEANUP_POLICY_CONFIG, req.cleanupPolicy().getValue());
        if (req.retentionMs() != null) {
            configs.put(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(req.retentionMs()));
        }
        NewTopic topic = new NewTopic(req.name(), req.partitions(), req.replicationFactor()).configs(configs);
        try (Admin admin = Admin.create(kafkaAdmin.getConfigurationProperties())) {
            admin.createTopics(List.of(topic)).all().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            log.info("Topico creado: {} (particiones={}, replicas={}, cleanup={})",
                    req.name(), req.partitions(), req.replicationFactor(), req.cleanupPolicy());
            return new TopicInfoResponse(req.name(), req.partitions(), req.replicationFactor());
        } catch (ExecutionException e) {
            if (e.getCause() instanceof TopicExistsException) {
                throw new ResourceAlreadyExistsException("El topico '" + req.name() + "' ya existe");
            }
            if (e.getCause() instanceof InvalidReplicationFactorException) {
                throw new IllegalArgumentException("Factor de replicacion invalido: " + e.getCause().getMessage());
            }
            throw operationFailed("crear el topico '" + req.name() + "'", e);
        } catch (InterruptedException | TimeoutException e) {
            throw operationFailed("crear el topico '" + req.name() + "'", e);
        }
    }

    public TopicInfoResponse getTopic(String name) {
        try (Admin admin = Admin.create(kafkaAdmin.getConfigurationProperties())) {
            TopicDescription td = admin.describeTopics(List.of(name)).allTopicNames()
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS).get(name);
            int replicas = td.partitions().isEmpty() ? 0 : td.partitions().get(0).replicas().size();
            return new TopicInfoResponse(td.name(), td.partitions().size(), replicas);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof UnknownTopicOrPartitionException) {
                throw new ResourceNotFoundException("El topico '" + name + "' no existe");
            }
            throw operationFailed("consultar el topico '" + name + "'", e);
        } catch (InterruptedException | TimeoutException e) {
            throw operationFailed("consultar el topico '" + name + "'", e);
        }
    }

    public List<String> listTopics() {
        try (Admin admin = Admin.create(kafkaAdmin.getConfigurationProperties())) {
            return admin.listTopics().names().get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .stream().sorted().toList();
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw operationFailed("listar los topicos", e);
        }
    }

    public void deleteTopic(String name) {
        try (Admin admin = Admin.create(kafkaAdmin.getConfigurationProperties())) {
            admin.deleteTopics(TopicCollection.ofTopicNames(List.of(name))).all()
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            log.info("Topico eliminado: {}", name);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof UnknownTopicOrPartitionException) {
                throw new ResourceNotFoundException("El topico '" + name + "' no existe");
            }
            throw operationFailed("eliminar el topico '" + name + "'", e);
        } catch (InterruptedException | TimeoutException e) {
            throw operationFailed("eliminar el topico '" + name + "'", e);
        }
    }

    private KafkaOperationException operationFailed(String action, Exception e) {
        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        Throwable cause = e instanceof ExecutionException && e.getCause() != null ? e.getCause() : e;
        return new KafkaOperationException("No se pudo " + action + ": " + cause.getMessage(), cause);
    }
}