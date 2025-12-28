package org.example.common.model;

import lombok.Value;

import java.io.Serializable;

/**
 * Represents a time slot in the schedule (day + hour)
 */
@Value
public class TimeSlot implements Serializable {
    private static final long serialVersionUID = 1L;

    int day;      // 0-4 (Monday-Friday)
    int hour;     // 0-7 (8:00-16:00)

    public int getAbsoluteSlot() {
        return day * 8 + hour;
    }

    @Override
    public String toString() {
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri"};
        return days[day] + " " + (8 + hour) + ":00";
    }
}
