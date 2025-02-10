package com.example.calendar.model;

import jakarta.validation.constraints.NotNull;
import lombok.NonNull;

import java.util.Set;

/**
 * Represents a time slot in which the maximum number of attendees are available,
 * along with the set of available attendees.
 */
public record MaxAttendanceSlot(@NonNull @NotNull TimeSlot timeSlot,
                                @NonNull @NotNull Set<Attendee> availableAttendees) {
}
