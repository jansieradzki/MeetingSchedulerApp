package com.example.calendar.service;

import com.example.calendar.model.Appointment;
import com.example.calendar.model.Attendee;
import com.example.calendar.model.MaxAttendanceSlot;
import com.example.calendar.model.TimeSlot;
import com.example.calendar.model.WorkingHours;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MeetingScheduler Tests")
class MeetingSchedulerTest {

    private final ZoneId warsawZone = ZoneId.of("Europe/Warsaw");
    private final ZoneId newYorkZone = ZoneId.of("America/New_York");

    private final ZonedDateTime globalTimeframeStart = ZonedDateTime.of(LocalDate.of(2025, 3, 24), LocalTime.of(8, 0), ZoneId.of("UTC"));
    private final ZonedDateTime globalTimeframeEnd = ZonedDateTime.of(LocalDate.of(2025, 3, 29), LocalTime.of(18, 0), ZoneId.of("UTC"));

    @Nested
    @DisplayName("ComputeFreeSlots Tests")
    class ComputeFreeSlotsTests {

        @Test
        @DisplayName("givenAttendeeWithNoAppointments_whenComputeFreeSlots_thenFirstProposalIsOneHourFromStart")
        void testNoAppointments() {
            // Given
            Attendee attendee = Attendee.builder()
                    .name("Alice")
                    .timeZone(warsawZone)
                    .workingHours(new WorkingHours(LocalTime.of(9, 0), LocalTime.of(17, 0)))
                    .build();
            // Restrict timeframe to a single day (March 24, 2025)
            ZonedDateTime dayStart = ZonedDateTime.of(LocalDate.of(2025, 3, 24), LocalTime.of(8, 0), ZoneId.of("UTC"));
            ZonedDateTime dayEnd   = ZonedDateTime.of(LocalDate.of(2025, 3, 24), LocalTime.of(18, 0), ZoneId.of("UTC"));
            MeetingScheduler scheduler = new MeetingScheduler();

            // When
            List<TimeSlot> proposals = scheduler.findCommonAvailableSlots(
                    List.of(attendee),
                    dayStart,
                    dayEnd,
                    Duration.ofHours(1),
                    5
            );

            // Then
            assertFalse(proposals.isEmpty());
            // For an attendee in Warsaw, working hours are 09:00–17:00 local,
            // which (assuming CET/CEST conversion) for March 24 (UTC) become 08:00–16:00 UTC.
            // With a meeting duration of 1h, the first proposal is expected to be [08:00, 09:00] UTC.
            ZonedDateTime expectedStart = ZonedDateTime.of(LocalDate.of(2025, 3, 24), LocalTime.of(8, 0), ZoneId.of("UTC"));
            ZonedDateTime expectedEnd   = ZonedDateTime.of(LocalDate.of(2025, 3, 24), LocalTime.of(9, 0), ZoneId.of("UTC"));
            assertEquals(expectedStart.toInstant(), proposals.get(0).start());
            assertEquals(expectedEnd.toInstant(), proposals.get(0).end());
        }

        @Test
        @DisplayName("givenAttendeeWithAppointments_whenComputeFreeSlots_thenSlotsAreSplit")
        void testWithAppointments() {
            // Given
            Attendee attendee = Attendee.builder()
                    .name("Bob")
                    .timeZone(newYorkZone)
                    .workingHours(new WorkingHours(LocalTime.of(8, 30), LocalTime.of(16, 30)))
                    .build();
            ZonedDateTime appStart = ZonedDateTime.of(LocalDate.of(2025, 3, 25), LocalTime.of(10, 0), newYorkZone);
            ZonedDateTime appEnd   = ZonedDateTime.of(LocalDate.of(2025, 3, 25), LocalTime.of(11, 30), newYorkZone);
            attendee.addAppointment(new Appointment(appStart, appEnd));
            // Restrict timeframe to March 25, 2025 only
            ZonedDateTime dayStart = ZonedDateTime.of(LocalDate.of(2025, 3, 25), LocalTime.of(8, 0), ZoneId.of("UTC"));
            ZonedDateTime dayEnd   = ZonedDateTime.of(LocalDate.of(2025, 3, 25), LocalTime.of(18, 0), ZoneId.of("UTC"));
            MeetingScheduler scheduler = new MeetingScheduler();

            // When
            List<TimeSlot> proposals = scheduler.findCommonAvailableSlots(
                    List.of(attendee),
                    dayStart,
                    dayEnd,
                    Duration.ofHours(1),
                    5
            );

            // Then
            // For Bob, working hours in New York: 08:30–16:30 local, which in UTC become [12:30, 20:30]
            // The appointment [10:00, 11:30] local becomes [14:00, 15:30] UTC.
            // So the free interval before the appointment is [12:30, 14:00] UTC.
            // Sliding window on [12:30, 14:00] with 1h meeting and 15min granularity should produce a proposal ending at 14:00 UTC.
            assertTrue(
                    proposals.stream()
                            .anyMatch(slot -> slot.end().equals(appStart.withZoneSameInstant(ZoneId.of("UTC")).toInstant())),
                    "Expected at least one proposal ending at the appointment start (14:00 UTC)"
            );
        }

        @Test
        @DisplayName("givenAttendeeWithFullDayAppointment_whenComputeFreeSlots_thenNoProposals")
        void testAppointmentMatchesWorkingHours() {
            // Given
            Attendee attendee = Attendee.builder()
                    .name("Charlie")
                    .timeZone(warsawZone)
                    .workingHours(new WorkingHours(LocalTime.of(9, 0), LocalTime.of(17, 0)))
                    .build();
            ZonedDateTime appStart = ZonedDateTime.of(LocalDate.of(2025, 3, 26), LocalTime.of(9, 0), warsawZone);
            ZonedDateTime appEnd   = ZonedDateTime.of(LocalDate.of(2025, 3, 26), LocalTime.of(17, 0), warsawZone);
            attendee.addAppointment(new Appointment(appStart, appEnd));
            // Restrict timeframe to March 26, 2025 only
            ZonedDateTime dayStart = ZonedDateTime.of(LocalDate.of(2025, 3, 26), LocalTime.of(8, 0), ZoneId.of("UTC"));
            ZonedDateTime dayEnd   = ZonedDateTime.of(LocalDate.of(2025, 3, 26), LocalTime.of(18, 0), ZoneId.of("UTC"));
            MeetingScheduler scheduler = new MeetingScheduler();

            // When
            List<TimeSlot> proposals = scheduler.findCommonAvailableSlots(
                    List.of(attendee),
                    dayStart,
                    dayEnd,
                    Duration.ofHours(1),
                    5
            );

            // Then
            assertTrue(proposals.isEmpty());
        }
    }

    @Nested
    @DisplayName("Intersection and CommonSlots Tests")
    class CommonAndBonusTests {

        @Test
        @DisplayName("givenAttendeesWithIdenticalWorkingHours_whenFindCommonAvailableSlots_thenCommonSlotsFound")
        void testFindCommonAvailableSlots_AllAvailable() {
            // Given
            Attendee attendee1 = Attendee.builder()
                    .name("Alice")
                    .timeZone(warsawZone)
                    .workingHours(new WorkingHours(LocalTime.of(9, 0), LocalTime.of(17, 0)))
                    .build();
            Attendee attendee2 = Attendee.builder()
                    .name("Bob")
                    .timeZone(warsawZone)
                    .workingHours(new WorkingHours(LocalTime.of(9, 0), LocalTime.of(17, 0)))
                    .build();
            MeetingScheduler scheduler = new MeetingScheduler();

            // When
            List<TimeSlot> commonSlots = scheduler.findCommonAvailableSlots(
                    List.of(attendee1, attendee2),
                    globalTimeframeStart,
                    globalTimeframeEnd,
                    Duration.ofHours(1),
                    5
            );

            // Then
            assertFalse(commonSlots.isEmpty());
        }

        @Test
        @DisplayName("givenAttendeesWithNonOverlappingWorkingHours_whenFindCommonAvailableSlots_thenNoCommonSlots")
        void testFindCommonAvailableSlots_NoCommon() {
            // Given
            Attendee attendee1 = Attendee.builder()
                    .name("Alice")
                    .timeZone(warsawZone)
                    .workingHours(new WorkingHours(LocalTime.of(9, 0), LocalTime.of(11, 0)))
                    .build();
            Attendee attendee2 = Attendee.builder()
                    .name("Bob")
                    .timeZone(warsawZone)
                    .workingHours(new WorkingHours(LocalTime.of(15, 0), LocalTime.of(17, 0)))
                    .build();
            MeetingScheduler scheduler = new MeetingScheduler();

            // When
            List<TimeSlot> commonSlots = scheduler.findCommonAvailableSlots(
                    List.of(attendee1, attendee2),
                    globalTimeframeStart,
                    globalTimeframeEnd,
                    Duration.ofHours(1),
                    5
            );

            // Then
            assertTrue(commonSlots.isEmpty());
        }

        @Test
        @DisplayName("givenAttendeesWithPartialOverlap_whenFindSlotWithMaxAttendance_thenBonusSlotReturned")
        void testFindSlotWithMaxAttendance() {
            // Given
            Attendee attendee1 = Attendee.builder()
                    .name("Alice")
                    .timeZone(warsawZone)
                    .workingHours(new WorkingHours(LocalTime.of(9, 0), LocalTime.of(17, 0)))
                    .build();
            Attendee attendee2 = Attendee.builder()
                    .name("Bob")
                    .timeZone(warsawZone)
                    .workingHours(new WorkingHours(LocalTime.of(9, 0), LocalTime.of(17, 0)))
                    .build();
            Attendee attendee3 = Attendee.builder()
                    .name("Charlie")
                    .timeZone(warsawZone)
                    .workingHours(new WorkingHours(LocalTime.of(7, 0), LocalTime.of(8, 0)))
                    .build();
            MeetingScheduler scheduler = new MeetingScheduler();

            // When
            Optional<MaxAttendanceSlot> bonusSlotOpt = scheduler.findSlotWithMaxAttendance(
                    List.of(attendee1, attendee2, attendee3),
                    globalTimeframeStart,
                    globalTimeframeEnd,
                    Duration.ofHours(1)
            );

            // Then
            assertTrue(bonusSlotOpt.isPresent());
            MaxAttendanceSlot bonusSlot = bonusSlotOpt.get();
            assertTrue(bonusSlot.availableAttendees().size() < 3);
        }
    }

    @Nested
    @DisplayName("Granularity Parameterization Tests")
    class GranularityTests {

        @ParameterizedTest(name = "givenGranularity {3} minutes, meetingDuration {2} minutes")
        @CsvSource({
                "10:00, 12:00, 30, 15",
                "10:00, 12:00, 30, 30"
        })
        void testParameterizeGranularity(String startStr, String endStr, int meetingDurationMinutes, int granularityMinutes) {
            // Given
            ZonedDateTime startZdt = ZonedDateTime.of(LocalDate.of(2025, 3, 24), LocalTime.parse(startStr), ZoneId.of("UTC"));
            ZonedDateTime endZdt = ZonedDateTime.of(LocalDate.of(2025, 3, 24), LocalTime.parse(endStr), ZoneId.of("UTC"));
            Duration meetingDuration = Duration.ofMinutes(meetingDurationMinutes);
            Duration granularity = Duration.ofMinutes(granularityMinutes);
            MeetingScheduler scheduler = new MeetingScheduler(granularity);

            // When
            List<TimeSlot> proposals = scheduler.findCommonAvailableSlots(
                    List.of(
                            Attendee.builder()
                                    .name("Alice")
                                    .timeZone(ZoneId.of("UTC"))
                                    .workingHours(new WorkingHours(LocalTime.of(10, 0), LocalTime.of(12, 0)))
                                    .build()
                    ),
                    startZdt,
                    endZdt,
                    meetingDuration,
                    20
            );

            // Then
            assertFalse(proposals.isEmpty());
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("givenTimeframeOutsideWorkingHours_whenComputeFreeSlots_thenNoProposals")
        void testNoOverlapTimeframe() {
            // Given
            Attendee attendee = Attendee.builder()
                    .name("Alice")
                    .timeZone(warsawZone)
                    .workingHours(new WorkingHours(LocalTime.of(9, 0), LocalTime.of(17, 0)))
                    .build();
            ZonedDateTime tfStart = ZonedDateTime.of(LocalDate.of(2025, 3, 24), LocalTime.of(0, 0), ZoneId.of("UTC"));
            ZonedDateTime tfEnd   = ZonedDateTime.of(LocalDate.of(2025, 3, 24), LocalTime.of(1, 0), ZoneId.of("UTC"));
            MeetingScheduler scheduler = new MeetingScheduler();

            // When
            List<TimeSlot> slots = scheduler.findCommonAvailableSlots(
                    List.of(attendee),
                    tfStart,
                    tfEnd,
                    Duration.ofHours(1),
                    5
            );

            // Then
            assertTrue(slots.isEmpty());
        }

        @Test
        @DisplayName("givenMultipleAppointments_whenComputeFreeSlots_thenCorrectFreeIntervals")
        void testMultipleAppointments() {
            // Given
            Attendee attendee = Attendee.builder()
                    .name("Bob")
                    .timeZone(warsawZone)
                    .workingHours(new WorkingHours(LocalTime.of(8, 0), LocalTime.of(18, 0)))
                    .build();
            attendee.addAppointment(new Appointment(
                    ZonedDateTime.of(LocalDate.of(2025, 3, 24), LocalTime.of(10, 0), warsawZone),
                    ZonedDateTime.of(LocalDate.of(2025, 3, 24), LocalTime.of(11, 0), warsawZone)
            ));
            attendee.addAppointment(new Appointment(
                    ZonedDateTime.of(LocalDate.of(2025, 3, 24), LocalTime.of(14, 0), warsawZone),
                    ZonedDateTime.of(LocalDate.of(2025, 3, 24), LocalTime.of(15, 0), warsawZone)
            ));
            MeetingScheduler scheduler = new MeetingScheduler();

            // When
            List<TimeSlot> slots = scheduler.findCommonAvailableSlots(
                    List.of(attendee),
                    globalTimeframeStart,
                    globalTimeframeEnd,
                    Duration.ofHours(1),
                    10
            );

            // Then
            assertFalse(slots.isEmpty());
        }
    }
}
