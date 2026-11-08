package dev.vertcode.hyperion.blueprint;

import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Load a folder of real Blockbench models, when one is given.
 *
 * <p>Real models are rarely ours to publish, so none are kept in the repository. Point
 * {@code HYPERION_MODELS} at a folder of {@code .bbmodel} files to check them all load:
 * {@code HYPERION_MODELS=~/models ./gradlew test}.
 */
@EnabledIfEnvironmentVariable(named = "HYPERION_MODELS", matches = ".+")
class RealModelsTest {

    @Test
    void everyModelLoads() {
        Logger logger = Logger.getLogger(RealModelsTest.class.getName());
        logger.setLevel(Level.SEVERE);
        BlueprintRegistry registry = new BlueprintRegistry(
                Path.of(System.getenv("HYPERION_MODELS")),
                new HyperionFormatRegistry(),
                Runnable::run,
                logger
        );

        ReloadResult result = registry.reloadAll().join();
        for (Blueprint blueprint : result.loaded()) {
            System.out.printf("%-40s %3d bones %3d animations %2d warnings%n", blueprint.key().asString(),
                    blueprint.bones().size(), blueprint.animations().size(), blueprint.report().count(IssueSeverity.WARNING));
        }

        assertTrue(result.failed().isEmpty(), () -> "Failed: " + result.failed().stream().map(LoadReport::source).toList());
        assertTrue(!result.loaded().isEmpty(), "The folder has no .bbmodel files");
    }
}
