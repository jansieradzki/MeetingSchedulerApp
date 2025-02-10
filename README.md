# Calendar Scheduler

## Overview

The Calendar Scheduler is a solution for automatically finding available meeting time slots for a group of people. The system considers each attendee’s working hours, existing appointments, and time zone. In addition to finding common free slots where all participants are available, the solution also provides a bonus functionality: if no slot exists where **all** attendees can attend, it computes a time slot in which the maximum number of participants are available and returns that slot along with the list of available attendees.

This solution is implemented in Java 17 using modern practices (such as Java Records, Streams, and Lombok) and is designed with Clean Code principles (as advocated by Robert C. Martin). The solution is production-ready and can be deployed using Docker/Docker Compose.

---

## Requirements

### Input Parameters

- **Attendees:**  
  Each attendee has the following:
    - **Working Hours:** The working period (e.g., 09:00 to 17:00) in the attendee’s local time zone.
    - **Appointments:** A list of already booked time slots (meetings) in the attendee’s local time.
    - **Time Zone:** The time zone identifier (e.g., "Europe/Warsaw", "America/New_York").

- **Meeting Duration:**  
  The required duration of the meeting (e.g., 1 hour).

- **Number of Proposed Slots:**  
  The maximum number of meeting proposals to return.

- **Timeframe:**  
  The overall global timeframe during which to search for available slots (e.g., from March 24, 2025 08:00 UTC to March 29, 2025 18:00 UTC).

### Output

- **Common Available Slots:**  
  A list of time slots during which all attendees are available.

- **Bonus – Maximum Attendance Slot:**  
  If no slot is found where all attendees are available, the algorithm returns a time slot in which the maximum number of participants can attend, along with a list of those available attendees.

---

## Detailed Algorithm Description

### 1. Normalization and Free Slot Computation

For each attendee, the system performs the following steps:

- **Data Normalization:**
    - Convert the attendee’s working hours and appointments to their local time zone.
    - For each day within the global timeframe, compute the **working period** by taking the intersection of the attendee’s working hours with the global timeframe.

- **Subtracting Appointments:**
    - For a given day, subtract the intervals occupied by the attendee’s appointments from the working period.
    - The result is a list of free time intervals for that day.

- **Time Complexity:**
    - Assuming the number of appointments per day is relatively small, the processing is nearly linear for each attendee.

### 2. Intersection of Free Slots

The goal is to determine the time intervals during which **all** attendees are free:

- **Approach:**
    - Each attendee’s free time intervals (already sorted by start time) are intersected using a two-pointer algorithm.
    - The algorithm iterates over two lists simultaneously, computing the common intersection (using the maximum of the start times and the minimum of the end times) and advances the pointer for the interval that ends first.

- **Time Complexity:**
    - For two lists of sizes _n_ and _m_, the intersection is computed in O(n + m).
    - When iterating over multiple attendees, the overall complexity depends on the total number of free intervals, which is efficient in typical calendar scenarios.

### 3. Generating Meeting Slot Proposals

For each common free interval:

- **Sliding Window Approach:**
    - A sliding window of size equal to the meeting duration is moved along the interval.
    - The window is shifted by a configurable granularity (e.g., every 15 minutes by default, but this is now parameterized).
    - For each valid position where the window does not extend past the end of the interval, a meeting slot proposal is generated.

- **Time Complexity:**
    - This process is linear relative to the number of generated proposals. The configurable granularity controls the number of iterations.

### 4. Bonus: Maximum Attendance Slot (Sweep-Line Algorithm)

If no common slot exists for all attendees:

- **Event Generation:**
    - For every free interval of every attendee, two events are created: one marking the start and one marking the end.

- **Processing with Sweep-Line:**
    - All events are sorted by time (with start events preceding end events when times are equal).
    - As the sweep-line moves through time, it maintains a set of currently available attendees.
    - Whenever the duration between successive events is at least the meeting duration, the algorithm considers that interval as a candidate and records it along with the number of available attendees.

- **Time Complexity:**
    - Sorting the events takes O(N log N), where N is twice the number of free intervals.
    - Processing the events is linear, O(N).

---

## Complexity Analysis Summary

1. **Free Slot Computation for an Attendee:**
    - For one attendee: roughly O(D * log(n)), where D is the number of days in the timeframe and _n_ is the number of appointments per day.

2. **Intersection of Free Slots:**
    - Each two-list intersection is computed in O(n + m) using the two-pointer approach.

3. **Generating Meeting Proposals:**
    - Linear relative to the number of generated proposals, controllable via the granularity parameter.

4. **Sweep-Line Algorithm (Bonus):**
    - Sorting events: O(N log N)
    - Processing events: O(N)

Under typical calendar scenarios (e.g., a weekly timeframe with a few appointments per day), the solution is very efficient.

---

## How the Requirements Are Met

- **Input Handling:**
    - The system accepts a list of attendees, each with a time zone, fixed working hours, and a list of appointments.

- **Meeting Duration & Timeframe:**
    - The meeting duration and global timeframe are used to compute the available intervals.

- **Common Available Slots:**
    - The algorithm computes free intervals for each attendee, then intersects them to produce a list of slots when all are available.

- **Meeting Slot Proposals:**
    - Proposals are generated from the common free intervals using a sliding window approach with a configurable granularity.

- **Bonus – Maximum Attendance Slot:**
    - If no common slot exists, the system uses a sweep-line algorithm to find the slot with the maximum attendance and returns that slot along with the list of available attendees.

- **Clean Code & Modularity:**
    - Each step (normalization, free slot computation, intersection, proposal generation, bonus sweep-line) is implemented in separate, well-named methods.
    - Modern Java constructs (Streams, Records, parameterization) ensure that the code is readable, maintainable, and easily extensible.

---

## Conclusion

The Calendar Scheduler meets all the task requirements by:

- **Precisely processing input data:**
    - It normalizes working hours and appointments based on time zones.

- **Efficiently computing free intervals:**
    - It subtracts busy intervals from working periods to determine free time.

- **Accurately intersecting free intervals:**
    - The two-pointer algorithm computes the common free slots in near-linear time.

- **Generating meeting proposals:**
    - A sliding window with configurable granularity produces meeting slot proposals.

- **Handling edge cases with a bonus solution:**
    - If no common slot exists, a sweep-line algorithm finds the time slot with maximum available attendees.

- **Maintaining Clean Code and Modularity:**
    - The solution is implemented using best practices in modern Java, ensuring high quality and ease of maintenance.

Overall, this solution is efficient and scalable under typical calendar conditions and is designed to be easily extendable for more demanding scenarios.

