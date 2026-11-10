package com.carepath.profile;
import com.carepath.security.Ownership;
import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
@org.springframework.stereotype.Service
public class PatientProfileService {
 public record Profile(@NotNull @Size(max=2000) String introduction,@NotNull @Size(max=4000) String medicalHistory,@NotNull @Size(max=2000) String medications,@NotNull @Size(max=2000) String allergies,@NotNull @Size(max=2000) String reasonForVisit) {}
 private final JdbcTemplate db; private final Ownership ownership;
 public PatientProfileService(JdbcTemplate db,Ownership ownership){this.db=db;this.ownership=ownership;}
 @Transactional(readOnly=true) public Profile get(){return db.query("SELECT introduction,medical_history,medications,allergies,reason_for_visit FROM patient_profile WHERE owner_id=?",(r,n)->new Profile(r.getString(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5)),ownership.currentOwnerId()).stream().findFirst().orElse(new Profile("","","","",""));}
 @Transactional public Profile save(Profile p){
  var owner=ownership.currentOwnerId();
  // Serialize first creation and edits for this owner, including concurrent first saves.
  db.queryForObject("SELECT id FROM app_user WHERE id=? FOR UPDATE",java.util.UUID.class,owner);
  int updated=db.update("UPDATE patient_profile SET introduction=?,medical_history=?,medications=?,allergies=?,reason_for_visit=?,updated_at=CURRENT_TIMESTAMP WHERE owner_id=?",p.introduction(),p.medicalHistory(),p.medications(),p.allergies(),p.reasonForVisit(),owner);
  if(updated==0)db.update("INSERT INTO patient_profile(owner_id,introduction,medical_history,medications,allergies,reason_for_visit) VALUES (?,?,?,?,?,?)",owner,p.introduction(),p.medicalHistory(),p.medications(),p.allergies(),p.reasonForVisit());
  return p;
 }
}
