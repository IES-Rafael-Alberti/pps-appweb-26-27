package com.vetclinic.adoption;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.vetclinic.common.EnumSupport;
import java.util.Locale;

public enum AdoptionStatus {
    /** The animal went home with the adopter. */
    COMPLETED,
    /** The agreement was undone, the animal is available again. */
    CANCELED,
    /** The adopter brought the animal back to the shelter. */
    RETURNED;

    @JsonCreator
    public static AdoptionStatus from(String value) {
        return EnumSupport.parse(AdoptionStatus.class, value);
    }

    @JsonValue
    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }
}
