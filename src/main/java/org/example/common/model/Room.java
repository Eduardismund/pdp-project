package org.example.common.model;

import lombok.Value;

import java.io.Serializable;

/**
 * Represents a classroom
 */
@Value
public class Room implements Serializable {
    private static final long serialVersionUID = 1L;

    int id;
    int capacity;

    @Override
    public String toString() {
        return "Room{R" + id + ", cap=" + capacity + "}";
    }
}
