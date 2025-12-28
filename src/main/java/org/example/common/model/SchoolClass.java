package org.example.common.model;

import lombok.Value;

import java.io.Serializable;

/**
 * Represents a class that needs to be scheduled
 */
@Value
public class SchoolClass implements Serializable {
    private static final long serialVersionUID = 1L;

    int id;
    String subject;
    int teacherId;
    int studentGroup;
    int requiredCapacity;

    @Override
    public String toString() {
        return "Class{" + subject + ", T" + teacherId + ", G" + studentGroup + "}";
    }
}
