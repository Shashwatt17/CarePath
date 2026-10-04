package com.carepath.security;

import com.carepath.foundation.ApiFailure;
import com.carepath.identity.CarePrincipal;
import java.util.*;
import java.util.function.BiFunction;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class Ownership {
    public UUID currentOwnerId() {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if (auth==null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof CarePrincipal principal)) throw ApiFailure.session();
        return principal.userId();
    }
    // Pass repository::findByIdAndOwnerId. Do not provide an unscoped findById callback.
    public <T> T require(UUID recordId, BiFunction<UUID,UUID,Optional<T>> ownerScopedLookup) {
        return ownerScopedLookup.apply(recordId,currentOwnerId())
            .orElseThrow(() -> new ApiFailure(404,"NOT_FOUND","The requested resource was not found."));
    }
}
