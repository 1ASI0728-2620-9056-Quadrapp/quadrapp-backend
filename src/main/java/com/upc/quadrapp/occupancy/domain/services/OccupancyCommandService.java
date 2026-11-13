package com.upc.quadrapp.occupancy.domain.services;

import com.upc.quadrapp.occupancy.domain.model.aggregates.OccupancySnapshot;
import com.upc.quadrapp.occupancy.domain.model.commands.CreateParkingLotSnapshotCommand;
import com.upc.quadrapp.occupancy.domain.model.commands.CreateOccupancySnapshotCommand;
import com.upc.quadrapp.occupancy.domain.model.commands.RecordSensorEventCommand;

import java.util.List;

public interface OccupancyCommandService {

    boolean handle(RecordSensorEventCommand command);

    OccupancySnapshot handle(CreateOccupancySnapshotCommand command);

    List<OccupancySnapshot> handle(CreateParkingLotSnapshotCommand command);
}
