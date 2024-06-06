import json
import random
from time import sleep
from kafka import KafkaProducer
from card_generator import CardGenerator
from transaction_generator import transaction_generator
from transaction import Transaction


class TransactionSimulator:
    def __init__(
        self, sim_speed: int, anomaly_chance: float, card_limit: int, user_limit: int, filename: str, generate: bool
    ):
        self._sim_speed = sim_speed
        self._anomaly_chance = anomaly_chance
        self._card_limit = card_limit
        self._user_limit = user_limit
        self._filename = filename
        self._generate = generate
        self._producer = KafkaProducer(
            bootstrap_servers=["localhost:29092"], value_serializer=lambda m: json.dumps(m).encode("utf-8")
        )

    def run(self):
        card_generator = CardGenerator(self._filename, self._card_limit, self._user_limit)
        cards = card_generator.get_cards(self._generate)
        for transaction in transaction_generator(cards, self._anomaly_chance):
            self._producer.send("CreditCardTransactions", dict(transaction))
            sleep(1 / self._sim_speed)
