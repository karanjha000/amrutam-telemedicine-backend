package com.amrutam.common.security;

import java.util.Set;

/**
 * Canonical role constants for the Amrutam platform. Roles are carried in
 * JWT claims and mapped onto Spring Security authorities.
 */
public interface Roles {

    /** Patient user (self-service, appointment requests). */
    String PATIENT = "ROLE_PATIENT";

    /** Doctor user (appointment management, patient chart access). */
    String DOCTOR = "ROLE_DOCTOR";

    /** Platform administrator. */
    String ADMIN = "ROLE_ADMIN";

    /**
     * All known roles.
     *
     * @return unmodifiable set of the three canonical roles
     */
    static Set<String> allRoles() {
        return Set.of(PATIENT, DOCTOR, ADMIN);
    }
}
