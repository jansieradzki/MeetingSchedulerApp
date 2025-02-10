package com.example.calendar;

import com.example.calendar.model.Appointment;
import com.example.calendar.model.Attendee;
import com.example.calendar.model.MaxAttendanceSlot;
import com.example.calendar.model.TimeSlot;
import com.example.calendar.model.WorkingHours;
import com.example.calendar.service.MeetingScheduler;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Main application class – entry point.
 */
public class MeetingSchedulerApplication {

    public static void main(String[] args) {
        // Define the overall timeframe: from March 24, 2025 08:00 UTC to March 29, 2025 18:00 UTC
        ZonedDateTime timeframeStart = ZonedDateTime.of(LocalDate.of(2025, 3, 24), LocalTime.of(8, 0), ZoneId.of("UTC"));
        ZonedDateTime timeframeEnd = ZonedDateTime.of(LocalDate.of(2025, 3, 29), LocalTime.of(18, 0), ZoneId.of("UTC"));
        Duration meetingDuration = Duration.ofHours(1);
        int numberOfSlots = 3;

        // Create sample attendees
        Attendee alice = Attendee.builder()
                .name("Alice")
                .timeZone(ZoneId.of("Europe/Warsaw"))
                .workingHours(new WorkingHours(LocalTime.of(9, 0), LocalTime.of(17, 0)))
                .build();
        // Appointment for Alice: March 24, 2025, 13:00–14:00 (local time)
        alice.addAppointment(new Appointment(
                ZonedDateTime.of(LocalDate.of(2025, 3, 24), LocalTime.of(13, 0), ZoneId.of("Europe/Warsaw")),
                ZonedDateTime.of(LocalDate.of(2025, 3, 24), LocalTime.of(14, 0), ZoneId.of("Europe/Warsaw"))
        ));

        Attendee bob = Attendee.builder()
                .name("Bob")
                .timeZone(ZoneId.of("America/New_York"))
                .workingHours(new WorkingHours(LocalTime.of(8, 30), LocalTime.of(16, 30)))
                .build();
        // Appointment for Bob: March 25, 2025, 10:00–11:30 (local time)
        bob.addAppointment(new Appointment(
                ZonedDateTime.of(LocalDate.of(2025, 3, 25), LocalTime.of(10, 0), ZoneId.of("America/New_York")),
                ZonedDateTime.of(LocalDate.of(2025, 3, 25), LocalTime.of(11, 30), ZoneId.of("America/New_York"))
        ));

        List<Attendee> attendees = Arrays.asList(alice, bob);

        MeetingScheduler scheduler = new MeetingScheduler();
        List<TimeSlot> availableSlots = scheduler.findCommonAvailableSlots(attendees, timeframeStart, timeframeEnd, meetingDuration, numberOfSlots);

        if (!availableSlots.isEmpty()) {
            System.out.println("Available slots (for all attendees):");
            availableSlots.forEach(System.out::println);
        } else {
            System.out.println("No common slot found. Searching for a slot with maximum attendance.");
            Optional<MaxAttendanceSlot> maxAttendanceSlot = scheduler.findSlotWithMaxAttendance(attendees, timeframeStart, timeframeEnd, meetingDuration);
            if (maxAttendanceSlot.isPresent()) {
                MaxAttendanceSlot slot = maxAttendanceSlot.get();
                System.out.println("Slot with maximum attendance: " + slot.timeSlot());
                String availableNames = slot.availableAttendees().stream()
                        .map(Attendee::getName)
                        .collect(Collectors.joining(", "));
                System.out.println("Available attendees: " + availableNames);
            } else {
                System.out.println("No suitable slot found.");
            }
        }
    }
}
