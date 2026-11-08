package com.carepath.vault;

import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

class DatabaseDocumentStorageTest {
    JdbcTemplate jdbc;
    DatabaseDocumentStorage storage;
    String key="a".repeat(32);
    @BeforeEach void setup() {
        var ds=new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=PostgreSQL;DB_CLOSE_DELAY=-1","sa","");
        jdbc=new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE document_blob(storage_key varchar(32) PRIMARY KEY, contents bytea NOT NULL)");
        storage=new DatabaseDocumentStorage(jdbc,new VaultProperties("unused",16));
    }
    @Test void persistsAcrossAdapterRecreation() throws Exception {
        byte[] data={1,2,3};storage.store(key,data);
        var restarted=new DatabaseDocumentStorage(jdbc,new VaultProperties("unused",16));
        assertTrue(restarted.exists(key));assertArrayEquals(data,restarted.retrieve(key));
    }
    @Test void rejectsOverwriteWithoutChangingOriginal() throws Exception {
        storage.store(key,new byte[]{1});assertThrows(IOException.class,()->storage.store(key,new byte[]{2}));
        assertArrayEquals(new byte[]{1},storage.retrieve(key));
    }
    @Test void deleteIsIdempotentAndMissingReadFails() throws Exception {
        storage.store(key,new byte[]{1});storage.delete(key);storage.delete(key);
        assertFalse(storage.exists(key));assertThrows(IOException.class,()->storage.retrieve(key));
    }
    @Test void validatesKeysAndSizes() {
        for(String invalid:new String[]{"../escape","A".repeat(32),"short","' OR 1=1--"}) {
            assertThrows(IOException.class,()->storage.store(invalid,new byte[]{1}));
            assertThrows(IOException.class,()->storage.retrieve(invalid));
            assertThrows(IOException.class,()->storage.delete(invalid));
            assertThrows(IOException.class,()->storage.exists(invalid));
        }
        assertThrows(IOException.class,()->storage.store(key,null));
        assertThrows(IOException.class,()->storage.store(key,new byte[0]));
        assertThrows(IOException.class,()->storage.store(key,new byte[17]));
    }
    @Test void refusesOversizedStoredBytes() {
        jdbc.update("INSERT INTO document_blob VALUES (?, ?)",key,new byte[17]);
        assertThrows(IOException.class,()->storage.retrieve(key));
    }
    @Test void rollsBackWithUploadTransaction() throws Exception {
        var tx=new TransactionTemplate(new DataSourceTransactionManager(jdbc.getDataSource()));
        tx.executeWithoutResult(s->{try {storage.store(key,new byte[]{1});} catch(IOException e){throw new RuntimeException(e);}s.setRollbackOnly();});
        assertFalse(storage.exists(key));
    }
}
