import random
from typing import List
import uuid
import json
from card import Card
from location import Location
from user import User


class CardGenerator:
    def __init__(self, filename: str = "cards.json", card_limit: int = 10_000, user_num: int = 1_000):
        self._filename = filename
        self._card_limit = card_limit
        self._user_num = user_num

    def _generate_users(self):
        return [
            User(
                user_id=str(uuid.uuid4()), typical_location=Location(random.uniform(-90, 90), random.uniform(-180, 180))
            )
            for _ in range(self._user_num)
        ]

    def _generate_cards(self):
        users = self._generate_users()
        cards = []
        for _ in range(self._card_limit):
            balance = round(random.uniform(100, 1_000_000), 2)
            cards.append(
                Card(
                    card_num=random.randint(1, 1_000_000_000),
                    user=random.choice(users),
                    balance=balance,
                    initial_balance=balance,
                )
            )
        return cards

    def _load_cards(self):
        try:
            with open(self._filename, "r") as file:
                card_dicts = json.load(file)
                cards = [Card.from_dict(card_dict) for card_dict in card_dicts]
        except FileNotFoundError:
            cards = self._generate_cards()
            with open(self._filename, "w") as file:
                json.dump(cards, file, indent=2, default=lambda x: x.to_dict())
        return cards

    def _save_cards(self, cards: List[Card]):
        with open(self._filename, "w") as file:
            json.dump(cards, file, indent=2, default=lambda x: x.to_dict())

    def get_cards(self, generate: bool = False):
        if generate:
            cards = self._generate_cards()
            self._save_cards(cards)
            return cards
        return self._load_cards()
