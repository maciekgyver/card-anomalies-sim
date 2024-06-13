package psd;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.formats.json.JsonSerializationSchema;

public class App {

    public static void main(String[] args) throws Exception {

        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        String bootstrapServers = "kafka:9092";

        KafkaSource<Transaction> source = KafkaSource.<Transaction>builder()
                .setBootstrapServers(bootstrapServers)
                .setTopics("CreditCardTransactions")
                .setGroupId("TransactionsConsumer")
                .setProperty("enable.auto.commit", "true")
                .setStartingOffsets(OffsetsInitializer.committedOffsets(OffsetResetStrategy.EARLIEST))
                .setDeserializer(new TransactionDeserializationSchema())
                .build();

        DataStream<Alert> value_alerts = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source")
                .keyBy(Transaction::getCardNumber)
                .process(new ValueAnomalyDetector());

        DataStream<Alert> location_alerts = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source")
                .keyBy(Transaction::getCardNumber)
                .process(new LocationAnomalyDetector());

        DataStream<Alert> interval_alerts = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source")
                .keyBy(Transaction::getCardNumber)
                .process(new TimeAnomalyDetector());


        JsonSerializationSchema<Alert> jsonSchema = new JsonSerializationSchema<>();

        KafkaSink<Alert> AlertKafkaSink = KafkaSink.<Alert>builder()
                .setBootstrapServers(bootstrapServers)
                .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                        .setTopic("TransactionAlerts")
                        .setValueSerializationSchema(jsonSchema)
                        .build())
                .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
                .build();

        value_alerts.sinkTo(AlertKafkaSink);
        location_alerts.sinkTo(AlertKafkaSink);
        interval_alerts.sinkTo(AlertKafkaSink);

        env.execute();
    }

}
