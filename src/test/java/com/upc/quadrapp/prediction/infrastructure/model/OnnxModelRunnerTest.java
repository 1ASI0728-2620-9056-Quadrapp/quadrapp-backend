package com.upc.quadrapp.prediction.infrastructure.model;

import com.upc.quadrapp.prediction.domain.model.valueobjects.ModelPrediction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The runner loads the newest {@code model-vN.onnx} of the model directory with its metadata
 * ({@code model-vN.json}: version, feature order, output per horizon, MAE per horizon), runs it in
 * process with ONNX Runtime and loads a newer version without a restart. Without a usable model it
 * returns nothing, so Prediction uses the fallback. The test model is
 * {@code occupancy_h = 0.5 * occupancy_pct + h} (see generate_test_model.py).
 */
class OnnxModelRunnerTest {

    private static final Map<String, Double> FEATURES = Map.of(
            "occupancy_pct", 60.0, "minute_of_day", 600.0, "day_of_week", 3.0, "campus_event", 0.0);

    @TempDir
    private Path directory;

    private OnnxModelRunner runner;

    @AfterEach
    void close() {
        if (runner != null) {
            runner.destroy();
        }
    }

    @Test
    void runsTheModelWithTheFeaturesInTheOrderOfItsMetadata() throws IOException {
        install(1, "test-v1");
        runner = new OnnxModelRunner(directory.toString(), Duration.ZERO);

        ModelPrediction prediction = runner.predict(FEATURES).orElseThrow();

        assertThat(prediction.modelVersion()).isEqualTo("test-v1");
        assertThat(prediction.expectedOccupancyPctByHorizon())
                .containsEntry(15, 45.0).containsEntry(30, 60.0).containsEntry(45, 75.0).containsEntry(60, 90.0);
        assertThat(prediction.meanAbsoluteErrorByHorizon())
                .containsEntry(15, 2.5).containsEntry(30, 5.0).containsEntry(45, 10.0).containsEntry(60, 30.0);
    }

    @Test
    void aNewerVersionInTheDirectoryIsLoadedWithoutARestart() throws IOException {
        install(1, "test-v1");
        runner = new OnnxModelRunner(directory.toString(), Duration.ZERO);
        assertThat(runner.predict(FEATURES).orElseThrow().modelVersion()).isEqualTo("test-v1");

        install(2, "test-v2");

        assertThat(runner.predict(FEATURES).orElseThrow().modelVersion()).isEqualTo("test-v2");
    }

    @Test
    void withoutADirectoryOrAModelThereIsNoPrediction() {
        runner = new OnnxModelRunner("", Duration.ZERO);
        assertThat(runner.predict(FEATURES)).isEmpty();

        runner = new OnnxModelRunner(directory.toString(), Duration.ZERO);
        assertThat(runner.predict(FEATURES)).isEmpty();
    }

    @Test
    void metadataWithAnUnknownFeatureIsRejected() throws IOException {
        install(1, "test-v1");
        Files.writeString(directory.resolve("model-v1.json"), """
                {"version": "bad", "features": ["occupancy_pct", "rain_mm", "day_of_week", "campus_event"],
                 "outputs": {"15": "occupancy_15"}, "maeByHorizon": {"15": 2.5}}
                """);
        runner = new OnnxModelRunner(directory.toString(), Duration.ZERO);

        assertThat(runner.predict(FEATURES)).isEmpty();
    }

    private void install(int number, String version) throws IOException {
        try (InputStream model = resource("model-v1.onnx")) {
            Files.copy(model, directory.resolve("model-v" + number + ".onnx"));
        }
        String metadata;
        try (InputStream json = resource("model-v1.json")) {
            metadata = new String(json.readAllBytes()).replace("\"test-v1\"", "\"" + version + "\"");
        }
        Files.writeString(directory.resolve("model-v" + number + ".json"), metadata);
    }

    private static InputStream resource(String name) {
        return Objects.requireNonNull(OnnxModelRunnerTest.class.getResourceAsStream("/prediction/model/" + name));
    }
}
