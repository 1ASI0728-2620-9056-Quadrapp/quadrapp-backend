package com.upc.quadrapp.prediction.infrastructure.configuration;

import com.upc.quadrapp.prediction.domain.model.valueobjects.AdvisoryPolicy;
import com.upc.quadrapp.prediction.domain.services.ArrivalAdvisor;
import com.upc.quadrapp.prediction.domain.services.ForecastDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the framework-free domain services of Prediction with the advisory policy. */
@Configuration(proxyBeanMethods = false)
public class PredictionDomainConfiguration {

    @Bean
    public ForecastDomainService forecastDomainService() {
        return new ForecastDomainService(AdvisoryPolicy.DEFAULT);
    }

    @Bean
    public ArrivalAdvisor arrivalAdvisor() {
        return new ArrivalAdvisor(AdvisoryPolicy.DEFAULT);
    }
}
