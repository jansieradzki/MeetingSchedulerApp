package com.example.calendar.service;

import com.example.calendar.model.Appointment;
import com.example.calendar.model.Attendee;
import com.example.calendar.model.MaxAttendanceSlot;
import com.example.calendar.model.TimeSlot;
import com.example.calendar.model.WorkingHours;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service responsible for computing free time slots and selecting common meeting slots.
 */
public class MeetingScheduler {

    private final Duration slotGranularity;

    public MeetingScheduler() {
        this(Duration.ofMinutes(15));
    }

    public MeetingScheduler(Duration slotGranularity) {
        this.slotGranularity = slotGranularity;
    }

    /**
     * Finds common available time slots for all attendees.
     *
     * @param attendees       list of attendees
     * @param timeframeStart  start of the search timeframe
     * @param timeframeEnd    end of the search timeframe
     * @param meetingDuration required meeting duration
     * @param numberOfSlots   maximum number of meeting proposals to return
     * @return list of common available time slots
     */
    public List<TimeSlot> findCommonAvailableSlots(List<Attendee> attendees,
                                                   ZonedDateTime timeframeStart,
                                                   ZonedDateTime timeframeEnd,
                                                   Duration meetingDuration,
                                                   int numberOfSlots) {
        List<List<TimeSlot>> freeSlotsPerAttendee = attendees.stream()
                .map(attendee -> computeFreeSlotsForAttendee(attendee, timeframeStart, timeframeEnd))
                .toList();

        List<TimeSlot> commonFreeSlots = freeSlotsPerAttendee.stream()
                .reduce(this::intersectTimeSlotsTwoPointer)
                .orElse(Collections.emptyList());

        return commonFreeSlots.stream()
                .flatMap(interval -> generateMeetingSlotsFromInterval(interval, meetingDuration, slotGranularity).stream())
                .limit(numberOfSlots)
                .collect(Collectors.toList());
    }

    /**
     * Finds a time slot with maximum attendance using a sweep-line algorithm.
     *
     * @param attendees       list of attendees
     * @param timeframeStart  start of the search timeframe
     * @param timeframeEnd    end of the search timeframe
     * @param meetingDuration required meeting duration
     * @return an Optional containing the MaxAttendanceSlot if found
     */
    public Optional<MaxAttendanceSlot> findSlotWithMaxAttendance(List<Attendee> attendees,
                                                                 ZonedDateTime timeframeStart,
                                                                 ZonedDateTime timeframeEnd,
                                                                 Duration meetingDuration) {
        List<TimeSlotWithAttendee> allSlots = attendees.stream()
                .flatMap(attendee -> computeFreeSlotsForAttendee(attendee, timeframeStart, timeframeEnd)
                        .stream()
                        .map(slot -> new TimeSlotWithAttendee(slot, attendee)))
                .toList();

        List<Event> events = allSlots.stream()
                .flatMap(tsa -> Stream.of(
                        new Event(tsa.timeSlot().start(), true, tsa.attendee()),
                        new Event(tsa.timeSlot().end(), false, tsa.attendee())
                ))
                .sorted(Comparator.comparing(Event::time)
                        .thenComparing(e -> !e.isStart()))
                .toList();

        Set<Attendee> currentAttendees = new HashSet<>();
        Instant currentTime = timeframeStart.toInstant();
        MaxAttendanceSlot bestSlot = null;
        int bestCount = 0;

        for (Event event : events) {
            Instant eventTime = event.time();
            if (eventTime.isAfter(currentTime)) {
                if (!currentTime.isBefore(timeframeEnd.toInstant())) break;
                Instant intervalEnd = eventTime.isBefore(timeframeEnd.toInstant()) ? eventTime : timeframeEnd.toInstant();
                if (Duration.between(currentTime, intervalEnd).compareTo(meetingDuration) >= 0) {
                    int count = currentAttendees.size();
                    if (count > bestCount) {
                        TimeSlot candidateSlot = new TimeSlot(currentTime, currentTime.plus(meetingDuration));
                        bestSlot = new MaxAttendanceSlot(candidateSlot, new HashSet<>(currentAttendees));
                        bestCount = count;
                    }
                }
            }
            if (event.isStart()) {
                currentAttendees.add(event.attendee());
            } else {
                currentAttendees.remove(event.attendee());
            }
            currentTime = event.time();
        }
        return Optional.ofNullable(bestSlot);
    }

    /**
     * Computes free time slots for a given attendee within the specified timeframe.
     *
     * @param attendee       the attendee
     * @param timeframeStart start of the timeframe
     * @param timeframeEnd   end of the timeframe
     * @return list of free time slots
     */
    private List<TimeSlot> computeFreeSlotsForAttendee(Attendee attendee,
                                                       ZonedDateTime timeframeStart,
                                                       ZonedDateTime timeframeEnd) {
        ZoneId zone = attendee.getTimeZone();
        ZonedDateTime localStart = timeframeStart.withZoneSameInstant(zone);
        ZonedDateTime localEnd = timeframeEnd.withZoneSameInstant(zone);
        LocalDate startDate = localStart.toLocalDate();
        LocalDate endDate = localEnd.toLocalDate();

        return Stream.iterate(startDate, date -> date.plusDays(1))
                .limit(ChronoUnit.DAYS.between(startDate, endDate) + 1)
                .map(date -> {
                    WorkingHours workingHours = attendee.getWorkingHours();
                    ZonedDateTime workStart = ZonedDateTime.of(date, workingHours.start(), zone);
                    ZonedDateTime workEnd = ZonedDateTime.of(date, workingHours.end(), zone);
                    ZonedDateTime periodStart = maxZonedDateTime(workStart, localStart);
                    ZonedDateTime periodEnd = minZonedDateTime(workEnd, localEnd);
                    if (periodStart.isBefore(periodEnd)) {
                        TimeSlot workingPeriod = new TimeSlot(periodStart.toInstant(), periodEnd.toInstant());
                        List<Appointment> dayAppointments = attendee.getAppointments().stream()
                                .filter(appt -> {
                                    ZonedDateTime apptStart = appt.start().withZoneSameInstant(zone);
                                    ZonedDateTime apptEnd = appt.end().withZoneSameInstant(zone);
                                    return !(apptEnd.isBefore(periodStart) || apptStart.isAfter(periodEnd));
                                })
                                .sorted(Comparator.comparing(Appointment::start))
                                .collect(Collectors.toList());
                        return subtractAppointmentsFromSlot(workingPeriod, dayAppointments);
                    }
                    return Collections.<TimeSlot>emptyList();
                })
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    /**
     * Subtracts overlapping appointments from a working period to produce free time slots.
     *
     * @param workingPeriod the working period
     * @param appointments  list of appointments overlapping with the working period
     * @return list of free time slots
     */
    private List<TimeSlot> subtractAppointmentsFromSlot(TimeSlot workingPeriod, List<Appointment> appointments) {
        List<TimeSlot> freeSlots = new ArrayList<>();
        Instant currentStart = workingPeriod.start();
        for (Appointment appointment : appointments) {
            Instant apptStart = appointment.start().toInstant();
            Instant apptEnd = appointment.end().toInstant();
            if (apptStart.isAfter(currentStart)) {
                freeSlots.add(new TimeSlot(currentStart, apptStart));
            }
            if (apptEnd.isAfter(currentStart)) {
                currentStart = apptEnd;
            }
            if (!currentStart.isBefore(workingPeriod.end())) {
                break;
            }
        }
        if (currentStart.isBefore(workingPeriod.end())) {
            freeSlots.add(new TimeSlot(currentStart, workingPeriod.end()));
        }
        return freeSlots;
    }

    /**
     * Computes the intersection between two lists of time slots using a two-pointer algorithm.
     * Both input lists are sorted by start time.
     *
     * @param slots1 first list of time slots
     * @param slots2 second list of time slots
     * @return list of intersecting time slots
     */
    private List<TimeSlot> intersectTimeSlotsTwoPointer(List<TimeSlot> slots1, List<TimeSlot> slots2) {
        List<TimeSlot> intersections = new ArrayList<>();
        List<TimeSlot> sortedSlots1 = slots1.stream()
                .sorted(Comparator.comparing(TimeSlot::start))
                .toList();
        List<TimeSlot> sortedSlots2 = slots2.stream()
                .sorted(Comparator.comparing(TimeSlot::start))
                .toList();

        int pointer1 = 0, pointer2 = 0;
        while (pointer1 < sortedSlots1.size() && pointer2 < sortedSlots2.size()) {
            TimeSlot slot1 = sortedSlots1.get(pointer1);
            TimeSlot slot2 = sortedSlots2.get(pointer2);
            Instant maxStart = slot1.start().isAfter(slot2.start()) ? slot1.start() : slot2.start();
            Instant minEnd = slot1.end().isBefore(slot2.end()) ? slot1.end() : slot2.end();
            if (!maxStart.isAfter(minEnd)) {
                intersections.add(new TimeSlot(maxStart, minEnd));
            }
            if (slot1.end().isBefore(slot2.end())) {
                pointer1++;
            } else {
                pointer2++;
            }
        }
        return intersections;
    }

    /**
     * Generates meeting slot proposals from a free interval using a streaming approach.
     *
     * @param interval        the free interval
     * @param meetingDuration required meeting duration
     * @param granularity     step size for shifting the meeting window
     * @return list of proposed meeting time slots
     */
    private List<TimeSlot> generateMeetingSlotsFromInterval(TimeSlot interval, Duration meetingDuration, Duration granularity) {
        return Stream.iterate(interval.start(), start -> !start.plus(meetingDuration).isAfter(interval.end()), start -> start.plus(granularity))
                .map(start -> new TimeSlot(start, start.plus(meetingDuration)))
                .collect(Collectors.toList());
    }

    /**
     * Returns the later of two ZonedDateTime values.
     *
     * @param dt1 first date/time
     * @param dt2 second date/time
     * @return the later ZonedDateTime
     */
    private ZonedDateTime maxZonedDateTime(ZonedDateTime dt1, ZonedDateTime dt2) {
        return dt1.isAfter(dt2) ? dt1 : dt2;
    }

    /**
     * Returns the earlier of two ZonedDateTime values.
     *
     * @param dt1 first date/time
     * @param dt2 second date/time
     * @return the earlier ZonedDateTime
     */
    private ZonedDateTime minZonedDateTime(ZonedDateTime dt1, ZonedDateTime dt2) {
        return dt1.isBefore(dt2) ? dt1 : dt2;
    }

    /**
     * Represents an event in the sweep-line algorithm.
     */
    private record Event(Instant time, boolean isStart, Attendee attendee) {
    }

    /**
     * Associates a time slot with an attendee.
     */
    private record TimeSlotWithAttendee(TimeSlot timeSlot, Attendee attendee) {
    }
}
