package psd;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.connector.kafka.source.reader.deserializer.KafkaRecordDeserializationSchema;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.io.IOException;

public class LocationDeserializationSchema implements KafkaRecordDeserializationSchema<Location> {

	private static final long serialVersionUID = 1L;

	private static final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public TypeInformation<Location> getProducedType() {
		return TypeInformation.of(Location.class);
	}

	@Override
	public void deserialize(ConsumerRecord<byte[], byte[]> record, Collector<Location> out) throws IOException {
		Location message = objectMapper.readValue(record.value(), Location.class);
		out.collect(message);
	}
}
