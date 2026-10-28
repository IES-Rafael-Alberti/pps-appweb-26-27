package com.vetclinic.pet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.vetclinic.common.EnumSupport;
import java.util.Locale;

/**
 * @see Pet#getStatus()
 */
public enum PetStatus {
    /** Shelter animal waiting for a home. Has no owner and can be adopted. */
    AVAILABLE,
    /** Belongs to a registered client. */
    OWNED,
    /** Was adopted from the shelter; the adopter is now the owner. */
    ADOPTED;

    @JsonCreator
    public static PetStatus from(String value) {
        return EnumSupport.parse(PetStatus.class, value);
    }

    @JsonValue
    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }
}
