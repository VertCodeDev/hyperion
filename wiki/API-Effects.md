# API Effects

Play effects from your plugin, ship your own effect files, build effects in code, and add part
types. For what an effect is and how the YAML looks, see [Effects](Effects).

## Playing

```java
Key levelUp = Key.key("hyperion", "level_up");

EffectHandle handle = Hyperion.effects().play(levelUp, EffectAnchor.on(model));
```

`play` is safe from any thread. The effect starts on the next tick of the thread that owns its
anchor.

### Anchors

The anchor is what the effect plays on, and follows.

| Anchor | Plays |
|---|---|
| `EffectAnchor.at(location)` | At a spot |
| `EffectAnchor.on(entity)` | On an entity, following it |
| `EffectAnchor.on(model)` | On a model |
| `EffectAnchor.bone(model, "jaw")` | On a bone, turning with it |
| `EffectAnchor.locator(model, "mouth")` | On a Blockbench locator, turning with it |

### Options

```java
Hyperion.effects().play(Key.key("hyperion", "aura"), EffectAnchor.on(player), options -> options
        .parameter("color", "#FF5555")
        .viewers(EffectViewers.nearby(32))
        .onEnd(() -> player.sendMessage("The aura fades.")));
```

| Option | What it does |
|---|---|
| `parameter(name, value)` | Fills in a `%name%` of the effect |
| `viewers(EffectViewers)` | Who sees it: `model()`, `nearby(radius)`, `everyone()` or `only(uuid)` |
| `camera(players...)` | The players whose view the effect's camera takes |
| `onEnd(Runnable)` | Something to run when it ends |

`EffectViewers.only(uuid)` makes a private effect, such as a quest marker one player sees.

### The handle

```java
handle.isPlaying();
handle.tick();        // how far along it is
handle.location();    // where it is now
handle.stop();
```

A looping effect plays until you stop it, so keep its handle.

## Cutscenes

A camera never takes everyone's view, only the players you name:

```java
Hyperion.effects().play(Key.key("myplugin", "boss_intro"), EffectAnchor.on(boss), options -> options
        .camera(player)
        .onEnd(() -> startFight(player)));
```

The view goes back by itself when the effect ends, and when the player dies, changes worlds or
leaves.

## Shipping effect files

Register a folder of `.yml` files in your own namespace. They load now and on every `/hy reload`.

```java
@Override
public void onEnable() {
    saveResource("effects/spells.yml", false);
    int loaded = Hyperion.effects().registerFolder(this, "myplugin", getDataPath().resolve("effects"));
    getLogger().info("Loaded " + loaded + " effects");
}
```

A section `fireball` in that folder plays as `myplugin:fireball`.

## Building an effect in code

The builder takes the same settings as the YAML.

```java
Effect sparkle = Effect.builder(Key.key("myplugin", "sparkle"), 40)
        .part(EffectPart.particle("ring")
                .setting("particle", "END_ROD")
                .setting("shape", "ring")
                .setting("count", 12)
                .setting("every", 2)
                .keyframe(0, frame -> frame.radius(0.2f))
                .keyframe(30, Easing.EASE_OUT, frame -> frame.radius(1.2f)))
        .part(EffectPart.sound("chime")
                .keyframe(0, frame -> frame.sound("block.amethyst_block.chime").pitch(1.2f)))
        .build();

Hyperion.effects().register(this, sparkle);
```

`EffectPart` has a starter for every built-in type: `particle`, `burst`, `sound`, `event`, `text`,
`item`, `block`, `model`, `title`, `actionbar`, `message`, `command`, `time` and `camera`.

An effect you do not want to register can be played directly:

```java
Hyperion.effects().play(sparkle, EffectAnchor.at(location), options -> { });
```

## Reacting to an effect

An `event` part fires `EffectKeyframeEvent` at each of its keyframes, with the keyframe's name and
where the effect is. It is how an effect tells your plugin when to act:

```yaml
fire_breath:
  duration: 40
  parts:
    hit:
      type: event
      keyframes:
        12: { name: breath_damage }
```

```java
@EventHandler
public void onKeyframe(EffectKeyframeEvent event) {
    if (event.name().equals("breath_damage")) {
        // hurt players in front of event.location()
    }
}
```

## Your own part types

A part type adds a new `type:` that effect files can use, such as `myplugin:shake`.

```java
public final class ShakeType implements EffectPartType {

    private static final Key KEY = Key.key("myplugin", "shake");

    @Override
    public Key key() {
        return KEY;
    }

    @Override
    public boolean sampled() {
        return false;   // act once at each keyframe, like a sound
    }

    @Override
    public EffectActor create(EffectPart part, EffectContext context) {
        return (tick, frame) -> {
            float strength = frame.number("strength", 1f);
            for (Player viewer : context.viewers()) {
                // shake this viewer's screen
            }
        };
    }
}
```

```java
Hyperion.effects().registerPartType(this, new ShakeType());
```

```yaml
stomp:
  duration: 20
  parts:
    rumble:
      type: myplugin:shake
      keyframes:
        5: { strength: 2 }
```

| | |
|---|---|
| `sampled()` | True to act every tick on values eased between keyframes, like an emitter. False to act once at each keyframe. |
| `EffectActor#frame(tick, frame)` | Your code, with the values for this moment |
| `EffectActor#stop()` | Clean up when the effect ends |
| `context.viewers()` | The players who see the effect |
| `context.location(frame)` | Where the part is, from its `at` and `offset` |
| `context.fill(text)` | Fills in the effect's `%parameters%` |
| `context.particles(count)` | A particle count scaled by `effects.particle-multiplier` |

Read values from the frame with `frame.number`, `frame.string`, `frame.bool` and `frame.vector`.
