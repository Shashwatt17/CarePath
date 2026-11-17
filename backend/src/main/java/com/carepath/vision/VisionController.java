package com.carepath.vision;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/documents/{id}/vision")
public class VisionController {
    private final VisionService service;
    public VisionController(VisionService service){this.service=service;}
    @GetMapping public VisionData.Status status(@PathVariable UUID id){return service.status(id);}
    @GetMapping("/{page}") public VisionData.View get(@PathVariable UUID id,@PathVariable int page){return service.get(id,page);}
    @PostMapping public VisionData.View read(@PathVariable UUID id,@RequestBody VisionData.Request request){return service.read(id,request);}
}
