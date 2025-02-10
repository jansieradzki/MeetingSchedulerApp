package com.example.calendar.model;

import jakarta.validation.constraints.NotNull;
import lombok.NonNull;

import java.time.Duration;
import java.time.Instant;

/**
 * Represents a time interval (either free or proposed).
 */
public record TimeSlot(@NonNull @NotNull Instant start, @NonNull @NotNull Instant end) {
    /**
     * Constructs a TimeSlot.
     *
     * @param start the start instant
     * @param end   the end instant (must be after start)
     * @throws IllegalArgumentException if end is before start
     */
    public TimeSlot(@NonNull Instant start, @NonNull Instant end) {
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        this.start = start;
        this.end = end;
    }

    /**
     * Returns the duration of the time slot.
     *
     * @return the duration between start and end
     */
    public Duration getDuration() {
        return Duration.between(start, end);
    }
}
