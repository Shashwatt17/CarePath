package com.carepath.nearby;
import com.carepath.security.*;import jakarta.validation.Valid;import org.springframework.web.bind.annotation.*;import org.springframework.http.*;import java.util.*;
@RestController @RequestMapping("/api/v1/nearby-care")
public class NearbyCareController {
 private final NearbyCareProvider provider;private final Ownership ownership;private final AuthRateLimiter limits;
 public NearbyCareController(NearbyCareProvider p,Ownership o,AuthRateLimiter l){provider=p;ownership=o;limits=l;}
 @GetMapping("/config") public ResponseEntity<?> config(){ownership.currentOwnerId();return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("available",provider.available(),"maxRadiusMeters",20000,"provider","OpenStreetMap"));}
 @PostMapping("/search") public ResponseEntity<?> search(@Valid @RequestBody NearbyCareProvider.Search s){
  limits.nearby(ownership.currentOwnerId().toString());return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("provider","OpenStreetMap","results",provider.search(s)));
 }
}

