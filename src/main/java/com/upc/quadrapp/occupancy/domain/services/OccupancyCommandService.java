package com.upc.quadrapp.occupancy.domain.services;

import com.upc.quadrapp.occupancy.domain.model.aggregates.OccupancySnapshot;
import com.upc.quadrapp.occupancy.domain.model.commands.CreateOccupancySnapshotCommand;
import com.upc.quadrapp.occupancy.domain.model.commands.RecordSensorEventCommand;

public interface OccupancyCommandService {

    boolean handle(RecordSensorEventCommand command);

    OccupancySnapshot handle(CreateOccupancySnapshotCommand command);
}
