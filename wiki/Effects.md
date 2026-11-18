# Effects

An effect is particles, sounds and more on a timeline. It can play on a model, a bone, a
Blockbench locator, an entity or a spot in the world.

## Where effects live

Every top-level section of a `.yml` file in `plugins/Hyperion/effects` is one effect, played as
`hyperion:<section>`. The first start writes `example.yml`, with every option explained and six
effects to try. A server that already has the folder keeps its own `example.yml`.

```yaml
level_up:
  duration: 40          # in ticks, 20 a second
  parts:
    spiral:
      type: particle
      particle: END_ROD
      shape: helix
      count: 12
      radius: 0.8
      height: 2.2
      every: 2
      keyframes:
        0: { radius: 0.2 }
        20: { radius: 1.0, easing: ease-out }
        36: { radius: 0.3, easing: ease-in }
        38: { visible: false }

    chime:
      type: sound
      keyframes:
        0: { sound: block.amethyst_block.chime, pitch: 1.2 }
        20: { sound: entity.player.levelup, volume: 0.8, pitch: 1.4 }
```

Try it with `/hy effect play level_up` while you stand near a model.

## Part types

An effect has parts. Each part has a type and keyframes at ticks.

| Type | What it does |
|---|---|
| `particle` | An emitter: a point, ring, helix, sphere or line of particles every few ticks while visible |
| `burst` | A burst of particles at each keyframe |
| `sound` | A sound at each keyframe, with volume, pitch and category |
| `event` | Fires an event plugins listen to at each keyframe, with the keyframe's `name` |
| `text`, `item`, `block` | A prop in the air. Its rotation, scale and offset move between keyframes. |
| `model` | A Hyperion model as a prop, playing one of its animations |
| `title`, `actionbar`, `message` | MiniMessage text on each viewer's screen at each keyframe |
| `command` | A console command at each keyframe, once per viewer, with `%player%` |
| `time` | Each viewer's own sky time, put back when the effect ends |
| `camera` | Moves the view of chosen players along its keyframes |

## Keyframes and easing

Numbers and `[x, y, z]` values move smoothly from one keyframe to the next, eased by the later
keyframe's `easing`: `linear`, `ease-in`, `ease-out`, `ease-in-out`, `ease-in-back`,
`ease-out-back` or `ease-out-bounce`.

## Where a part is

```yaml
at: anchor            # where the effect is played (the default)
at: bone:jaw          # a bone of the model, turning with it
at: locator:mouth     # a Blockbench locator of the model, turning with it
at: spiral            # another part, such as an emitter that moves
offset: [0, 1.5, 0]   # blocks to the right, up and forward from there
```

The offset turns with what the part sits on, so a breath on a jaw tilts with the jaw.

## Who sees it

```yaml
viewers: model        # the players who see the model (the default)
viewers: nearby:32    # players within 32 blocks
viewers: everyone
```

## Props that move on their own

A `text`, `item` or `block` prop can rise and fall on top of its keyframes:

```yaml
bob: 0.15         # blocks it rises
bob-period: 30    # ticks for one rise and fall (40 when left out)
```

A text's background can fade and breathe:

```yaml
background: "#A0101018"
alpha: 0.5        # 0 to 1, how solid the background is. Eases between keyframes.
pulse: 30         # ticks for one dim and back
pulse-floor: 0.3  # how far it dims, as a part of itself (0.3 when left out)
```

## Heavy parts

Mark the costly parts of an effect, and players who want fewer effects go without them:

```yaml
sparks:
  type: particle
  heavy: true
```

A player wants fewer effects when they have the permission `hyperion.effects.reduced`, or when
another plugin says so through the [API](API-Effects#players-who-want-fewer-effects). The
permission is off for everyone by default, operators included. A plugin that gives a player every
permission (`*`) also gives them this one.

## A player's own effect

Some effects are one player's moment, such as a crate they open. An effect played as a player's
own:

- is always seen and heard by that player,
- ends when they leave,
- takes the turn of the effect they owned before, which stops.

Plugins choose the owner when they play an effect. To try it: `/hy effect play <effect> --own`.

Three settings of an effect go with it:

```yaml
crate:
  duration: 80
  reveal: 30          # the tick it shows the prize
  hide-players: 3     # other players this close are hidden from its viewers meanwhile
  ambient: false      # true: plays beside the owner's effect in place of taking its turn
```

- **`reveal`** tells the plugin that played the effect when the prize shows, so it can hold its
  chat message back until then.
- **`hide-players`** hides the players within that many blocks of where the effect starts, from
  everyone who sees it when it starts. The owner is never hidden. They come back when it ends.
- **`ambient`** is for small effects that pile up, such as a number popping out of a block.

The `crate` effect in `example.yml` uses all of this.

## Parameters

An effect can have values that whoever plays it fills in:

```yaml
aura:
  duration: 40
  loop: true
  parameters:
    color: "#7FD4FF"    # the default
  parts:
    ring:
      type: particle
      particle: DUST
      color: "%color%"
```

With PlaceholderAPI installed, titles, action bars, messages and commands also fill in its
placeholders for each viewer.

## Effects from Blockbench

Effect keyframes in a Blockbench animation play as the animation passes them:

| Blockbench keyframe | What happens |
|---|---|
| Sound | Plays the sound |
| Particle | Plays the effect it names at its locator. A plain particle name gives a burst of that particle. |
| Timeline (script) | Fires an event plugins listen to, with the script text |

A model can also carry its own effects in a `hyperion_effects` field of its `.bbmodel`, in the same
form as the YAML. They are named `<model key>/<name>`. For safety, a model's own effects cannot
run commands.

## Cameras

A `camera` part moves a player's view along its keyframes, looking at a spot, bone, locator or
another part. It makes cutscenes possible.

A camera never takes everyone's view. It only takes the view of players the play names:
`/hy effect play <effect> --camera` for yourself, or players a plugin picks. The view goes back
when the camera hides, the effect ends, the player sneaks on a `skippable` camera, or they die,
change worlds or leave. By default the player takes no damage meanwhile.

## Commands

| Command | What it does |
|---|---|
| `/hy effect play <effect>` | Plays an effect on the nearest model |
| `/hy effect play <effect> --here` | Plays it where you stand |
| `/hy effect play <effect> --camera` | Lets its camera take your view |
| `/hy effect play <effect> --own` | Plays it as your own, in place of the one you owned before |
| `/hy effect stop` | Stops every effect within 16 blocks |
| `/hy effect list` | Lists every loaded effect, each a click away from playing it |

`/hy gallery --effects` lists them in a menu.

## Settings

```yaml
effects:
  # How far away players see an effect that is not on a model.
  view-distance: 32
  # Scales every particle count: 0.5 halves them on a busy server, 0 sends none.
  particle-multiplier: 1.0
```

Problems in an effect file show in `/hy validate`.
