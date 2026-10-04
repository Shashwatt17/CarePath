package com.carepath.nearby;

import static com.carepath.nearby.NearbyCareProvider.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class LocalOpenStreetMapProvider implements NearbyCareProvider {

    private final ObjectMapper json;
    private final List<LocalFacility> facilities = new ArrayList<>();

    public LocalOpenStreetMapProvider(ObjectMapper json) {
        this.json = json;
    }

    record LocalFacility(
            String id,
            String name,
            Category category,
            String address,
            double latitude,
            double longitude,
            String phone,
            String website,
            List<String> hours) {}

    @PostConstruct
    void load() {
        try (InputStream in = getClass()
                .getResourceAsStream("/nearby/healthcare-facilities.json")) {

            if (in == null) {
                throw new IllegalStateException(
                        "Healthcare facility dataset is missing");
            }

            JsonNode root = json.readTree(in);

            if (!root.isArray()) {
                throw new IllegalStateException(
                        "Healthcare facility dataset must be an array");
            }

            for (JsonNode n : root) {
                LocalFacility facility = parse(n);

                if (facility != null) {
                    facilities.add(facility);
                }
            }

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Unable to load healthcare facility dataset", e);
        }
    }

    private LocalFacility parse(JsonNode n) {
        try {
            String id = text(n, "id");
            String name = text(n, "name");
            String categoryText = text(n, "category");

            if (id == null || name == null || categoryText == null) {
                return null;
            }

            double latitude = n.path("latitude").asDouble(Double.NaN);
            double longitude = n.path("longitude").asDouble(Double.NaN);

            if (!Double.isFinite(latitude)
                    || !Double.isFinite(longitude)
                    || Math.abs(latitude) > 90
                    || Math.abs(longitude) > 180) {
                return null;
            }

            Category category = Category.valueOf(categoryText);

            List<String> hours = new ArrayList<>();

            if (n.path("hours").isArray()) {
                for (JsonNode h : n.path("hours")) {
                    if (h.isTextual() && !h.asText().isBlank()) {
                        hours.add(h.asText());
                    }
                }
            }

            return new LocalFacility(
                    id,
                    name,
                    category,
                    text(n, "address"),
                    latitude,
                    longitude,
                    PlaceSafety.phone(text(n, "phone")),
                    PlaceSafety.web(text(n, "website")),
                    List.copyOf(hours));

        } catch (Exception e) {
            return null;
        }
    }

    private static String text(JsonNode n, String field) {
        JsonNode value = n.path(field);

        if (!value.isTextual() || value.asText().isBlank()) {
            return null;
        }

        return value.asText();
    }

    @Override
    public boolean available() {
        return !facilities.isEmpty();
    }

    @Override
    public List<Facility> search(Search search) {

        List<Facility> results = new ArrayList<>();

        for (LocalFacility f : facilities) {

            if (f.category() != search.category()) {
                continue;
            }

            long distance = PlaceSafety.distance(
                    search.latitude(),
                    search.longitude(),
                    f.latitude(),
                    f.longitude());

            if (distance > search.radiusMeters()) {
                continue;
            }

            String directions =
                    "https://www.openstreetmap.org/directions"
                    + "?engine=fossgis_osrm_car"
                    + "&route="
                    + search.latitude()
                    + "%2C"
                    + search.longitude()
                    + "%3B"
                    + f.latitude()
                    + "%2C"
                    + f.longitude();

            results.add(new Facility(
                    f.id(),
                    f.name(),
                    f.category(),
                    f.address(),
                    distance,
                    f.phone(),
                    null,
                    f.hours(),
                    f.website(),
                    directions,
                    null,
                    List.of(new Attribution(
                            "OpenStreetMap contributors",
                            "https://www.openstreetmap.org/copyright"))));
        }

        results.sort(
                Comparator.comparing(
                        Facility::distanceMeters,
                        Comparator.nullsLast(Long::compareTo)));

        if (results.size() > 20) {
            return List.copyOf(results.subList(0, 20));
        }

        return List.copyOf(results);
    }
}