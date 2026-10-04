package com.carepath.nearby;

import static org.junit.jupiter.api.Assertions.*;
import static com.carepath.nearby.NearbyCareProvider.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NearbyTest {

    private final ObjectMapper json = new ObjectMapper();

    private LocalOpenStreetMapProvider provider() {
        var provider = new LocalOpenStreetMapProvider(json);
        provider.load();
        return provider;
    }

    @Test
    void datasetLoadsAndProviderIsAvailable() {
        var provider = provider();

        assertTrue(provider.available());
    }

    @Test
    void realDatasetContainsHealthcareResults() {
        var provider = provider();

        // Delhi coordinates are inside the Northern Zone dataset.
        var results = provider.search(
                new Search(
                        28.6139,
                        77.2090,
                        20000,
                        Category.HOSPITAL,
                        true
                )
        );

        assertFalse(results.isEmpty());
    }

    @Test
    void resultsRespectCategory() {
        var provider = provider();

        var results = provider.search(
                new Search(
                        28.6139,
                        77.2090,
                        20000,
                        Category.HOSPITAL,
                        true
                )
        );

        assertTrue(
                results.stream()
                        .allMatch(x -> x.category() == Category.HOSPITAL)
        );
    }

    @Test
    void resultsRespectRadius() {
        var provider = provider();

        int radius = 20000;

        var results = provider.search(
                new Search(
                        28.6139,
                        77.2090,
                        radius,
                        Category.HOSPITAL,
                        true
                )
        );

        assertTrue(
                results.stream()
                        .allMatch(x ->
                                x.distanceMeters() != null
                                        && x.distanceMeters() <= radius)
        );
    }

    @Test
    void resultsSortedByDistance() {
        var provider = provider();

        var results = provider.search(
                new Search(
                        28.6139,
                        77.2090,
                        20000,
                        Category.HOSPITAL,
                        true
                )
        );

        for (int i = 1; i < results.size(); i++) {
            assertTrue(
                    results.get(i - 1).distanceMeters()
                            <= results.get(i).distanceMeters()
            );
        }
    }

    @Test
    void resultCountLimitedToTwenty() {
        var provider = provider();

        var results = provider.search(
                new Search(
                        28.6139,
                        77.2090,
                        20000,
                        Category.HOSPITAL,
                        true
                )
        );

        assertTrue(results.size() <= 20);
    }

    @Test
    void attributionIsOpenStreetMap() {
        var provider = provider();

        var results = provider.search(
                new Search(
                        28.6139,
                        77.2090,
                        20000,
                        Category.HOSPITAL,
                        true
                )
        );

        assertFalse(results.isEmpty());

        var attribution =
                results.getFirst()
                        .attributions()
                        .getFirst();

        assertEquals(
                "OpenStreetMap contributors",
                attribution.name()
        );

        assertEquals(
                "https://www.openstreetmap.org/copyright",
                attribution.url()
        );
    }

    @Test
    void directionsUseOpenStreetMap() {
        var provider = provider();

        var results = provider.search(
                new Search(
                        28.6139,
                        77.2090,
                        20000,
                        Category.HOSPITAL,
                        true
                )
        );

        assertFalse(results.isEmpty());

        assertTrue(
                results.getFirst()
                        .directions()
                        .startsWith(
                                "https://www.openstreetmap.org/directions"
                        )
        );
    }

    @Test
    void providerDoesNotInventLiveAvailability() {
        var provider = provider();

        var results = provider.search(
                new Search(
                        28.6139,
                        77.2090,
                        20000,
                        Category.HOSPITAL,
                        true
                )
        );

        assertTrue(
                results.stream()
                        .allMatch(x -> x.openNow() == null)
        );
    }

    @Test
    void providerDoesNotInventBookingLinks() {
        var provider = provider();

        var results = provider.search(
                new Search(
                        28.6139,
                        77.2090,
                        20000,
                        Category.HOSPITAL,
                        true
                )
        );

        assertTrue(
                results.stream()
                        .allMatch(x -> x.bookingUrl() == null)
        );
    }

    @Test
    void distantSearchCanReturnEmptyResults() {
        var provider = provider();

        var results = provider.search(
                new Search(
                        0.0,
                        0.0,
                        1000,
                        Category.HOSPITAL,
                        true
                )
        );

        assertTrue(results.isEmpty());
    }

    @Test
    void haversineBoundaries() {
        assertEquals(
                0,
                PlaceSafety.distance(
                        90,
                        0,
                        90,
                        180
                )
        );

        assertEquals(
                20015114,
                PlaceSafety.distance(
                        0,
                        0,
                        0,
                        180
                )
        );

        assertEquals(
                222390,
                PlaceSafety.distance(
                        0,
                        179,
                        0,
                        -179
                )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "javascript:alert(1)",
            "data:text/html,x",
            "https://user:pass@example.org",
            "http://localhost/x",
            "http://127.0.0.1",
            "file:///etc/passwd"
    })
    void unsafeUrlsRejected(String value) {
        assertNull(
                PlaceSafety.web(value)
        );
    }

    @Test
    void safeWebsiteAccepted() {
        assertEquals(
                "https://example.org",
                PlaceSafety.web(
                        "https://example.org"
                )
        );
    }

    @Test
    void unsafePhoneRejected() {
        assertNull(
                PlaceSafety.phone(
                        "12345;ext=malicious"
                )
        );

        assertNull(
                PlaceSafety.phone(
                        "++12345"
                )
        );
    }

    @Test
    void safePhoneNormalized() {
        assertEquals(
                "+911234567890",
                PlaceSafety.phone(
                        "+91 12345 67890"
                )
        );
    }
}