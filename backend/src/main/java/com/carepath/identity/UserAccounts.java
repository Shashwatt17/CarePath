package com.carepath.identity;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UserAccounts extends JpaRepository<UserAccount, UUID> {
    Optional<UserAccount> findByEmail(String email);
}
