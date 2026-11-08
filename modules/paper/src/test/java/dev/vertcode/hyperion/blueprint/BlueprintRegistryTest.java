package dev.vertcode.hyperion.blueprint;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlueprintRegistryTest {

    private static final Key LEGACY = Key.key("hyperion", "mobs/legacy_layout");
    private static final Key GROUPS = Key.key("hyperion", "groups_layout");

    @TempDir
    private Path folder;

    private BlueprintRegistry registry;

    @BeforeEach
    void setUp() throws IOException {
        Logger logger = Logger.getAnonymousLogger();
        logger.setLevel(Level.OFF);
        this.registry = new BlueprintRegistry(this.folder, new HyperionFormatRegistry(), Runnable::run, logger);

        Files.createDirectories(this.folder.resolve("mobs"));
        copy("legacy_layout", "mobs/legacy_layout.bbmodel");
        copy("groups_layout", "groups_layout.bbmodel");
    }

    @Test
    void loadsEveryFileInTheFolder() {
        ReloadResult result = this.registry.reloadAll().join();

        assertEquals(2, result.loaded().size());
        assertTrue(result.failed().isEmpty());
        assertEquals(List.of(GROUPS, LEGACY), this.registry.all().stream().map(Blueprint::key).toList());
        assertEquals(1, this.registry.find(LEGACY).orElseThrow().revision());
        assertTrue(this.registry.find(LEGACY).orElseThrow().owner() == null);
    }

    @Test
    void countsRevisionsAndReportsChanges() throws IOException {
        this.registry.reloadAll().join();
        copy("java_block", "groups_layout.bbmodel");

        ReloadResult result = this.registry.reloadAll().join();
        BlueprintChanges changes = Objects.requireNonNull(result.changes().get(GROUPS));

        assertEquals(2, this.registry.find(GROUPS).orElseThrow().revision());
        assertEquals(List.of("block"), changes.addedBones());
        assertEquals(List.of("body", "head"), changes.removedBones());
        assertEquals("+1 bone, -2 bones, -3 animations", changes.describe());
        assertTrue(Objects.requireNonNull(result.changes().get(LEGACY)).isEmpty());
    }

    @Test
    void dropsModelsWhoseFileIsGone() throws IOException {
        this.registry.reloadAll().join();
        Files.delete(this.folder.resolve("groups_layout.bbmodel"));

        ReloadResult result = this.registry.reloadAll().join();

        assertEquals(List.of(GROUPS), result.removed());
        assertTrue(this.registry.find(GROUPS).isEmpty());
    }

    @Test
    void keepsTheOldBlueprintWhenAReloadFails() throws IOException {
        this.registry.reloadAll().join();
        Files.writeString(this.folder.resolve("groups_layout.bbmodel"), "{ not json");

        ReloadResult result = this.registry.reloadAll().join();

        assertEquals(1, result.failed().size());
        assertEquals(1, this.registry.find(GROUPS).orElseThrow().revision());
        assertTrue(this.registry.report(GROUPS).orElseThrow().hasErrors());
        assertFalse(this.registry.report(LEGACY).orElseThrow().hasErrors());
    }

    @Test
    void reportsFilesWhoseNameCannotBeAKey() throws IOException {
        copy("java_block", "bad+name.bbmodel");

        ReloadResult result = this.registry.reloadAll().join();

        assertEquals(2, result.loaded().size());
        assertEquals("bad+name.bbmodel", result.failed().getFirst().source());
    }

    @Test
    void reloadsOneKey() {
        this.registry.reloadAll().join();

        Blueprint reloaded = this.registry.reload(LEGACY).join();

        assertEquals(2, reloaded.revision());
        assertTrue(this.registry.unregister(LEGACY));
        assertTrue(this.registry.find(LEGACY).isEmpty());
        assertTrue(this.registry.reload(LEGACY).isCompletedExceptionally());
    }

    private void copy(final String fixture, final String target) throws IOException {
        Files.copy(Fixtures.path(fixture), this.folder.resolve(target), StandardCopyOption.REPLACE_EXISTING);
    }
}
