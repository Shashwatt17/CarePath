package com.carepath.vault;
import static com.carepath.vault.DocumentDtos.*;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.*;
import java.nio.charset.StandardCharsets;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
@RestController @RequestMapping("/api/v1/documents")
public class DocumentController {
    private final DocumentService service;private final VaultProperties properties;
    public DocumentController(DocumentService service,VaultProperties properties) { this.service=service;this.properties=properties; }
    @GetMapping("/config") public Map<String,Object> config() { return Map.of("maxBytes",properties.maxBytes(),"mimeTypes",List.of("application/pdf","image/jpeg","image/png")); }
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<View> upload(@RequestPart("file") MultipartFile file,@Valid @RequestPart("metadata") Metadata metadata) {
        var result=service.upload(file,metadata);return ResponseEntity.status(201).body(result);
    }
    @GetMapping public Page list(@RequestParam(required=false) Category type,@RequestParam(required=false) State status,
        @RequestParam(required=false) String filename,@RequestParam(required=false) String provider,
        @RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to,
        @RequestParam(required=false) LocalDate uploadedFrom,@RequestParam(required=false) LocalDate uploadedTo,
        @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return service.list(new Filters(type,status,filename,provider,from,to,uploadedFrom,uploadedTo,page,size));
    }
    @GetMapping("/{id}") public View metadata(@PathVariable UUID id) { return service.metadata(id); }
    @PutMapping("/{id}") public View update(@PathVariable UUID id,@Valid @RequestBody Update request) { return service.update(id,request); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable UUID id) { return ResponseEntity.status(service.delete(id)?204:202).build(); }
    @GetMapping("/{id}/download") public ResponseEntity<byte[]> download(@PathVariable UUID id) { return file(service.content(id,false,1),false); }
    @GetMapping("/{id}/preview") public ResponseEntity<byte[]> preview(@PathVariable UUID id,@RequestParam(defaultValue="1") int page) { return file(service.content(id,true,page),true); }
    private ResponseEntity<byte[]> file(DocumentService.Content file,boolean inline) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.mime())).contentLength(file.bytes().length)
            .header(HttpHeaders.CONTENT_DISPOSITION,inline?"inline":ContentDisposition.attachment().filename(file.filename(),StandardCharsets.UTF_8).build().toString())
            .cacheControl(CacheControl.noStore()).header("Pragma","no-cache").body(file.bytes());
    }
}
