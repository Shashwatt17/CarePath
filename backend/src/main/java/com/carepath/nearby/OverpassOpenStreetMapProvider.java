package com.carepath.nearby;

import static com.carepath.nearby.NearbyCareProvider.*;

import com.carepath.foundation.ApiFailure;
import com.fasterxml.jackson.databind.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.beans.factory.annotation.*;


public class OverpassOpenStreetMapProvider implements NearbyCareProvider {

    private final int timeout;
    private final String endpoint;
    private final ObjectMapper json;
    private final HttpClient http;

    private final Semaphore capacity = new Semaphore(2);

    @Autowired
    public OverpassOpenStreetMapProvider(
            @Value("${OVERPASS_API_URL:https://overpass-api.de/api/interpreter}") String endpoint,
            @Value("${OVERPASS_TIMEOUT_SECONDS:10}") int timeout,
            ObjectMapper json) {

        this(
            endpoint,
            timeout,
            json,
            HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build()
        );
    }

    OverpassOpenStreetMapProvider(
            String endpoint,
            int timeout,
            ObjectMapper json,
            HttpClient http) {

        if (timeout < 1 || timeout > 15) {
            throw new IllegalArgumentException("Invalid Overpass timeout");
        }

        URI uri;
        try {
            uri = URI.create(endpoint);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Overpass endpoint");
        }

        if (!"https".equalsIgnoreCase(uri.getScheme())
                || uri.getHost() == null
                || uri.getUserInfo() != null) {
            throw new IllegalArgumentException("Invalid Overpass endpoint");
        }

        this.endpoint = uri.toASCIIString();
        this.timeout = timeout;
        this.json = json.copy()
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        this.http = http;
    }

    @Override
    public boolean available() {
        return true;
    }

    static ApiFailure unavailable() {
        return new ApiFailure(
                503,
                "PLACES_UNAVAILABLE",
                "Nearby Care is unavailable. Check provider configuration or try again later."
        );
    }

    static String filter(Category category) {
        return switch (category) {
            case HOSPITAL ->
                "[\"amenity\"=\"hospital\"]";

            case CLINIC ->
                "[\"amenity\"=\"clinic\"]";

            case PHARMACY ->
                "[\"amenity\"=\"pharmacy\"]";

            case DIAGNOSTIC_CENTER ->
                "[\"healthcare\"=\"laboratory\"]";
        };
    }

    String query(Search s) {
        String f = filter(s.category());

        return "[out:json][timeout:" + timeout + "];"
                + "("
                + "nwr" + f + "(around:"
                + s.radiusMeters() + ","
                + s.latitude() + ","
                + s.longitude() + ");"
                + ");"
                + "out center tags;";
    }

    @Override
    public List<Facility> search(Search s) {

        if (!capacity.tryAcquire()) {
            throw unavailable();
        }

        try {
            String body =
                    "data=" + URLEncoder.encode(
                            query(s),
                            StandardCharsets.UTF_8
                    );

            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(timeout))
                    .header(
                            "X-Request-ID",
                            com.carepath.foundation.RequestCorrelation.current()
                    )
                    .header(
                            "Content-Type",
                            "application/x-www-form-urlencoded; charset=UTF-8"
                    )
                    .header(
                            "User-Agent",
                            "CarePath-NearbyCare/1.0"
                    )
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            CompletableFuture<HttpResponse<byte[]>> future =
                    http.sendAsync(
                            request,
                            info -> new LimitedBody(524288)
                    );

            HttpResponse<byte[]> response;

            try {
                response = future.get(timeout, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                future.cancel(true);
                Thread.currentThread().interrupt();
                throw unavailable();
            } catch (Exception e) {
                future.cancel(true);
                throw unavailable();
            }

            if (response.statusCode() != 200) {
                throw unavailable();
            }

            return normalize(
                    json.readTree(response.body()),
                    s
            );

        } catch (ApiFailure e) {
            throw e;
        } catch (Exception e) {
            throw unavailable();
        } finally {
            capacity.release();
        }
    }

    static String text(JsonNode node, int max) {
        return node != null
                && node.isTextual()
                && !node.textValue().isBlank()
                && node.textValue().length() <= max
                ? node.textValue()
                : null;
    }

    List<Facility> normalize(JsonNode root, Search s) {

        if (root == null || !root.isObject()) {
            throw unavailable();
        }

        JsonNode elements = root.get("elements");

if (elements == null || !elements.isArray()) {
    throw unavailable();
}

        List<Facility> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (JsonNode element : elements) {

            if (out.size() >= 20) {
                break;
            }

            String osmType = text(element.path("type"), 16);

            if (osmType == null || !element.path("id").canConvertToLong()) {
                continue;
            }

            String id = osmType + "/" + element.path("id").asLong();

            if (!seen.add(id)) {
                continue;
            }

            JsonNode tags = element.path("tags");

            if (!tags.isObject()) {
                continue;
            }

            String name = text(tags.path("name"), 500);

            if (name == null) {
                name = text(tags.path("operator"), 500);
            }

            if (name == null) {
                continue;
            }

            Double latitude = coordinate(element, "lat", "lat");
            Double longitude = coordinate(element, "lon", "lon");

            JsonNode center = element.path("center");

            if ((latitude == null || longitude == null) && center.isObject()) {
                latitude = coordinate(center, "lat", "lat");
                longitude = coordinate(center, "lon", "lon");
            }

            Long distance = null;
            String directions = null;

            if (latitude != null && longitude != null) {
                distance = PlaceSafety.distance(
                        s.latitude(),
                        s.longitude(),
                        latitude,
                        longitude
                );

                if (distance > s.radiusMeters()) {
                    continue;
                }

                directions =
                        "https://www.openstreetmap.org/directions"
                        + "?engine=fossgis_osrm_car"
                        + "&route="
                        + s.latitude() + "%2C" + s.longitude()
                        + "%3B"
                        + latitude + "%2C" + longitude;
            }

            String address = address(tags);

            String phone = first(
                    text(tags.path("contact:phone"), 40),
                    text(tags.path("phone"), 40)
            );

            String website = first(
                    text(tags.path("contact:website"), 2048),
                    text(tags.path("website"), 2048)
            );

            String openingHours =
                    text(tags.path("opening_hours"), 500);

            List<String> hours =
                    openingHours == null
                            ? List.of()
                            : List.of(openingHours);

            List<Attribution> attributions =
                    List.of(
                            new Attribution(
                                    "OpenStreetMap contributors",
                                    "https://www.openstreetmap.org/copyright"
                            )
                    );

            out.add(
                    new Facility(
                            id,
                            name,
                            s.category(),
                            address,
                            distance,
                            PlaceSafety.phone(phone),
                            null,
                            hours,
                            PlaceSafety.web(website),
                            directions,
                            null,
                            attributions
                    )
            );
        }

        out.sort(
                Comparator.comparing(
                        Facility::distanceMeters,
                        Comparator.nullsLast(Long::compareTo)
                )
        );

        return List.copyOf(out);
    }

    private static Double coordinate(
            JsonNode node,
            String field,
            String ignored) {

        JsonNode value = node.path(field);

        if (!value.isNumber()) {
            return null;
        }

        double d = value.doubleValue();

        if (!Double.isFinite(d)) {
            return null;
        }

        if ("lat".equals(field) && Math.abs(d) > 90) {
            return null;
        }

        if ("lon".equals(field) && Math.abs(d) > 180) {
            return null;
        }

        return d;
    }

    private static String first(String a, String b) {
        return a != null ? a : b;
    }

    private static String address(JsonNode tags) {

        String full = text(tags.path("addr:full"), 1000);

        if (full != null) {
            return full;
        }

        List<String> parts = new ArrayList<>();

        add(parts, text(tags.path("addr:housenumber"), 100));
        add(parts, text(tags.path("addr:street"), 300));
        add(parts, text(tags.path("addr:suburb"), 300));
        add(parts, text(tags.path("addr:city"), 300));
        add(parts, text(tags.path("addr:state"), 300));
        add(parts, text(tags.path("addr:postcode"), 50));

        return parts.isEmpty()
                ? null
                : String.join(", ", parts);
    }

    private static void add(List<String> parts, String value) {
        if (value != null) {
            parts.add(value);
        }
    }
}