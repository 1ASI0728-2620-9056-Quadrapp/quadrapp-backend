package com.upc.quadrapp.occupancy.infrastructure.acl;

import com.upc.quadrapp.occupancy.application.internal.outboundservices.acl.ParkingLayoutPort.SpacePlacement;
import com.upc.quadrapp.parkingconfiguration.interfaces.acl.ParkingConfigurationContextFacade;
import com.upc.quadrapp.parkingconfiguration.interfaces.acl.ParkingConfigurationContextFacade.LevelLayoutView;
import com.upc.quadrapp.parkingconfiguration.interfaces.acl.ParkingConfigurationContextFacade.ParkingLayoutView;
import com.upc.quadrapp.parkingconfiguration.interfaces.acl.ParkingConfigurationContextFacade.SpaceLayoutView;
import com.upc.quadrapp.parkingconfiguration.interfaces.acl.ParkingConfigurationContextFacade.ZoneLayoutView;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Spaces of a deactivated parking lot are not in use: Occupancy treats every one of them as
 * inactive, so they are not counted and never change state.
 */
class ConfigurationLayoutAdapterTest {

    private final ParkingConfigurationContextFacade facade = mock(ParkingConfigurationContextFacade.class);
    private final ConfigurationLayoutAdapter adapter = new ConfigurationLayoutAdapter(facade);

    @Test
    void everySpaceOfAnInactiveParkingLotIsInactive() {
        when(facade.layoutOf(7L)).thenReturn(Optional.of(layout(false)));

        assertThat(adapter.layoutOf(7L).orElseThrow().spaces())
                .containsExactly(new SpacePlacement(11L, 2L, 3L, false), new SpacePlacement(12L, 2L, 3L, false));
    }

    @Test
    void anActiveParkingLotKeepsTheActiveFlagOfEachSpace() {
        when(facade.layoutOf(7L)).thenReturn(Optional.of(layout(true)));

        assertThat(adapter.layoutOf(7L).orElseThrow().spaces())
                .containsExactly(new SpacePlacement(11L, 2L, 3L, true), new SpacePlacement(12L, 2L, 3L, false));
    }

    private static ParkingLayoutView layout(boolean lotActive) {
        return new ParkingLayoutView(7L, 5L, lotActive, 10, List.of(new ZoneLayoutView(2L, List.of(
                new LevelLayoutView(3L, List.of(new SpaceLayoutView(11L, true), new SpaceLayoutView(12L, false)))))),
                List.of());
    }
}
