package com.vetclinic.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.vetclinic.common.EnumSupport;
import java.util.Locale;

/**
 * Who the user is inside the clinic.
 *
 * <ul>
 *   <li>{@link #ADMIN} manages the platform, including staff and client accounts.</li>
 *   <li>{@link #MANAGER} runs the clinic: owners, pets, appointments, shop and adoptions.</li>
 *   <li>{@link #CLIENT} only sees and manages their own profile, pets, appointments and purchases.</li>
 * </ul>
 */
public enum Role {
    ADMIN,
    MANAGER,
    CLIENT;

    @JsonCreator
    public static Role from(String value) {
        return EnumSupport.parse(Role.class, value);
    }

    @JsonValue
    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }
}
