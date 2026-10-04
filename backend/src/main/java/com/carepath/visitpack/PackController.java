package com.carepath.visitpack;
import static com.carepath.visitpack.PackDtos.*;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import com.carepath.longitudinal.HistoryDtos.Page;

@RestController @Validated @RequestMapping("/api/v1/visit-packs")
public class PackController {
    private final PackService service;
    public PackController(PackService s){service=s;}
    @GetMapping public Page<Summary> list(@RequestParam(defaultValue="0") @Min(0) @Max(10000) int page){return service.list(page);}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public Pack create(@Valid @RequestBody Input x){return service.create(x);}
    @GetMapping("/{id}") public Pack get(@PathVariable UUID id){return service.get(id);}
    @PutMapping("/{id}") public Pack edit(@PathVariable UUID id,@Valid @RequestBody Input x){return service.edit(id,x);}
    @GetMapping("/{id}/preview") public Preview preview(@PathVariable UUID id){return service.preview(id);}
    @PostMapping("/{id}/generate") public Pack generate(@PathVariable UUID id,@Valid @RequestBody Generate x){return service.generate(id,x);}
    @PostMapping("/{id}/revise") public Pack revise(@PathVariable UUID id){return service.revise(id);}
    @GetMapping("/{id}/pdf") public ResponseEntity<byte[]> pdf(@PathVariable UUID id){byte[] bytes=service.download(id);return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).contentLength(bytes.length).cacheControl(CacheControl.noStore()).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=carepath-visit-pack.pdf").header("X-Content-Type-Options","nosniff").body(bytes);}
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable UUID id){service.delete(id);}
}
