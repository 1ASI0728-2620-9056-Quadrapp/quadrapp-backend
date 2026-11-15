package com.upc.quadrapp.prediction.infrastructure.model;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.upc.quadrapp.prediction.application.internal.outboundservices.model.PredictionModelPort;
import com.upc.quadrapp.prediction.domain.model.valueobjects.ModelPrediction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Runs the exported forecasting model in process with ONNX Runtime (D8): no network call.
 * <p>
 * The model directory ({@code quadrapp.prediction.model.directory}) holds {@code model-vN.onnx}
 * files, each with its metadata {@code model-vN.json}:
 * <pre>
 * {"version": "v3",
 *  "features": ["occupancy_pct", "minute_of_day", "day_of_week", "campus_event"],
 *  "outputs": {"15": "occupancy_15", "30": "occupancy_30", "45": "occupancy_45", "60": "occupancy_60"},
 *  "maeByHorizon": {"15": 4.1, "30": 6.3, "45": 7.9, "60": 9.4}}
 * </pre>
 * The model has one float input of shape {@code [N, features]} with the features in that order,
 * and one output of shape {@code [N, 1]} (or {@code [N]}) per horizon. The runner uses the highest
 * N that has both files, and looks for a newer one at most once per
 * {@code quadrapp.prediction.model.reload-interval}, so a new version is loaded without a
 * restart. Without a directory, a model or valid metadata, it predicts nothing and Prediction
 * uses the fallback.
 */
@Component
public class OnnxModelRunner implements PredictionModelPort, DisposableBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(OnnxModelRunner.class);
    private static final Pattern MODEL_FILE = Pattern.compile("model-v(\\d{1,9})\\.onnx");
    private static final Set<String> KNOWN_FEATURES = Set.of(
            OCCUPANCY_PCT, MINUTE_OF_DAY, DAY_OF_WEEK, CAMPUS_EVENT);
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final Path directory;
    private final Duration reloadInterval;

    private LoadedModel loaded;
    private ModelFile rejected;
    private long lastCheckNanos;
    private boolean checked;

    public OnnxModelRunner(
            @Value("${quadrapp.prediction.model.directory:}") String directory,
            @Value("${quadrapp.prediction.model.reload-interval:PT1M}") Duration reloadInterval
    ) {
        this.directory = directory == null || directory.isBlank() ? null : Path.of(directory);
        this.reloadInterval = reloadInterval;
    }

    @Override
    public synchronized Optional<ModelPrediction> predict(Map<String, Double> features) {
        refreshIfDue();
        if (loaded == null) {
            return Optional.empty();
        }
        float[] row = new float[loaded.metadata().features().size()];
        for (int i = 0; i < row.length; i++) {
            Double value = features.get(loaded.metadata().features().get(i));
            if (value == null || !Double.isFinite(value)) {
                return Optional.empty();
            }
            row[i] = value.floatValue();
        }
        try (OnnxTensor input = OnnxTensor.createTensor(OrtEnvironment.getEnvironment(), new float[][]{row});
             OrtSession.Result result = loaded.session().run(
                     Map.of(loaded.inputName(), input),
                     new HashSet<>(loaded.metadata().outputs().values()))) {
            Map<Integer, Double> expected = new HashMap<>();
            Map<Integer, Double> errors = new HashMap<>();
            for (Map.Entry<String, String> output : loaded.metadata().outputs().entrySet()) {
                int horizon = Integer.parseInt(output.getKey());
                Optional<OnnxValue> value = result.get(output.getValue());
                Double mae = loaded.metadata().maeByHorizon().get(output.getKey());
                if (value.isPresent() && mae != null) {
                    expected.put(horizon, firstValue(value.get().getValue()));
                    errors.put(horizon, mae);
                }
            }
            return Optional.of(new ModelPrediction(loaded.metadata().version(), expected, errors));
        } catch (OrtException | RuntimeException exception) {
            LOGGER.warn("Model {} failed; the fallback is used.", loaded.metadata().version(), exception);
            return Optional.empty();
        }
    }

    @Override
    public synchronized void destroy() {
        close(loaded);
        loaded = null;
    }

    private void refreshIfDue() {
        if (directory == null) {
            return;
        }
        long now = System.nanoTime();
        if (checked && now - lastCheckNanos < reloadInterval.toNanos()) {
            return;
        }
        checked = true;
        lastCheckNanos = now;
        Optional<ModelFile> newest = newestModel();
        if (newest.isEmpty() || newest.get().equals(rejected)
                || (loaded != null && loaded.file().equals(newest.get()))) {
            return;
        }
        try {
            LoadedModel candidate = load(newest.get());
            close(loaded);
            loaded = candidate;
            rejected = null;
            LOGGER.info("Loaded forecasting model {} from {}.", candidate.metadata().version(), newest.get().onnx());
        } catch (IOException | OrtException | RuntimeException exception) {
            rejected = newest.get();
            LOGGER.error("Model {} was not loaded; {} is still used.", newest.get().onnx(),
                    loaded == null ? "the fallback" : loaded.metadata().version(), exception);
        }
    }

    private Optional<ModelFile> newestModel() {
        if (!Files.isDirectory(directory)) {
            return Optional.empty();
        }
        try (Stream<Path> files = Files.list(directory)) {
            return files
                    .map(path -> {
                        Matcher matcher = MODEL_FILE.matcher(path.getFileName().toString());
                        if (!matcher.matches()) {
                            return null;
                        }
                        int number = Integer.parseInt(matcher.group(1));
                        Path metadata = directory.resolve("model-v" + number + ".json");
                        if (!Files.isRegularFile(metadata)) {
                            return null;
                        }
                        return new ModelFile(number, path, metadata, lastModified(path), lastModified(metadata));
                    })
                    .filter(java.util.Objects::nonNull)
                    .max(Comparator.comparingInt(ModelFile::number));
        } catch (IOException exception) {
            LOGGER.warn("Model directory {} could not be read.", directory, exception);
            return Optional.empty();
        }
    }

    private static LoadedModel load(ModelFile file) throws IOException, OrtException {
        ModelMetadata metadata = JSON.readValue(Files.readAllBytes(file.metadata()), ModelMetadata.class);
        metadata.validate();
        OrtSession session = OrtEnvironment.getEnvironment()
                .createSession(file.onnx().toString(), new OrtSession.SessionOptions());
        try {
            if (session.getInputNames().size() != 1) {
                throw new IllegalArgumentException("The model must have exactly one input.");
            }
            if (!session.getOutputNames().containsAll(metadata.outputs().values())) {
                throw new IllegalArgumentException("The metadata names outputs the model does not have.");
            }
            return new LoadedModel(file, metadata, session, session.getInputNames().iterator().next());
        } catch (RuntimeException exception) {
            session.close();
            throw exception;
        }
    }

    private static double firstValue(Object value) {
        return switch (value) {
            case float[][] matrix -> matrix[0][0];
            case float[] vector -> vector[0];
            case double[][] matrix -> matrix[0][0];
            case double[] vector -> vector[0];
            default -> throw new IllegalStateException("Unsupported model output " + value.getClass());
        };
    }

    private static long lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException exception) {
            return -1;
        }
    }

    private static void close(LoadedModel model) {
        if (model == null) {
            return;
        }
        try {
            model.session().close();
        } catch (OrtException exception) {
            LOGGER.warn("Model session could not be closed.", exception);
        }
    }

    private record ModelFile(int number, Path onnx, Path metadata, long onnxModified, long metadataModified) {}

    private record LoadedModel(ModelFile file, ModelMetadata metadata, OrtSession session, String inputName) {}

    record ModelMetadata(
            String version,
            List<String> features,
            Map<String, String> outputs,
            Map<String, Double> maeByHorizon
    ) {
        void validate() {
            if (version == null || version.isBlank() || version.length() > 40) {
                throw new IllegalArgumentException("Model version must contain 1 to 40 characters.");
            }
            if (features == null || features.isEmpty() || !KNOWN_FEATURES.containsAll(features)) {
                throw new IllegalArgumentException("Model features must be a subset of " + KNOWN_FEATURES + ".");
            }
            if (outputs == null || outputs.isEmpty() || maeByHorizon == null) {
                throw new IllegalArgumentException("Model outputs and MAE per horizon are required.");
            }
            for (String horizon : outputs.keySet()) {
                if (!Set.of("15", "30", "45", "60").contains(horizon)) {
                    throw new IllegalArgumentException("Unsupported horizon " + horizon + ".");
                }
                Double mae = maeByHorizon.get(horizon);
                if (mae == null || !Double.isFinite(mae) || mae < 0) {
                    throw new IllegalArgumentException("Horizon " + horizon + " needs a non-negative MAE.");
                }
            }
        }
    }
}
