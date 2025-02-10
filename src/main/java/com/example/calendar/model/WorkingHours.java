package com.example.calendar.model;

import jakarta.validation.constraints.NotNull;
import lombok.NonNull;

import java.time.LocalTime;

/**
 * Represents the working hours of an attendee.
 */
public record WorkingHours(@NonNull @NotNull LocalTime start, @NonNull @NotNull LocalTime end) {
    /**
     * Constructs WorkingHours.
     *
     * @param start the start time
     * @param end   the end time (must not be before start)
     * @throws IllegalArgumentException if end is before start
     */
    public WorkingHours(@NonNull LocalTime start, @NonNull LocalTime end) {
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("Working hours end time cannot be before start time");
        }
        this.start = start;
        this.end = end;
    }
}
