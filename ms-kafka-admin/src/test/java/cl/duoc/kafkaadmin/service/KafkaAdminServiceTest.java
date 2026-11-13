package cl.duoc.kafkaadmin.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.duoc.kafkaadmin.dto.CleanupPolicy;
import cl.duoc.kafkaadmin.dto.TopicInfoResponse;
import cl.duoc.kafkaadmin.dto.TopicRequest;
import cl.duoc.kafkaadmin.exception.KafkaOperationException;
import cl.duoc.kafkaadmin.exception.ResourceAlreadyExistsException;
import cl.duoc.kafkaadmin.exception.ResourceNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicCollection;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.errors.InvalidReplicationFactorException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.apache.kafka.common.internals.KafkaFutureImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaAdmin;

@ExtendWith(MockitoExtension.class)
class KafkaAdminServiceTest {

    @Mock
    private KafkaAdmin kafkaAdmin;
    @Mock
    private Admin admin;

    private KafkaAdminService service;

    @BeforeEach
    void setUp() {
        lenient().when(kafkaAdmin.getConfigurationProperties()).thenReturn(new HashMap<>());
        service = new KafkaAdminService(kafkaAdmin);
    }

    private static <T> KafkaFuture<T> failed(Throwable t) {
        KafkaFutureImpl<T> f = new KafkaFutureImpl<>();
        f.completeExceptionally(t);
        return f;
    }

    private static TopicRequest request() {
        return new TopicRequest("pedidos.test", 3, (short) 3, CleanupPolicy.DELETE, 60000L);
    }

    private void crearConResultado(KafkaFuture<Void> future) {
        CreateTopicsResult result = mock(CreateTopicsResult.class);
        when(result.all()).thenReturn(future);
        when(admin.createTopics(anyCollection())).thenReturn(result);
    }

    @Test
    void createTopic_ok_devuelveInfoYCierraAdmin() {
        crearConResultado(KafkaFuture.completedFuture(null));
        try (MockedStatic<Admin> st = mockStatic(Admin.class)) {
            st.when(() -> Admin.create(anyMap())).thenReturn(admin);
            TopicInfoResponse r = service.createTopic(request());
            assertEquals("pedidos.test", r.name());
            assertEquals(3, r.partitions());
            assertEquals(3, r.replicationFactor());
            verify(admin).close();
        }
    }

    @Test
    void createTopic_yaExiste_lanzaResourceAlreadyExists() {
        crearConResultado(failed(new TopicExistsException("existe")));
        try (MockedStatic<Admin> st = mockStatic(Admin.class)) {
            st.when(() -> Admin.create(anyMap())).thenReturn(admin);
            assertThrows(ResourceAlreadyExistsException.class, () -> service.createTopic(request()));
        }
    }

    @Test
    void createTopic_replicacionInvalida_lanzaIllegalArgument() {
        crearConResultado(failed(new InvalidReplicationFactorException("rf")));
        try (MockedStatic<Admin> st = mockStatic(Admin.class)) {
            st.when(() -> Admin.create(anyMap())).thenReturn(admin);
            assertThrows(IllegalArgumentException.class, () -> service.createTopic(request()));
        }
    }

    @Test
    void createTopic_otroError_lanzaKafkaOperationException() {
        crearConResultado(failed(new RuntimeException("broker caido")));
        try (MockedStatic<Admin> st = mockStatic(Admin.class)) {
            st.when(() -> Admin.create(anyMap())).thenReturn(admin);
            assertThrows(KafkaOperationException.class, () -> service.createTopic(request()));
        }
    }

    @Test
    void getTopic_ok_devuelveParticionesYReplicas() {
        Node n1 = new Node(1, "kafka-1", 9092);
        Node n2 = new Node(2, "kafka-2", 9092);
        Node n3 = new Node(3, "kafka-3", 9092);
        List<Node> nodes = List.of(n1, n2, n3);
        TopicDescription td = new TopicDescription("pedidos.eventos", false, List.of(
                new TopicPartitionInfo(0, n1, nodes, nodes),
                new TopicPartitionInfo(1, n2, nodes, nodes)));
        DescribeTopicsResult result = mock(DescribeTopicsResult.class);
        when(result.allTopicNames()).thenReturn(KafkaFuture.completedFuture(Map.of("pedidos.eventos", td)));
        when(admin.describeTopics(anyCollection())).thenReturn(result);
        try (MockedStatic<Admin> st = mockStatic(Admin.class)) {
            st.when(() -> Admin.create(anyMap())).thenReturn(admin);
            TopicInfoResponse r = service.getTopic("pedidos.eventos");
            assertEquals("pedidos.eventos", r.name());
            assertEquals(2, r.partitions());
            assertEquals(3, r.replicationFactor());
        }
    }

    @Test
    void getTopic_noExiste_lanzaResourceNotFound() {
        DescribeTopicsResult result = mock(DescribeTopicsResult.class);
        when(result.allTopicNames()).thenReturn(failed(new UnknownTopicOrPartitionException("no")));
        when(admin.describeTopics(anyCollection())).thenReturn(result);
        try (MockedStatic<Admin> st = mockStatic(Admin.class)) {
            st.when(() -> Admin.create(anyMap())).thenReturn(admin);
            assertThrows(ResourceNotFoundException.class, () -> service.getTopic("x"));
        }
    }

    @Test
    void listTopics_devuelveNombresOrdenados() {
        ListTopicsResult result = mock(ListTopicsResult.class);
        when(result.names()).thenReturn(KafkaFuture.completedFuture(Set.of("b", "c", "a")));
        when(admin.listTopics()).thenReturn(result);
        try (MockedStatic<Admin> st = mockStatic(Admin.class)) {
            st.when(() -> Admin.create(anyMap())).thenReturn(admin);
            assertEquals(List.of("a", "b", "c"), service.listTopics());
        }
    }

    @Test
    void listTopics_error_lanzaKafkaOperationException() {
        ListTopicsResult result = mock(ListTopicsResult.class);
        when(result.names()).thenReturn(failed(new RuntimeException("caido")));
        when(admin.listTopics()).thenReturn(result);
        try (MockedStatic<Admin> st = mockStatic(Admin.class)) {
            st.when(() -> Admin.create(anyMap())).thenReturn(admin);
            assertThrows(KafkaOperationException.class, () -> service.listTopics());
        }
    }

    @Test
    void deleteTopic_ok_llamaDeleteTopics() {
        DeleteTopicsResult result = mock(DeleteTopicsResult.class);
        when(result.all()).thenReturn(KafkaFuture.completedFuture(null));
        when(admin.deleteTopics(any(TopicCollection.class))).thenReturn(result);
        try (MockedStatic<Admin> st = mockStatic(Admin.class)) {
            st.when(() -> Admin.create(anyMap())).thenReturn(admin);
            service.deleteTopic("pedidos.test");
            verify(admin).deleteTopics(any(TopicCollection.class));
        }
    }

    @Test
    void deleteTopic_noExiste_lanzaResourceNotFound() {
        DeleteTopicsResult result = mock(DeleteTopicsResult.class);
        when(result.all()).thenReturn(failed(new UnknownTopicOrPartitionException("no")));
        when(admin.deleteTopics(any(TopicCollection.class))).thenReturn(result);
        try (MockedStatic<Admin> st = mockStatic(Admin.class)) {
            st.when(() -> Admin.create(anyMap())).thenReturn(admin);
            assertThrows(ResourceNotFoundException.class, () -> service.deleteTopic("x"));
        }
    }
}