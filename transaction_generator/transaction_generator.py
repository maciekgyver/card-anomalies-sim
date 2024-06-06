import time
import random
from typing import List
import uuid

from card import Card
from location import Location
from timing_decorator import timing_decorator
from transaction import Transaction


# @timing_decorator
def transaction_generator(cards: List[Card], anomaly_chance: float):
    while True:
        card = random.choice(cards)
        if (value := round(abs(random.gauss(card.initial_balance / 10000, card.balance / 10000)), 2)) > card.balance:
            raise StopIteration

        latitude = random.gauss(card.user.typical_location.latitude, 2)
        longitude = random.gauss(card.user.typical_location.longitude, 3)
        transaction_time = int(card.last_transaction_time + abs(random.gauss(60 * 60 * 24, 60 * 60 * 12)))

        if random.random() < anomaly_chance / 100:
            rand = random.random()
            if rand < 0.33: # Value anomaly
                print("Value anomaly")
                value *= 5
                if value > card.balance:
                    value = card.balance
                card.balance -= value
            elif 0.33 < rand < 0.66:  # Location anomaly
                print("Location anomaly")
                longitude += random.uniform(-90, 90)
                latitude += random.uniform(-180, 180)
            else:   # Time anomaly
                print("Time anomaly")
                for _ in range(5):
                    if value > card.balance:
                        raise StopIteration
                    card.balance -= value
                    transaction_time = int(card.last_transaction_time + abs(random.gauss(60, 30)))
                    yield Transaction(
                        transaction_id=str(uuid.uuid4()),
                        card=card,
                        value=value,
                        timestamp=transaction_time,
                        location=Location(latitude=latitude, longitude=longitude),
                    )
                    
        card.last_transaction_time = transaction_time
        yield Transaction(
            transaction_id=str(uuid.uuid4()),
            card=card,
            value=value,
            timestamp=transaction_time,
            location=Location(latitude=latitude, longitude=longitude),
        )
