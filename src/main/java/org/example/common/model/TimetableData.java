package org.example.common.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Contains all data for the timetabling problem
 * This is shared across all MPI ranks
 */
@Getter
@RequiredArgsConstructor
public class TimetableData implements Serializable {
    private static final long serialVersionUID = 1L;

    private final List<SchoolClass> classes;
    private final List<Room> rooms;
    private final int numTeachers;
    private final int numStudentGroups;
    private final int daysPerWeek = 5;
    private final int hoursPerDay = 8;

    /**
     * Generate a random problem instance
     */
    public static TimetableData generateRandom(int numClasses, int numRooms, int numTeachers, int numGroups, long seed) {
        Random rand = new Random(seed);

        // Generate rooms
        List<Room> rooms = new ArrayList<>();
        for (int i = 0; i < numRooms; i++) {
            int capacity = 20 + rand.nextInt(31); // 20-50 students
            rooms.add(new Room(i, capacity));
        }

        // Generate classes
        List<SchoolClass> classes = new ArrayList<>();
        String[] subjects = {"Math", "Physics", "Chemistry", "Biology", "History", "English", "CS", "Art"};

        for (int i = 0; i < numClasses; i++) {
            String subject = subjects[rand.nextInt(subjects.length)];
            int teacherId = rand.nextInt(numTeachers);
            int studentGroup = rand.nextInt(numGroups);
            int requiredCapacity = 15 + rand.nextInt(26); // 15-40 students

            classes.add(new SchoolClass(i, subject, teacherId, studentGroup, requiredCapacity));
        }

        return new TimetableData(classes, rooms, numTeachers, numGroups);
    }

    public int getTotalTimeSlots() {
        return daysPerWeek * hoursPerDay;
    }

    public SchoolClass getClass(int id) {
        return classes.get(id);
    }

    public Room getRoom(int id) {
        return rooms.get(id);
    }

    @Override
    public String toString() {
        return "TimetableData{" +
                "classes=" + classes.size() +
                ", rooms=" + rooms.size() +
                ", teachers=" + numTeachers +
                ", groups=" + numStudentGroups +
                '}';
    }
}
