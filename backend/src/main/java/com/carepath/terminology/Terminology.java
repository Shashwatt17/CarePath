package com.carepath.terminology;
import java.util.*;
import java.math.BigDecimal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
/** Bounded immutable snapshot. Deploy/restart after versioned terminology migrations; no per-row SQL. */
@Component
@org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization
public class Terminology {
    public record Concept(UUID id,String name,String unit,String identifierSystem,String standardIdentifier,String version,String source) {}
    public record Rule(UUID id,UUID concept,String source,String target,BigDecimal factor,String version) {}
    public record Mapping(String originalTerm,String status,Concept concept,List<Concept> alternatives,String method) {}
    private final List<Concept> concepts;private final Map<String,List<Concept>> aliases;private final List<Rule> rules;
    public Terminology(JdbcTemplate jdbc) {
        concepts=List.copyOf(jdbc.query("SELECT * FROM canonical_medical_concept WHERE active=true ORDER BY canonical_name LIMIT 1001",(r,n)->new Concept(r.getObject("id",UUID.class),r.getString("canonical_name"),r.getString("canonical_unit"),r.getString("identifier_system"),r.getString("identifier_code"),r.getString("vocabulary_version"),r.getString("mapping_source"))));
        if(concepts.size()>1000) throw new IllegalStateException("Terminology capacity exceeded");
        Map<UUID,Concept> byId=new HashMap<>();for(var c:concepts) byId.put(c.id(),c);
        Map<String,List<Concept>> index=new HashMap<>();
        var rows=jdbc.queryForList("SELECT concept_id,normalized_alias FROM concept_alias WHERE reviewed=true AND context_key='' LIMIT 5001");
        if(rows.size()>5000) throw new IllegalStateException("Alias capacity exceeded");
        for(var row:rows) { var c=byId.get((UUID)row.get("concept_id"));if(c!=null) index.computeIfAbsent(key((String)row.get("normalized_alias")),k->new ArrayList<>()).add(c); }
        Map<String,List<Concept>> immutable=new HashMap<>();index.forEach((k,v)->immutable.put(k,List.copyOf(v)));aliases=Map.copyOf(immutable);
        rules=List.copyOf(jdbc.query("SELECT * FROM unit_conversion_rule WHERE reviewed=true AND rule_version='carepath-units-v1' AND offset_value=0 LIMIT 5001",(r,n)->new Rule(r.getObject("id",UUID.class),r.getObject("concept_id",UUID.class),r.getString("source_unit"),r.getString("target_unit"),r.getBigDecimal("multiplier"),r.getString("rule_version"))));
        if(rules.size()>5000) throw new IllegalStateException("Unit rule capacity exceeded");
    }
    public static String key(String term) { return term==null?"":term.strip().replaceAll("\\s+"," ").toLowerCase(Locale.ROOT); }
    public List<Concept> concepts() { return concepts; }
    public Optional<Concept> byId(UUID id) { return concepts.stream().filter(c->c.id().equals(id)).findFirst(); }
    public Mapping normalize(String term) {
        var exact=concepts.stream().filter(c->key(c.name()).equals(key(term))).toList();
        if(exact.size()==1) return new Mapping(term,"EXACT",exact.getFirst(),List.of(),"CURATED_EXACT_V1");
        var matches=aliases.getOrDefault(key(term),List.of()).stream().distinct().toList();
        return new Mapping(term,matches.isEmpty()?"UNMAPPED":matches.size()==1?"ALIAS_MATCH":"AMBIGUOUS",matches.size()==1?matches.getFirst():null,matches.size()>1?matches:List.of(),"CURATED_ALIAS_V1");
    }
    public Optional<Rule> rule(UUID concept,String unit) { var target=byId(concept).map(Concept::unit).orElse(null);var matches=rules.stream().filter(r->r.concept().equals(concept) && r.source().equals(unit) && Objects.equals(r.target(),target)).toList();return matches.size()==1?Optional.of(matches.getFirst()):Optional.empty(); }
}
