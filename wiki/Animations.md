# Animations

Hyperion plays the animations you make in Blockbench's Animate tab. They are stored in the same
`.bbmodel` file as the model.

## What is supported

- Rotation, position and scale keyframes.
- Linear, smooth, step and bezier interpolation.
- Loop, play once, and hold on the last frame.
- Several animations at once. They add up per bone, as in Blockbench. An animation marked to
  override replaces those started before it.
- Speed control, and fading in and out.
- Molang in keyframes: arithmetic, comparisons, `math.*`, `query.anim_time`, `query.life_time`
  and variables. Anything else is a warning in `/hy validate` and reads as 0.
- Effect keyframes: sounds, particles and timeline events, see [Effects](Effects).

## Idle and walk play by themselves

A model on an entity plays `idle` while the entity stands still and `walk` while it moves, when
the model has animations with those names.

Change this under `models.auto-animations` in `config.yml`:

```yaml
models:
  auto-animations:
    enabled: true
    idle: idle
    walk: walk
    # A choice per entity type that wins over "enabled".
    entities:
      armor_stand: false
```

A plugin can also turn it off for one model.

## Playing animations by hand

These commands act on the model nearest to you:

| Command | What it does |
|---|---|
| `/hy animate play <animation> [speed]` | Plays an animation |
| `/hy animate stop [animation]` | Stops one animation, or all of them |

You can also sneak and right-click a model to open its menu, and play, loop or stop its animations
from there. See [Staff Tools](Staff-Tools).

## Smoothness

Clients smooth each move of a bone over `models.interpolation` ticks, 2 by default. 0 snaps every
move. Higher values are smoother but trail behind.

## Per-player animations

A plugin can play an animation for one player only: the same model then does something different
for each viewer. See [Developer API](Developer-API).

## How it stays cheap

Every animation is worked out once when its model loads, off the server thread. Every model
playing it shares that data, so playing an animation only reads and blends numbers. Bones that no
animation moves are not sent at all.
