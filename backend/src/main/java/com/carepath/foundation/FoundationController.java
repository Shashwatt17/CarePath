package com.carepath.foundation;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FoundationController {
    @GetMapping("/api/v1/system/info")
    public SystemInfo info() {
        return new SystemInfo("CarePath", "0.1.0-SNAPSHOT", "PHASE_8_CARE_ORGANIZATION", false);
    }

    public record SystemInfo(String name, String version, String stage, boolean productFeaturesAvailable) {}
}
