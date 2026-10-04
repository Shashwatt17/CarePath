package com.carepath.security;

import java.util.*;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

/** Future owned entities must have ownerId. This interface intentionally exposes no unscoped lookup. */
@NoRepositoryBean
public interface OwnerScopedRepository<T> extends Repository<T,UUID> {
    Optional<T> findByIdAndOwnerId(UUID id, UUID ownerId);
}
