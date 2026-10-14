package cl.duoc.pedidos360;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

/** Levanta el contexto completo; requiere la BD cloud, por eso solo corre si DB_URL esta definida. */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class Pedidos360BackendApplicationTests {

        @Test
        void contextLoads() {
        }

}