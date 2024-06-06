from dataclasses import dataclass

from card import Card
from location import Location
from user import User


@dataclass
class Transaction:
    transaction_id: str
    card: Card
    value: float
    timestamp: int
    location: Location

    def __str__(self):
        return f"{self.transaction_id} : {self.card} : {self.value} : {self.timestamp} : {self.location}"

    def to_dict(self):
        return {
            "transaction_id": self.transaction_id,
            "card": dict(self.card),
            "value": self.value,
            "timestamp": self.timestamp,
            "location": dict(self.location),
            "balance": self.card.balance,
        }

    def __iter__(self):
        yield "transaction_id", self.transaction_id
        yield "user_id", self.card.user.user_id
        yield "card_num", self.card.card_num
        yield "value", self.value
        yield "timestamp", self.timestamp
        yield "location", {"latitude": self.location.latitude, "longitude": self.location.longitude}
        yield "balance", self.card.balance

    @staticmethod
    def from_dict(transaction_dict):
        return Transaction(
            transaction_id=transaction_dict["transaction_id"],
            card=Card.from_dict(transaction_dict["card"]),
            value=transaction_dict["value"],
            timestamp=transaction_dict["timestamp"],
            location=Location.from_dict(transaction_dict["location"]),
        )
