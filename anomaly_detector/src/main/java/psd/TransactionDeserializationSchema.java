package psd;

import java.io.IOException;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.connector.kafka.source.reader.deserializer.KafkaRecordDeserializationSchema;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.ConsumerRecord;

public class TransactionDeserializationSchema implements KafkaRecordDeserializationSchema<Transaction> {

	private static final long serialVersionUID = 1L;

	private static final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public TypeInformation<Transaction> getProducedType() {
		return TypeInformation.of(Transaction.class);
	}

	@Override
	public void deserialize(ConsumerRecord<byte[], byte[]> record, Collector<Transaction> out) throws IOException {
		Transaction message = objectMapper.readValue(record.value(), Transaction.class);
		out.collect(message);
	}
}
