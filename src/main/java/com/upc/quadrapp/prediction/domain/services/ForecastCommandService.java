package com.upc.quadrapp.prediction.domain.services;

import com.upc.quadrapp.prediction.domain.model.aggregates.Forecast;
import com.upc.quadrapp.prediction.domain.model.commands.GenerateForecastsCommand;

import java.util.List;

public interface ForecastCommandService {

    List<Forecast> handle(GenerateForecastsCommand command);

    int evaluateDueForecasts();
}
