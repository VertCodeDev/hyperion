# API Animations

Every model has an `Animator`. Playing and stopping are safe from any thread.

## Playing

```java
Animator animator = model.animator();

AnimationHandle roar = animator.play("roar");
```

The name is the animation's name in Blockbench, and it is case sensitive. Check what a blueprint
has with `blueprint.animations()`.

With options:

```java
animator.play("walk", options -> options
        .speed(1.5f)
        .loop(LoopMode.LOOP)
        .fadeIn(5)
        .fadeOut(5));
```

| Option | What it does |
|---|---|
| `speed(float)` | 1 is normal, 2 is twice as fast |
| `loop(LoopMode)` | `ONCE` plays and stops, `LOOP` starts again, `HOLD` stays on the last frame |
| `fadeIn(int)` | Ticks to blend in over |
| `fadeOut(int)` | Ticks to blend out over when it ends or is stopped |

Without options, an animation uses the loop mode it has in Blockbench.

## Several at once

Animations play together and add up per bone, as in Blockbench. So a `walk` and a `wave` can run
at the same time. An animation marked to override in Blockbench replaces those started before it.

## The handle

`play` returns a handle to that one playing animation.

```java
AnimationHandle attack = animator.play("attack");

attack.onEnd(() -> animator.play("idle"));
attack.speed(0.5f);      // change the speed while it plays
attack.stop();
```

| Method | What it gives |
|---|---|
| `name()` | The animation's name |
| `isPlaying()` | Whether it still runs |
| `time()`, `length()` | Where it is, and how long it is, in seconds |
| `speed()`, `speed(float)` | Its speed |
| `loop()` | Its loop mode |
| `onEnd(Runnable)` | Something to run when it ends |
| `stop()` | Stops it, fading out if it has a fade |

## Stopping and asking

```java
animator.stop("walk");
animator.stopAll();

Optional<AnimationHandle> walking = animator.playing("walk");
Collection<AnimationHandle> all = animator.playing();
```

## Chaining animations

```java
public void attack(Model model) {
    model.animator().play("windup", options -> options.loop(LoopMode.ONCE))
            .onEnd(() -> model.animator().play("slam")
                    .onEnd(() -> model.animator().play("recover")));
}
```

## Automatic idle and walk

A model on an entity plays `idle` and `walk` by itself. When your plugin drives the animations,
turn that off:

```java
model.autoAnimations(false);
// or at spawn
blueprint.attach(entity, options -> options.autoAnimations(false));
```

## Per-player animations

An animation played on a player's view is seen by that player only, on top of what everyone sees.

```java
model.view(player).animator().play("greet");
```

Its sounds and particles from Blockbench keyframes also go to that player only.

## Reacting to an animation

Three events tell you what an animation is doing. `viewer()` is the player for a per-player
animation, and null otherwise.

```java
@EventHandler
public void onEnd(AnimationEndEvent event) {
    if (event.animation().name().equals("death") && event.finished()) {
        event.model().remove();
    }
}
```

**Timeline keyframes** are how an animation tells your plugin "now". Put a timeline keyframe in
Blockbench at the frame a sword lands, with a word as its script, and act when it fires:

```java
@EventHandler
public void onKeyframe(AnimationKeyframeEvent event) {
    if (event.script().equals("slam")) {
        Location center = event.model().location();
        // hurt players near the center
    }
}
```

This keeps the damage in step with the animation at any speed, without counting ticks yourself.
See [API Events](API-Events) for the full list.
