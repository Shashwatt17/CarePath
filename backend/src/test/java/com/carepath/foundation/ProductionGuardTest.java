package com.carepath.foundation;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import static org.junit.jupiter.api.Assertions.*;
class ProductionGuardTest {
 MockEnvironment valid(){var e=new MockEnvironment().withProperty("carepath.frontend-origin","https://carepath.example").withProperty("carepath.auth.frontend-origin","https://carepath.example").withProperty("carepath.auth.cookie-secure","true").withProperty("spring.datasource.url","jdbc:postgresql://db/carepath").withProperty("spring.datasource.password","synthetic-db").withProperty("spring.data.redis.password","synthetic-redis");e.setActiveProfiles("production");return e;}
 @Test void productionBeanStartsWithConsistentSecureConfiguration(){try(var c=new AnnotationConfigApplicationContext()){c.setEnvironment(valid());c.register(ProductionGuard.class);c.refresh();assertNotNull(c.getBean(ProductionGuard.class));}}
 @Test void insecureCookieRejected(){assertThrows(IllegalStateException.class,()->new ProductionGuard(valid().withProperty("carepath.auth.cookie-secure","false")));}
 @Test void localAndTestProfilesRejected(){for(String p:new String[]{"local","test"}){var e=valid();e.setActiveProfiles("production",p);assertThrows(IllegalStateException.class,()->new ProductionGuard(e));}}
 @Test void unsafeOriginsRejectedWithoutDisclosure(){for(String origin:new String[]{"http://carepath.example","https://localhost","https://carepath.example/path","https://user:secret@carepath.example","https://carepath.example?secret=x"}){var e=valid().withProperty("carepath.frontend-origin",origin).withProperty("carepath.auth.frontend-origin",origin);var error=assertThrows(IllegalStateException.class,()->new ProductionGuard(e));assertFalse(error.getMessage().contains("secret"));}}
 @Test void corsAndAuthMustAgree(){assertThrows(IllegalStateException.class,()->new ProductionGuard(valid().withProperty("carepath.auth.frontend-origin","https://other.example")));}
 @Test void realDatabaseAndCredentialsRequired(){for(String key:new String[]{"spring.datasource.url","spring.datasource.password","spring.data.redis.password"})assertThrows(IllegalStateException.class,()->new ProductionGuard(valid().withProperty(key,"")));}
 @Test void documentationMustNotBePublic(){assertThrows(IllegalStateException.class,()->new ProductionGuard(valid().withProperty("springdoc.api-docs.enabled","true")));}
}
