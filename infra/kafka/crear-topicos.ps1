# Crea los topicos de Pedidos360 en el cluster Kafka local (idempotente).
# Uso: .\crear-topicos.ps1
$bootstrap = "kafka-1:29092"
$topicos = @(
    @{ nombre = "pedidos.eventos";  configs = @("cleanup.policy=delete", "retention.ms=604800000") },
    @{ nombre = "pedidos.metricas"; configs = @("cleanup.policy=compact") }
)
foreach ($t in $topicos) {
    $args = @("--bootstrap-server", $bootstrap, "--create", "--if-not-exists",
              "--topic", $t.nombre, "--partitions", "3", "--replication-factor", "3")
    foreach ($c in $t.configs) { $args += @("--config", $c) }
    docker exec kafka-1 kafka-topics @args
}
docker exec kafka-1 kafka-topics --bootstrap-server $bootstrap --describe
