package com.example.calendar.model;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a meeting attendee.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Attendee {
    @NonNull
    @NotNull
    private String name;

    @NonNull
    @NotNull
    private ZoneId timeZone;

    @NonNull
    @NotNull
    private WorkingHours workingHours;

    @Builder.Default
    private List<Appointment> appointments = new ArrayList<>();

    /**
     * Adds an appointment to the attendee's schedule.
     *
     * @param appointment the appointment to add
     */
    public void addAppointment(@NonNull Appointment appointment) {
        appointments.add(appointment);
    }
}
