package com.vetclinic.appointment;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.vetclinic.common.EnumSupport;
import java.util.Locale;
import java.util.Set;

public enum AppointmentStatus {
    SCHEDULED,
    COMPLETED,
    CANCELLED;

    @JsonCreator
    public static AppointmentStatus from(String value) {
        return EnumSupport.parse(AppointmentStatus.class, value);
    }

    @JsonValue
    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Allowed state machine: scheduled can complete or cancel, cancelled can be re-booked, completed is final. */
    public boolean canTransitionTo(AppointmentStatus target) {
        return switch (this) {
            case SCHEDULED -> target == COMPLETED || target == CANCELLED;
            case CANCELLED -> target == SCHEDULED;
            case COMPLETED -> false;
        };
    }

    public static Set<AppointmentStatus> allowedTargets(AppointmentStatus current) {
        return java.util.Arrays.stream(values())
                .filter(current::canTransitionTo)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}
