package dev.vertcode.hyperion.blueprint.bbmodel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vertcode.hyperion.api.model.behavior.BehaviorMatcher;
import dev.vertcode.hyperion.api.model.source.BytesSource;
import dev.vertcode.hyperion.blueprint.Fixtures;
import dev.vertcode.hyperion.blueprint.IssueSeverity;
import dev.vertcode.hyperion.blueprint.LoadIssue;
import dev.vertcode.hyperion.blueprint.LoadReport;
import dev.vertcode.hyperion.blueprint.data.Animation;
import dev.vertcode.hyperion.blueprint.data.Animator;
import dev.vertcode.hyperion.blueprint.data.AnimatorType;
import dev.vertcode.hyperion.blueprint.data.Cube;
import dev.vertcode.hyperion.blueprint.data.CubeFace;
import dev.vertcode.hyperion.blueprint.data.FaceSide;
import dev.vertcode.hyperion.blueprint.data.Group;
import dev.vertcode.hyperion.blueprint.data.Interpolation;
import dev.vertcode.hyperion.blueprint.data.Keyframe;
import dev.vertcode.hyperion.blueprint.data.KeyframeChannel;
import dev.vertcode.hyperion.blueprint.data.Locator;
import dev.vertcode.hyperion.blueprint.data.LoopMode;
import dev.vertcode.hyperion.blueprint.data.ModelData;
import dev.vertcode.hyperion.blueprint.data.Texture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BbmodelReaderTest {

    private LoadReport report;
    private ModelData data;

    @BeforeEach
    void read() throws IOException {
        this.report = new LoadReport("groups_layout.bbmodel");
        this.data = Fixtures.read("groups_layout", this.report);
    }

    @Test
    void readsTheProject() {
        assertEquals("groups_layout", this.data.name());
        assertEquals("free", this.data.format());
        assertEquals(16, this.data.resolutionWidth());
        assertEquals(1.0, this.data.metadata().number("hyperion_version").orElseThrow());
        assertFalse(this.data.metadata().has("resolution"));
        assertFalse(this.report.hasErrors());
    }

    @Test
    void readsGroupsFromTheTopLevelList() {
        assertEquals(List.of("g-body"), this.data.roots());

        Group body = this.data.groups().get("g-body");
        assertNotNull(body);
        assertEquals("body", body.name());
        assertEquals(List.of("c-body", "g-head"), body.children());

        Group head = this.data.groups().get("g-head");
        assertNotNull(head);
        assertEquals(new Vector3f(0, 8, 0), head.origin());
        assertEquals(new Vector3f(90, 90, 0), head.rotation());
        assertFalse(head.visible());
        assertEquals(List.of("c-horn", "l-mouth"), head.children());
    }

    @Test
    void keepsOnlyTheFieldsBlockbenchDoesNotUse() {
        Group body = this.data.groups().get("g-body");
        assertNotNull(body);
        assertEquals(List.of("head", "glow"), body.metadata().strings(BehaviorMatcher.TAG_FIELD));
        assertEquals(2.5, body.metadata().number("spin_speed").orElseThrow());
        assertEquals(true, body.metadata().bool("boss").orElseThrow());
        assertEquals("two", body.metadata().section("extra").orElseThrow().string("phase").orElseThrow());
        assertFalse(body.metadata().has("origin"));
        assertFalse(body.metadata().has("isOpen"));
    }

    @Test
    void readsCubesAndFaces() {
        assertEquals(List.of("c-body", "c-horn"), List.copyOf(this.data.cubes().keySet()));

        Cube body = this.data.cubes().get("c-body");
        assertNotNull(body);
        assertEquals(new Vector3f(-4, 0, -4), body.from());
        assertEquals(new Vector3f(4, 8, 4), body.to());
        assertEquals(6, body.faces().size());

        CubeFace up = body.faces().get(FaceSide.UP);
        assertNotNull(up);
        assertFalse(up.textured());
        assertEquals(90, up.rotation());
        assertEquals(1, Objects.requireNonNull(body.faces().get(FaceSide.DOWN)).texture());

        Cube horn = this.data.cubes().get("c-horn");
        assertNotNull(horn);
        assertEquals(new Vector3f(0, 0, 22.5f), horn.rotation());
        assertEquals(0.5f, horn.inflate());
        assertEquals(CubeFace.NO_TEXTURE, Objects.requireNonNull(horn.faces().get(FaceSide.NORTH)).texture());
    }

    @Test
    void readsLocatorsWithTheirMetadata() {
        assertEquals(1, this.data.locators().size());
        Locator mouth = Objects.requireNonNull(this.data.locators().get("l-mouth"));
        assertEquals(new Vector3f(0, 10, -4), mouth.position());
        assertEquals("red", mouth.metadata().string("particle_color").orElseThrow());
    }

    @Test
    void decodesTexturesAndTheirFrames() {
        List<Texture> textures = this.data.textures();
        assertEquals(3, textures.size());

        assertEquals(16, textures.get(0).width());
        assertEquals(1, textures.get(0).frames());
        assertTrue(textures.get(0).png().length > 0);

        Texture flicker = textures.get(1);
        assertEquals(8, flicker.width());
        assertEquals(24, flicker.height());
        assertEquals(3, flicker.frames());
        assertEquals(4, flicker.frameTime());
        assertTrue(flicker.frameInterpolate());
        assertEquals(List.of(1, 0, 1), flicker.frameOrder());

        assertEquals(0, textures.get(2).png().length);
    }

    @Test
    void readsAnimationsWithEveryValueAsText() {
        Animation walk = this.data.animations().getFirst();
        assertEquals("walk", walk.name());
        assertEquals(LoopMode.LOOP, walk.loop());
        assertEquals(1.5f, walk.length());
        assertEquals("0", walk.startDelay());
        assertEquals("query.random * 2", walk.loopDelay());
        assertEquals(2, walk.animators().size());

        Animator head = walk.animators().getFirst();
        assertEquals(AnimatorType.BONE, head.type());
        assertEquals("g-head", head.target());

        List<Keyframe> keyframes = head.keyframes();
        assertEquals(List.of(0f, 0.5f, 0.7f, 1f, 1.5f), keyframes.stream().map(Keyframe::time).toList());
        assertEquals("math.sin(query.anim_time * 90) * 10", keyframes.get(1).points().getFirst().value("x", ""));
        assertEquals("0", keyframes.get(1).points().getFirst().value("z", ""));
        assertEquals(Interpolation.CATMULLROM, keyframes.get(1).interpolation());
        assertEquals(Interpolation.LINEAR, keyframes.get(2).interpolation());

        Keyframe step = keyframes.get(3);
        assertEquals(KeyframeChannel.POSITION, step.channel());
        assertEquals(Interpolation.STEP, step.interpolation());
        assertEquals(2, step.points().size());
        assertEquals("2.5", step.points().getFirst().value("y", ""));
        assertNull(step.bezier());

        Keyframe bezier = keyframes.get(4);
        assertEquals(Interpolation.BEZIER, bezier.interpolation());
        assertNotNull(bezier.bezier());
        assertEquals(new Vector3f(0.2f), bezier.bezier().rightTime());
    }

    @Test
    void readsTheEffectsTimeline() {
        Animator effects = this.data.animations().getFirst().animators().get(1);
        assertEquals(AnimatorType.EFFECT, effects.type());
        assertEquals(KeyframeChannel.PARTICLE, effects.keyframes().getFirst().channel());
        assertEquals(Map.of("effect", "flame", "locator", "mouth", "script", ""), effects.keyframes().getFirst().points().getFirst().values());
        assertEquals(KeyframeChannel.SOUND, effects.keyframes().get(1).channel());
    }

    @Test
    void warnsAboutWhatItCannotUse() {
        List<String> warnings = this.report.issues().stream()
                .filter(issue -> issue.severity() == IssueSeverity.WARNING)
                .map(LoadIssue::describe)
                .toList();

        assertTrue(warnings.contains("element 'blob': is a mesh, which Hyperion does not render, so it is left out"));
        assertTrue(warnings.contains("outliner: the outliner points at group g-missing, which is not in the file"));
        assertTrue(warnings.contains("cube 'horn': face north uses texture 5, but the file has 3 texture(s)"));
        assertTrue(warnings.contains("texture 'external.png': has no image saved in the file; save the texture into the project"));
        assertTrue(warnings.stream().anyMatch(warning -> warning.contains("'jiggle' keyframe")));
        assertTrue(warnings.stream().anyMatch(warning -> warning.contains("'wobbly' interpolation")));
        assertTrue(warnings.stream().anyMatch(warning -> warning.contains("null_object named 'tail_target'")));
        assertTrue(warnings.stream().anyMatch(warning -> warning.contains("loop mode 'spin'")));
        assertEquals(LoopMode.ONCE, this.data.animations().get(1).loop());
    }

    @Test
    void readsTheOlderLayoutWithGroupsInsideTheOutliner() throws IOException {
        ModelData legacy = Fixtures.read("legacy_layout", new LoadReport("legacy_layout.bbmodel"));

        assertEquals(List.of("g-root", "c-pebble"), legacy.roots());
        assertEquals(3, legacy.groups().size());
        Group arm = Objects.requireNonNull(legacy.groups().get("g-arm"));
        assertEquals("arm", arm.name());
        assertEquals(new Vector3f(0, 0, 45), arm.rotation());
        assertEquals(List.of("c-arm", "n-tip"), arm.children());
    }

    @Test
    void tellsFilesFromBeforeBlockbench5ByTheirVersion() throws IOException {
        ModelData legacy = Fixtures.read("legacy_layout", new LoadReport("legacy_layout.bbmodel"));

        assertEquals("5.0", this.data.formatVersion());
        assertFalse(this.data.invertedAnimations());
        assertEquals("4.10", legacy.formatVersion());
        assertTrue(legacy.invertedAnimations());
    }

    @Test
    void refusesBrokenJson() {
        BbmodelFormat format = new BbmodelFormat();
        BytesSource broken = new BytesSource("broken.bbmodel", "{\"elements\": [".getBytes(StandardCharsets.UTF_8));
        BytesSource array = new BytesSource("array.bbmodel", "[]".getBytes(StandardCharsets.UTF_8));

        assertThrows(IOException.class, () -> format.read(broken));
        assertThrows(IOException.class, () -> format.read(array));
        assertTrue(format.supports(broken));
        assertFalse(format.supports(new BytesSource("model.json", new byte[0])));
    }
}
