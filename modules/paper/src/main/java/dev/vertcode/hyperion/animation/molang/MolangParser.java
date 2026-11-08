package dev.vertcode.hyperion.animation.molang;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

/**
 * Read Molang text into an expression, by recursive descent.
 *
 * <p>Statements are separated by {@code ;}; the value is what {@code return} gives, or the last
 * statement's. Names are read without case. A name or function Hyperion does not know reads as
 * 0 and is noted, so a load warning can name it.
 */
final class MolangParser {

    private static final String UNEXPECTED = "unexpected '%s' at %d in '%s'";
    private static final String UNFINISHED = "'%s' ends too early";
    private static final float DEGREES = (float) (180d / Math.PI);
    private static final float RADIANS = (float) (Math.PI / 180d);

    private final String text;
    private final List<String> unsupported = new ArrayList<>();
    private int at;
    private boolean dynamic;

    /**
     * Prepare to read one text.
     *
     * @param text the Molang text
     */
    MolangParser(final String text) {
        this.text = text;
    }

    /**
     * Read the whole text.
     *
     * @return the compiled text
     * @throws MolangException when the text is not valid Molang
     */
    CompiledMolang parse() {
        List<MolangExpression> statements = new ArrayList<>();
        while (true) {
            this.skipSpace();
            if (this.at >= this.text.length()) {
                break;
            }

            if (this.peek(';')) {
                this.at++;
                continue;
            }

            if (this.keyword("return")) {
                statements.add(this.expression());
                break;
            }

            statements.add(this.statement());
        }

        this.skipSpace();
        if (this.at < this.text.length() && !this.peek(';')) {
            throw this.unexpected();
        }

        MolangExpression program = sequence(statements);

        return new CompiledMolang(program, this.unsupported, !this.dynamic);
    }

    /**
     * Join statements into one expression whose value is the last one's.
     *
     * @param statements the statements
     * @return the expression
     */
    private static MolangExpression sequence(final List<MolangExpression> statements) {
        if (statements.isEmpty()) {
            return context -> 0f;
        }

        if (statements.size() == 1) {
            return statements.getFirst();
        }

        MolangExpression[] all = statements.toArray(MolangExpression[]::new);

        return context -> {
            float value = 0f;
            for (MolangExpression statement : all) {
                value = statement.evaluate(context);
            }

            return value;
        };
    }

    /**
     * Read a statement: an assignment to a variable, or an expression.
     *
     * @return the statement
     */
    private MolangExpression statement() {
        int start = this.at;
        String name = this.tryName();
        this.skipSpace();
        if (name != null && this.peek('=') && !this.peekAhead("==")) {
            this.at++;
            String variable = variableName(name);
            if (variable == null) {
                throw new MolangException(String.format(UNEXPECTED, "=", this.at, this.text));
            }

            this.dynamic = true;
            MolangExpression value = this.expression();

            return context -> {
                float result = value.evaluate(context);
                context.variable(variable, result);

                return result;
            };
        }

        this.at = start;

        return this.expression();
    }

    private MolangExpression expression() {
        MolangExpression condition = this.nullish();
        this.skipSpace();
        if (!this.peek('?') || this.peekAhead("??")) {
            return condition;
        }

        this.at++;
        MolangExpression then = this.expression();
        this.skipSpace();
        MolangExpression otherwise = context -> 0f;
        if (this.peek(':')) {
            this.at++;
            otherwise = this.expression();
        }

        MolangExpression orElse = otherwise;

        return context -> condition.evaluate(context) != 0f ? then.evaluate(context) : orElse.evaluate(context);
    }

    private MolangExpression nullish() {
        MolangExpression left = this.or();
        while (this.match("??")) {
            // Every value Hyperion knows is set, so the left side always wins.
            this.or();
        }

        return left;
    }

    private MolangExpression or() {
        MolangExpression left = this.and();
        while (this.match("||")) {
            MolangExpression first = left;
            MolangExpression second = this.and();
            left = context -> first.evaluate(context) != 0f || second.evaluate(context) != 0f ? 1f : 0f;
        }

        return left;
    }

    private MolangExpression and() {
        MolangExpression left = this.equality();
        while (this.match("&&")) {
            MolangExpression first = left;
            MolangExpression second = this.equality();
            left = context -> first.evaluate(context) != 0f && second.evaluate(context) != 0f ? 1f : 0f;
        }

        return left;
    }

    private MolangExpression equality() {
        MolangExpression left = this.comparison();
        while (true) {
            MolangExpression first = left;
            if (this.match("==")) {
                MolangExpression second = this.comparison();
                left = context -> first.evaluate(context) == second.evaluate(context) ? 1f : 0f;
            } else if (this.match("!=")) {
                MolangExpression second = this.comparison();
                left = context -> first.evaluate(context) != second.evaluate(context) ? 1f : 0f;
            } else {
                return left;
            }
        }
    }

    private MolangExpression comparison() {
        MolangExpression left = this.additive();
        while (true) {
            MolangExpression first = left;
            if (this.match("<=")) {
                MolangExpression second = this.additive();
                left = context -> first.evaluate(context) <= second.evaluate(context) ? 1f : 0f;
            } else if (this.match(">=")) {
                MolangExpression second = this.additive();
                left = context -> first.evaluate(context) >= second.evaluate(context) ? 1f : 0f;
            } else if (this.match("<")) {
                MolangExpression second = this.additive();
                left = context -> first.evaluate(context) < second.evaluate(context) ? 1f : 0f;
            } else if (this.match(">")) {
                MolangExpression second = this.additive();
                left = context -> first.evaluate(context) > second.evaluate(context) ? 1f : 0f;
            } else {
                return left;
            }
        }
    }

    private MolangExpression additive() {
        MolangExpression left = this.multiplicative();
        while (true) {
            MolangExpression first = left;
            if (this.match("+")) {
                MolangExpression second = this.multiplicative();
                left = context -> first.evaluate(context) + second.evaluate(context);
            } else if (this.match("-")) {
                MolangExpression second = this.multiplicative();
                left = context -> first.evaluate(context) - second.evaluate(context);
            } else {
                return left;
            }
        }
    }

    private MolangExpression multiplicative() {
        MolangExpression left = this.unary();
        while (true) {
            MolangExpression first = left;
            if (this.match("*")) {
                MolangExpression second = this.unary();
                left = context -> first.evaluate(context) * second.evaluate(context);
            } else if (this.match("/")) {
                MolangExpression second = this.unary();
                left = context -> {
                    float divisor = second.evaluate(context);
                    return divisor == 0f ? 0f : first.evaluate(context) / divisor;
                };
            } else {
                return left;
            }
        }
    }

    private MolangExpression unary() {
        if (this.match("-")) {
            MolangExpression value = this.unary();
            return context -> -value.evaluate(context);
        }

        if (this.match("+")) {
            return this.unary();
        }

        if (this.match("!")) {
            MolangExpression value = this.unary();
            return context -> value.evaluate(context) == 0f ? 1f : 0f;
        }

        return this.primary();
    }

    private MolangExpression primary() {
        this.skipSpace();
        if (this.at >= this.text.length()) {
            throw new MolangException(String.format(UNFINISHED, this.text));
        }

        if (this.match("(")) {
            MolangExpression inner = this.expression();
            if (!this.match(")")) {
                throw this.unexpected();
            }

            return inner;
        }

        char next = this.text.charAt(this.at);
        if (Character.isDigit(next) || next == '.') {
            float value = this.number();
            return context -> value;
        }

        String name = this.tryName();
        if (name == null) {
            throw this.unexpected();
        }

        this.skipSpace();
        if (this.peek('(')) {
            this.at++;
            List<MolangExpression> arguments = new ArrayList<>();
            this.skipSpace();
            if (!this.match(")")) {
                do {
                    arguments.add(this.expression());
                } while (this.match(","));

                if (!this.match(")")) {
                    throw this.unexpected();
                }
            }

            return this.function(name, arguments);
        }

        return this.name(name);
    }

    /**
     * Turn a name into the value it reads.
     *
     * @param name the name, lowercase
     * @return the expression
     */
    private MolangExpression name(final String name) {
        MolangExpression known = switch (name) {
            case "true" -> context -> 1f;
            case "false" -> context -> 0f;
            case "math.pi" -> context -> (float) Math.PI;
            case "query.anim_time", "q.anim_time", "query.life_time", "q.life_time" -> {
                this.dynamic = true;
                yield MolangContext::animTime;
            }
            default -> null;
        };
        if (known != null) {
            return known;
        }

        String variable = variableName(name);
        if (variable != null) {
            this.dynamic = true;
            return context -> context.variable(variable);
        }

        this.unsupported.add(name);

        return context -> 0f;
    }

    /**
     * Turn a function call into the value it gives.
     *
     * @param name the function's name, lowercase
     * @param arguments the arguments
     * @return the expression
     */
    private MolangExpression function(final String name, final List<MolangExpression> arguments) {
        MolangExpression a = argument(arguments, 0);
        MolangExpression b = argument(arguments, 1);
        MolangExpression c = argument(arguments, 2);

        return switch (name) {
            case "math.sin" -> context -> (float) Math.sin(a.evaluate(context) * RADIANS);
            case "math.cos" -> context -> (float) Math.cos(a.evaluate(context) * RADIANS);
            case "math.asin" -> context -> (float) Math.asin(a.evaluate(context)) * DEGREES;
            case "math.acos" -> context -> (float) Math.acos(a.evaluate(context)) * DEGREES;
            case "math.atan" -> context -> (float) Math.atan(a.evaluate(context)) * DEGREES;
            case "math.atan2" -> context -> (float) Math.atan2(a.evaluate(context), b.evaluate(context)) * DEGREES;
            case "math.abs" -> context -> Math.abs(a.evaluate(context));
            case "math.sqrt" -> context -> (float) Math.sqrt(a.evaluate(context));
            case "math.pow" -> context -> (float) Math.pow(a.evaluate(context), b.evaluate(context));
            case "math.exp" -> context -> (float) Math.exp(a.evaluate(context));
            case "math.ln" -> context -> (float) Math.log(a.evaluate(context));
            case "math.floor" -> context -> (float) Math.floor(a.evaluate(context));
            case "math.ceil" -> context -> (float) Math.ceil(a.evaluate(context));
            case "math.round" -> context -> Math.round(a.evaluate(context));
            case "math.trunc" -> context -> (float) (long) a.evaluate(context);
            case "math.mod" -> context -> {
                float divisor = b.evaluate(context);
                return divisor == 0f ? 0f : a.evaluate(context) % divisor;
            };
            case "math.min" -> context -> Math.min(a.evaluate(context), b.evaluate(context));
            case "math.max" -> context -> Math.max(a.evaluate(context), b.evaluate(context));
            case "math.clamp" -> context -> Math.clamp(a.evaluate(context), b.evaluate(context), c.evaluate(context));
            case "math.lerp" -> context -> {
                float from = a.evaluate(context);
                return from + (b.evaluate(context) - from) * c.evaluate(context);
            };
            case "math.lerprotate" -> context -> {
                float from = a.evaluate(context);
                float turn = ((b.evaluate(context) - from) % 360f + 540f) % 360f - 180f;
                return from + turn * c.evaluate(context);
            };
            case "math.hermite_blend" -> context -> {
                float t = a.evaluate(context);
                return 3f * t * t - 2f * t * t * t;
            };
            case "math.random" -> {
                this.dynamic = true;
                yield context -> {
                    float low = a.evaluate(context);
                    return low + context.random().nextFloat() * (b.evaluate(context) - low);
                };
            }
            case "math.random_integer" -> {
                this.dynamic = true;
                yield context -> {
                    int low = Math.round(a.evaluate(context));
                    int high = Math.round(b.evaluate(context));
                    return high <= low ? low : low + context.random().nextInt(high - low + 1);
                };
            }
            default -> {
                this.unsupported.add(name + "()");
                yield context -> 0f;
            }
        };
    }

    /**
     * Return an argument, or 0 when the call has fewer.
     *
     * @param arguments the arguments
     * @param index the argument's place
     * @return the argument
     */
    private static MolangExpression argument(final List<MolangExpression> arguments, final int index) {
        return index < arguments.size() ? arguments.get(index) : context -> 0f;
    }

    /**
     * Return a variable's name without its prefix.
     *
     * @param name the full name, such as {@code variable.speed}
     * @return the name, such as {@code speed}, or null when it names no variable
     */
    private static @Nullable String variableName(final String name) {
        for (String prefix : new String[] {"variable.", "v.", "temp.", "t."}) {
            if (name.startsWith(prefix) && name.length() > prefix.length()) {
                return name.substring(prefix.length());
            }
        }

        return null;
    }

    /**
     * Read a number.
     *
     * @return the number
     */
    private float number() {
        int start = this.at;
        while (this.at < this.text.length()) {
            char next = this.text.charAt(this.at);
            if (!Character.isDigit(next) && next != '.') {
                break;
            }

            this.at++;
        }

        try {
            float value = Float.parseFloat(this.text.substring(start, this.at));
            if (this.at < this.text.length() && Character.toLowerCase(this.text.charAt(this.at)) == 'f') {
                this.at++;
            }

            return value;
        } catch (NumberFormatException exception) {
            this.at = start;
            throw this.unexpected();
        }
    }

    /**
     * Read a dotted name, such as {@code query.anim_time}, when one starts here.
     *
     * @return the name in lowercase, or null when none starts here
     */
    private @Nullable String tryName() {
        this.skipSpace();
        int start = this.at;
        while (this.at < this.text.length()) {
            char next = this.text.charAt(this.at);
            boolean part = Character.isLetterOrDigit(next) || next == '_' || next == '.';
            if (!part || (this.at == start && !Character.isLetter(next) && next != '_')) {
                break;
            }

            this.at++;
        }

        return this.at == start ? null : this.text.substring(start, this.at).toLowerCase(Locale.ROOT);
    }

    /**
     * Read a keyword followed by something other than a name character.
     *
     * @param keyword the keyword
     * @return true when it was read
     */
    private boolean keyword(final String keyword) {
        this.skipSpace();
        int end = this.at + keyword.length();
        if (!this.text.regionMatches(true, this.at, keyword, 0, keyword.length())) {
            return false;
        }

        if (end < this.text.length() && (Character.isLetterOrDigit(this.text.charAt(end)) || this.text.charAt(end) == '_')) {
            return false;
        }

        this.at = end;

        return true;
    }

    /**
     * Read a symbol when it comes next.
     *
     * @param symbol the symbol
     * @return true when it was read
     */
    private boolean match(final String symbol) {
        this.skipSpace();
        if (!this.text.startsWith(symbol, this.at)) {
            return false;
        }

        // A single symbol must not be the start of a longer one: < is not <=, ! is not !=.
        boolean single = symbol.length() == 1;
        if (single && this.at + 1 < this.text.length()) {
            char after = this.text.charAt(this.at + 1);
            boolean longer = (after == '=' && "<>!=".indexOf(symbol.charAt(0)) >= 0)
                    || (after == symbol.charAt(0) && "|&?".indexOf(after) >= 0);
            if (longer) {
                return false;
            }
        }

        this.at += symbol.length();

        return true;
    }

    private boolean peek(final char symbol) {
        return this.at < this.text.length() && this.text.charAt(this.at) == symbol;
    }

    private boolean peekAhead(final String symbol) {
        return this.text.startsWith(symbol, this.at);
    }

    private void skipSpace() {
        while (this.at < this.text.length() && Character.isWhitespace(this.text.charAt(this.at))) {
            this.at++;
        }
    }

    private MolangException unexpected() {
        String found = this.at < this.text.length() ? String.valueOf(this.text.charAt(this.at)) : "end";

        return new MolangException(String.format(UNEXPECTED, found, this.at, this.text));
    }
}
