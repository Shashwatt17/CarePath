import json
import math
import osmium
from pathlib import Path

INPUT = Path("osm-data/central-zone-latest.osm.pbf")
OUTPUT = Path("src/main/resources/nearby/healthcare-facilities.json")

CATEGORY_MAP = {
    ("amenity", "hospital"): "HOSPITAL",
    ("amenity", "clinic"): "CLINIC",
    ("amenity", "pharmacy"): "PHARMACY",
    ("healthcare", "hospital"): "HOSPITAL",
    ("healthcare", "clinic"): "CLINIC",
    ("healthcare", "pharmacy"): "PHARMACY",
    ("healthcare", "laboratory"): "DIAGNOSTIC_CENTER",
    ("healthcare", "diagnostic_centre"): "DIAGNOSTIC_CENTER",
}


def get_category(tags):
    for (key, value), category in CATEGORY_MAP.items():
        if tags.get(key) == value:
            return category
    return None


def build_address(tags):
    parts = []

    house = tags.get("addr:housenumber")
    street = tags.get("addr:street")
    locality = (
        tags.get("addr:suburb")
        or tags.get("addr:neighbourhood")
        or tags.get("addr:place")
    )
    city = (
        tags.get("addr:city")
        or tags.get("addr:town")
        or tags.get("addr:village")
    )
    state = tags.get("addr:state")
    postcode = tags.get("addr:postcode")

    for value in (house, street, locality, city, state, postcode):
        if value and value not in parts:
            parts.append(value)

    return ", ".join(parts) if parts else None


def first_tag(tags, *names):
    for name in names:
        value = tags.get(name)
        if value:
            return value
    return None


class HealthcareHandler(osmium.SimpleHandler):

    def __init__(self):
        super().__init__()
        self.facilities = []

    def add(self, obj, kind, lat, lon):
        category = get_category(obj.tags)

        if category is None:
            return

        name = first_tag(
            obj.tags,
            "name",
            "official_name",
            "operator"
        )

        if not name:
            return

        if not (
            math.isfinite(lat)
            and math.isfinite(lon)
            and -90 <= lat <= 90
            and -180 <= lon <= 180
        ):
            return

        phone = first_tag(
            obj.tags,
            "contact:phone",
            "phone"
        )

        website = first_tag(
            obj.tags,
            "contact:website",
            "website"
        )

        opening_hours = obj.tags.get("opening_hours")

        hours = []
        if opening_hours:
            hours.append(opening_hours)

        self.facilities.append({
            "id": f"osm-{kind}-{obj.id}",
            "name": name,
            "category": category,
            "address": build_address(obj.tags),
            "latitude": round(lat, 7),
            "longitude": round(lon, 7),
            "phone": phone,
            "website": website,
            "hours": hours
        })

    def node(self, n):
        if n.location.valid():
            self.add(
                n,
                "node",
                n.location.lat,
                n.location.lon
            )

    def way(self, w):
        try:
            if not w.nodes:
                return

            coordinates = [
                (node.location.lat, node.location.lon)
                for node in w.nodes
                if node.location.valid()
            ]

            if not coordinates:
                return

            lat = sum(x[0] for x in coordinates) / len(coordinates)
            lon = sum(x[1] for x in coordinates) / len(coordinates)

            self.add(w, "way", lat, lon)

        except osmium.InvalidLocationError:
            return


if not INPUT.exists():
    raise SystemExit(f"Input file not found: {INPUT}")

handler = HealthcareHandler()

print(f"Reading {INPUT} ...")

handler.apply_file(
    str(INPUT),
    locations=True
)

# Remove exact duplicate OSM IDs defensively.
unique = {}

for facility in handler.facilities:
    unique[facility["id"]] = facility

facilities = list(unique.values())

facilities.sort(
    key=lambda x: (
        x["category"],
        x["name"].lower(),
        x["id"]
    )
)

OUTPUT.parent.mkdir(parents=True, exist_ok=True)

with OUTPUT.open("w", encoding="utf-8") as f:
    json.dump(
        facilities,
        f,
        ensure_ascii=False,
        indent=2
    )

print()
print(f"Extracted {len(facilities)} healthcare facilities.")

counts = {}

for facility in facilities:
    category = facility["category"]
    counts[category] = counts.get(category, 0) + 1

for category in sorted(counts):
    print(f"{category}: {counts[category]}")

print()
print(f"Written to: {OUTPUT}")