package org.example.common.model;

import lombok.Value;

import java.io.Serializable;

/**
 * A gene represents one scheduled class
 * Gene = (which class, when, where)
 */
@Value
public class Gene implements Serializable {
    private static final long serialVersionUID = 1L;

    int classId;
    TimeSlot timeSlot;
    int roomId;

    @Override
    public String toString() {
        return "Gene{C" + classId + " @ " + timeSlot + " in R" + roomId + "}";
    }
}
