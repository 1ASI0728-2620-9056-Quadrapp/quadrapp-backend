/**
 * Domain events Occupancy publishes to other bounded contexts (Published Language), as Spring
 * application events inside the transaction that produced them (event publication registry).
 */
@NamedInterface("events")
package com.upc.quadrapp.occupancy.domain.model.events;

import org.springframework.modulith.NamedInterface;
