package cd.shad.erp.cmk.cmkerp.platform.config.messaging;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration Kafka pour le messaging asynchrone.
 *
 * <p>
 * Permet de publier et consommer des événements de domaine via Kafka,
 * essentiel pour découpler les traitements asynchrones (notifications, audit, reporting)
 * des traitements immédiats de l'API.
 *
 * <p>
 * Cette configuration est conditionnelle : elle ne s'active que si {@code cmkerp.kafka.enabled=true}.
 * Quand Kafka est désactivé, aucun bean Kafka ne sera créé et l'application peut démarrer sans broker Kafka.
 *
 * <p>
 * Configuration requise dans application-*.yml :
 * <pre>{@code
 * cmkerp:
 *   kafka:
 *     enabled: true  # false pour désactiver Kafka
 * spring:
 *   kafka:
 *     bootstrap-servers: ${CMK_KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
 *     consumer:
 *       group-id: ${CMK_KAFKA_GROUP_ID:cmkerp-api-group}
 *       auto-offset-reset: earliest
 *       key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
 *       value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
 *     producer:
 *       key-serializer: org.apache.kafka.common.serialization.StringSerializer
 *       value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
 * }</pre>
 *
 * <p>
 * Topics Kafka utilisés :
 * <ul>
 * <li>{@code cmkerp-user-events} : événements utilisateur</li>
 * <li>{@code cmkerp-audit-events} : événements d'audit</li>
 * </ul>
 *

 */
@Configuration
@EnableKafka
@ConditionalOnProperty(name = "cmkerp.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:cmkerp-api-group}")
    private String groupId;

    /**
     * Configuration du ProducerFactory pour publier des événements.
     *
     * @return ProducerFactory configuré
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        // Optimisations pour haute performance (Facebook-Grade)
        configProps.put(ProducerConfig.ACKS_CONFIG, "1"); // Acknowledgment : leader seulement (équilibre perf/fiabilité)
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5); // Performance améliorée
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 32768); // 32KB (optimisé)
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 10); // Attendre 10ms pour batch
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 67108864); // 64MB (augmenté)
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy"); // Compression pour réduire bande passante
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); // Idempotence pour garantir exactly-once
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * KafkaTemplate pour publier des événements.
     *
     * @return KafkaTemplate configuré
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * Configuration du ConsumerFactory pour consommer des événements.
     *
     * @return ConsumerFactory configuré
     */
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"); // Lire depuis le début si pas d'offset
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*"); // Permettre la désérialisation des événements
        // Optimisations pour haute performance (Facebook-Grade)
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1000); // Traiter jusqu'à 1000 messages par poll (optimisé)
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024); // Attendre au moins 1KB avant de retourner
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500); // Attendre max 500ms pour batch
        props.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, 10485760); // 10MB par partition
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Factory pour créer des listeners Kafka concurrents.
     *
     * @return ConcurrentKafkaListenerContainerFactory configuré
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(5); // 5 threads concurrents par listener (optimisé pour haute performance)
        factory.setBatchListener(true); // Activer le batch processing pour meilleure performance
        return factory;
    }
}

