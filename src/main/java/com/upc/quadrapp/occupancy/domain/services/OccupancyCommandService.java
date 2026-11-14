package com.upc.quadrapp.occupancy.domain.services;

import com.upc.quadrapp.occupancy.domain.model.aggregates.OccupancySnapshot;
import com.upc.quadrapp.occupancy.domain.model.commands.CreateParkingLotSnapshotCommand;
import com.upc.quadrapp.occupancy.domain.model.commands.CreateOccupancySnapshotCommand;
import com.upc.quadrapp.occupancy.domain.model.commands.RecordSensorEventCommand;

import java.util.List;

public interface OccupancyCommandService {

    /** Applies a sensing event once; returns false if its event ID was already recorded. */
    boolean handle(RecordSensorEventCommand command);

    OccupancySnapshot handle(CreateOccupancySnapshotCommand command);

    List<OccupancySnapshot> handle(CreateParkingLotSnapshotCommand command);

    /** Saves the snapshot of every zone for the current minute; returns the rows saved. */
    int recordMinuteSnapshots();

    /** Deletes snapshots older than 8 weeks and passages and sensor events older than 7 days. */
    int purgeExpiredHistory();
}
