import json
from kafka import KafkaConsumer

def create_kafka_consumer(topic_name: str = 'CreditCardTransactions'):
    return KafkaConsumer(
        topic_name,
        bootstrap_servers='localhost:29092',
        auto_offset_reset='earliest'
    )
