package com.carepath.profile;

import com.carepath.profile.PatientProfileService.Profile;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/patient-profile")
public class PatientProfileController {
    private final PatientProfileService profiles;
    public PatientProfileController(PatientProfileService profiles) { this.profiles=profiles; }
    @GetMapping public Profile get() { return profiles.get(); }
    @PutMapping public Profile save(@Valid @RequestBody Profile profile) { return profiles.save(profile); }
}
