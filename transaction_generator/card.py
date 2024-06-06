from dataclasses import dataclass
import random

from user import User


@dataclass
class Card:
    card_num: int
    user: User
    balance: float
    initial_balance: float
    last_transaction_time: int = int(random.gauss(1577880000, 31622400))

    def __str__(self):
        return f"{self.card_num} : {self.user.user_id} : {self.balance}"

    def to_dict(self):
        return {
            "card_num": self.card_num,
            "user": dict(self.user),
            "balance": self.balance,
            "initial_balance": self.initial_balance,
            "last_transaction_time": self.last_transaction_time,
        }

    def __iter__(self):
        yield "card_num", self.card_num
        yield "user", dict(self.user)
        yield "balance", self.balance
        yield "initial_balance", self.initial_balance
        yield "last_transaction_time", self.last_transaction_time

    @staticmethod
    def from_dict(card_dict):
        return Card(
            card_num=card_dict["card_num"],
            user=User.from_dict(card_dict["user"]),
            balance=card_dict["balance"],
            initial_balance=card_dict["initial_balance"],
            last_transaction_time=card_dict["last_transaction_time"],
        )
