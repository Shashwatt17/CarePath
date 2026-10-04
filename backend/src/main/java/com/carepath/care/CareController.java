package com.carepath.care;
import static com.carepath.care.CareDtos.*;import java.util.*;import jakarta.validation.Valid;import jakarta.validation.constraints.*;import org.springframework.validation.annotation.Validated;import org.springframework.web.bind.annotation.*;import org.springframework.http.HttpStatus;import com.carepath.longitudinal.HistoryDtos.Page;
@RestController @RequestMapping("/api/v1/care") @Validated
public class CareController {
 private final SymptomService symptoms;private final AppointmentService appointments;private final FollowUpService follow;private final ReminderService reminders;
 public CareController(SymptomService s,AppointmentService a,FollowUpService f,ReminderService r){symptoms=s;appointments=a;follow=f;reminders=r;}
 @GetMapping("/symptoms") public Page<Symptom> symptoms(@RequestParam(defaultValue="ALL") String status,@RequestParam(defaultValue="") @Size(max=80) String q,@RequestParam(defaultValue="0") @Min(0) @Max(10000) int page){return symptoms.list(status,q,page);}
 @GetMapping("/symptoms/{id}") public Symptom symptom(@PathVariable UUID id){return symptoms.get(id);}
 @PostMapping("/symptoms") @ResponseStatus(HttpStatus.CREATED) public Symptom create(@Valid @RequestBody SymptomInput x){return symptoms.save(null,x);}
 @PutMapping("/symptoms/{id}") public Symptom edit(@PathVariable UUID id,@Valid @RequestBody SymptomInput x){return symptoms.save(id,x);}
 @DeleteMapping("/symptoms/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteSymptom(@PathVariable UUID id){symptoms.delete(id);}
 @GetMapping("/appointments") public Page<Appointment> appointments(@RequestParam(defaultValue="ALL") String scope,@RequestParam(defaultValue="0") @Min(0) @Max(10000) int page){return appointments.list(scope,page);}
 @GetMapping("/appointments/{id}") public Appointment appointment(@PathVariable UUID id){return appointments.get(id);}
 @PostMapping("/appointments") @ResponseStatus(HttpStatus.CREATED) public Appointment create(@Valid @RequestBody AppointmentInput x){return appointments.save(null,x);}
 @PutMapping("/appointments/{id}") public Appointment edit(@PathVariable UUID id,@Valid @RequestBody AppointmentInput x){return appointments.save(id,x);}
 @PostMapping("/appointments/{id}/status") public Appointment status(@PathVariable UUID id,@Valid @RequestBody Action x){return appointments.action(id,x);}
 @DeleteMapping("/appointments/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteAppointment(@PathVariable UUID id){appointments.delete(id);}
 @PostMapping("/documents/{id}/detect-follow-ups") @ResponseStatus(HttpStatus.NO_CONTENT) public void detect(@PathVariable UUID id){follow.detectOwned(id);}
 @GetMapping("/follow-ups") public Page<FollowUp> followUps(@RequestParam(defaultValue="ALL") String scope,@RequestParam(defaultValue="0") @Min(0) @Max(10000) int page){return follow.list(page,scope);}
 @GetMapping("/follow-ups/{id}") public FollowUp followUp(@PathVariable UUID id){return follow.get(id);}
 @PostMapping("/follow-ups/{id}/decision") public FollowUp decide(@PathVariable UUID id,@Valid @RequestBody FollowAction x){return follow.decide(id,x);}
 @GetMapping("/reminders") public Page<Reminder> reminders(@RequestParam(defaultValue="ALL") String status,@RequestParam(defaultValue="0") @Min(0) @Max(10000) int page){return reminders.list(page,status);}
 @GetMapping("/notifications") public Page<Notice> notifications(@RequestParam(defaultValue="0") @Min(0) @Max(10000) int page){return reminders.notifications(page);}
 @GetMapping("/notifications/unread") public Map<String,Long> unread(){return Map.of("count",reminders.unread());}
 @PostMapping("/notifications/{id}/read") @ResponseStatus(HttpStatus.NO_CONTENT) public void read(@PathVariable UUID id){reminders.read(id);}
 @PostMapping("/notifications/read-all") @ResponseStatus(HttpStatus.NO_CONTENT) public void allRead(){reminders.read(null);}
}
