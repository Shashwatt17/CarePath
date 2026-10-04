package com.carepath.sharing;

import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import com.carepath.longitudinal.HistoryDtos.Page;

@RestController
@Validated
public class ShareController {
    private final ShareService shares;
    public ShareController(ShareService shares){this.shares=shares;}
    @PostMapping("/api/v1/shares")
    ResponseEntity<ShareDtos.Created> create(@Valid @RequestBody ShareDtos.Create input){return ResponseEntity.status(201).body(shares.create(input));}
    @GetMapping("/api/v1/shares")
    Page<ShareDtos.Summary> list(@RequestParam(defaultValue="0") @Min(0) @Max(10000) int page){return shares.list(page);}
    @PostMapping("/api/v1/shares/{id}/revoke")
    ShareDtos.Summary revoke(@PathVariable UUID id){return shares.revoke(id);}
    @PostMapping("/api/v1/public/share/access")
    ShareDtos.Snapshot access(@Valid @RequestBody ShareDtos.Access input){return shares.access(input.token());}
}
