package com.upc.quadrapp.occupancy.infrastructure.acl;

import com.upc.quadrapp.occupancy.application.internal.outboundservices.acl.ParkingLayoutPort;
import com.upc.quadrapp.parkingconfiguration.interfaces.acl.ParkingConfigurationContextFacade;
import com.upc.quadrapp.parkingconfiguration.interfaces.acl.ParkingConfigurationContextFacade.AccessLayoutView;
import com.upc.quadrapp.parkingconfiguration.interfaces.acl.ParkingConfigurationContextFacade.ParkingLayoutView;
import com.upc.quadrapp.parkingconfiguration.interfaces.acl.ParkingConfigurationContextFacade.ParkingLotView;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Anticorruption layer to Parking Configuration: translates its published layout into
 * Occupancy's placements. Occupancy never reads the configuration schema directly.
 */
@Component
public class ConfigurationLayoutAdapter implements ParkingLayoutPort {

    private final ParkingConfigurationContextFacade configuration;

    public ConfigurationLayoutAdapter(ParkingConfigurationContextFacade configuration) {
        this.configuration = configuration;
    }

    @Override
    public Optional<ParkingLayout> layoutOf(Long parkingLotId) {
        return configuration.layoutOf(parkingLotId).map(ConfigurationLayoutAdapter::toLayout);
    }

    @Override
    public Optional<Long> readableParkingLot(Long parkingLotId) {
        return configuration.findReadableParkingLot(parkingLotId).map(ParkingLotView::id);
    }

    @Override
    public Optional<Long> readableParkingLotOfZone(Long zoneId) {
        return configuration.findReadableParkingLotOfZone(zoneId).map(ParkingLotView::id);
    }

    @Override
    public Optional<Long> readableParkingLotOfLevel(Long levelId) {
        return configuration.findReadableParkingLotOfLevel(levelId).map(ParkingLotView::id);
    }

    private static ParkingLayout toLayout(ParkingLayoutView view) {
        List<SpacePlacement> spaces = view.zones().stream()
                .flatMap(zone -> zone.levels().stream()
                        .flatMap(level -> level.spaces().stream()
                                .map(space -> new SpacePlacement(
                                        space.spaceId(),
                                        zone.zoneId(),
                                        level.levelId(),
                                        space.active()
                                ))))
                .toList();
        return new ParkingLayout(
                view.parkingLotId(),
                view.institutionId(),
                view.lowAvailabilityPct(),
                spaces,
                view.accesses().stream().map(AccessLayoutView::accessId).toList(),
                view.accesses().stream()
                        .filter(AccessLayoutView::flowCountingEnabled)
                        .map(AccessLayoutView::accessId)
                        .toList()
        );
    }
}
