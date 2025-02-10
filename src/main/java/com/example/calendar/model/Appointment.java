package com.example.calendar.model;

import jakarta.validation.constraints.NotNull;
import lombok.NonNull;

import java.time.ZonedDateTime;

/**
 * Represents an already booked appointment.
 */
public record Appointment(@NonNull @NotNull ZonedDateTime start, @NonNull @NotNull ZonedDateTime end) {
    /**
     * Constructs an Appointment.
     *
     * @param start the start time
     * @param end   the end time (must not be before start)
     * @throws IllegalArgumentException if end is before start
     */
    public Appointment(@NonNull ZonedDateTime start, @NonNull ZonedDateTime end) {
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End time cannot be before start time.");
        }
        this.start = start;
        this.end = end;
    }
}
