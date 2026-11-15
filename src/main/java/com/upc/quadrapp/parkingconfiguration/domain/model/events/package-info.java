/**
 * Domain events Parking Configuration publishes to other bounded contexts (Published Language),
 * as Spring application events inside the transaction that produced them (event publication
 * registry), so a listener receives them only after the commit.
 */
@NamedInterface("events")
package com.upc.quadrapp.parkingconfiguration.domain.model.events;

import org.springframework.modulith.NamedInterface;
