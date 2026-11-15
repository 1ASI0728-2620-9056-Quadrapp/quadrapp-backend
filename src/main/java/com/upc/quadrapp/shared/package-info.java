/**
 * Shared kernel used by every bounded context (base types and the per-module Flyway setup).
 * It is an open module, so its types need no explicit exposure.
 */
@ApplicationModule(type = ApplicationModule.Type.OPEN)
package com.upc.quadrapp.shared;

import org.springframework.modulith.ApplicationModule;
