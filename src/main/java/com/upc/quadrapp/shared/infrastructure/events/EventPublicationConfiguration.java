package com.upc.quadrapp.shared.infrastructure.events;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Runs {@code @ApplicationModuleListener} handlers asynchronously after the publishing
 * transaction commits, so a slow consumer never delays the module that published the event.
 */
@Configuration(proxyBeanMethods = false)
@EnableAsync
public class EventPublicationConfiguration {
}
