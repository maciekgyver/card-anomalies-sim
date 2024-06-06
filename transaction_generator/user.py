from dataclasses import dataclass

from location import Location


@dataclass
class User:
    user_id: str
    typical_location: Location

    def __str__(self):
        return f"{self.user_id} : {self.typical_location}"

    def to_dict(self):
        return {"user_id": self.user_id, "typical_location": dict(self.typical_location)}

    def __iter__(self):
        yield "user_id", self.user_id
        yield "typical_location", dict(self.typical_location)

    @staticmethod
    def from_dict(user_dict):
        return User(user_id=user_dict["user_id"], typical_location=Location.from_dict(user_dict["typical_location"]))
