from dataclasses import dataclass


@dataclass
class Location:
    latitude: float
    longitude: float

    def __str__(self):
        return f"{self.latitude}, {self.longitude}"

    def to_dict(self):
        return {"latitude": self.latitude, "longitude": self.longitude}

    def __iter__(self):
        yield "latitude", self.latitude
        yield "longitude", self.longitude

    @staticmethod
    def from_dict(location_dict):
        return Location(latitude=location_dict["latitude"], longitude=location_dict["longitude"])
