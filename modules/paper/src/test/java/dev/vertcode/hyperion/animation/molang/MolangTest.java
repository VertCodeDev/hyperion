package dev.vertcode.hyperion.animation.molang;

import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MolangTest {

    private static final float EPSILON = 1.0e-4f;

    @Test
    void readsNumbersAndArithmeticInOrder() {
        assertEquals(22.5f, value("22.5"), EPSILON);
        assertEquals(7f, value("1 + 2 * 3"), EPSILON);
        assertEquals(9f, value("(1 + 2) * 3"), EPSILON);
        assertEquals(-4f, value("-(2 + 2)"), EPSILON);
        assertEquals(0f, value("5 / 0"), EPSILON);
        assertTrue(Molang.compile("1 + 2").constant());
    }

    @Test
    void usesDegreesForTrigonometryAndKnowsTheMathFunctions() {
        assertEquals(1f, value("math.sin(90)"), EPSILON);
        assertEquals(-1f, value("Math.cos(180)"), EPSILON);
        assertEquals(3f, value("math.clamp(5, 0, 3)"), EPSILON);
        assertEquals(5f, value("math.lerp(0, 10, 0.5)"), EPSILON);
        assertEquals(8f, value("math.pow(2, 3)"), EPSILON);
    }

    @Test
    void readsTheAnimationTime() {
        CompiledMolang compiled = Molang.compile("math.sin(query.anim_time * 90) * 10");
        MolangContext context = new MolangContext(new Random(1));
        context.animTime(1f);

        assertFalse(compiled.constant());
        assertEquals(10f, compiled.expression().evaluate(context), EPSILON);
    }

    @Test
    void comparesAndChoosesWithTheTernary() {
        assertEquals(1f, value("2 >= 1 && !(1 == 2)"), EPSILON);
        assertEquals(5f, value("1 < 2 ? 5 : 6"), EPSILON);
        assertEquals(6f, value("1 > 2 ? 5 : 6"), EPSILON);
    }

    @Test
    void setsAndReadsVariablesAcrossStatements() {
        assertEquals(12f, value("v.speed = 4; temp.twice = v.speed * 2; return v.speed + t.twice;"), EPSILON);
    }

    @Test
    void listsWhatItDoesNotKnowAndReadsItAsZero() {
        CompiledMolang compiled = Molang.compile("query.ground_speed * 2 + math.die_roll(1, 2, 3)");

        assertEquals(2, compiled.unsupported().size());
        assertEquals(0f, compiled.expression().evaluate(new MolangContext(new Random(1))), EPSILON);
    }

    @Test
    void refusesTextThatIsNotMolang() {
        assertThrows(MolangException.class, () -> Molang.compile("1 + * 2"));
        assertThrows(MolangException.class, () -> Molang.compile("(1 + 2"));
    }

    private static float value(final String text) {
        return Molang.compile(text).expression().evaluate(new MolangContext(new Random(1)));
    }
}
