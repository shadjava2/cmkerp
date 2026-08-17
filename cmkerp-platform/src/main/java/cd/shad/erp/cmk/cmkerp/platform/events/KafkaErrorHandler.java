package cd.shad.erp.cmk.cmkerp.platform.events;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Component;

@Component("kafkaErrorHandler")
@ConditionalOnProperty(name = "cmkerp.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class KafkaErrorHandler implements CommonErrorHandler {

  private static final Logger log = LoggerFactory.getLogger(KafkaErrorHandler.class);

  private final KafkaTemplate<String, Object> kafkaTemplate;

  public KafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  @Override
  public boolean handleOne(Exception exception, ConsumerRecord<?, ?> record, Consumer<?, ?> consumer,
      MessageListenerContainer container) {
    String originalTopic = record.topic();
    log.error("Erreur lors du traitement du message Kafka -> topic: {}, partition: {}, offset: {}, key: {}",
        originalTopic, record.partition(), record.offset(), record.key(), exception);

    String dltTopic = originalTopic + ".dlt";
    try {
      Object key = record.key();
      Object value = record.value();

      kafkaTemplate.send(dltTopic, key != null ? key.toString() : null, value);
      log.info("Message redirigé vers DLT -> topic: {}, partition: {}, offset: {}, key: {}",
          dltTopic, record.partition(), record.offset(), record.key());
    } catch (Exception e) {
      log.error("Échec lors de l'envoi du message vers le DLT {} -> topic: {}, partition: {}, offset: {}",
          dltTopic, originalTopic, record.partition(), record.offset(), e);
    }

    return true;
  }

  @Override
  public void handleOtherException(Exception exception, Consumer<?, ?> consumer,
      MessageListenerContainer container, boolean batchListener) {
    log.error("Erreur générale dans le listener Kafka", exception);
  }
}

