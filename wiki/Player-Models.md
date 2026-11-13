# Player Models

A player model is a model that wears a player's skin. Put one on a player and it takes their
place, so you can animate the player themselves:

- **Emotes:** wave, dance, bow.
- **Poses that last:** hands up while a player runs off with a stolen egg, carrying a crate,
  limping while hurt.
- **Standing still:** sitting, lying down, kneeling.
- **NPCs and statues** in any player's skin.

> [!IMPORTANT]
> Skins are drawn from Minecraft 1.21.4. On 1.21 to 1.21.3 a player model still works, but it
> shows the skin saved in its own file, not the player's.

## Quick start

1. Copy `plugins/Hyperion/player-template.bbmodel` into `plugins/Hyperion/models` and name it
   `player.bbmodel`.
2. Run `/hy reload`.
3. Run `/hy player play player wave`.

You wave, wearing your own skin, and everyone near you sees it. Now try
`/hy player play player hands_up` and run around: your hands stay up while your legs walk.
`/hy player stop` puts you back.

The template is a ready player rig with five animations: `idle`, `walk`, `wave`, `hands_up` and
`sit`. Open it in [Blockbench](https://www.blockbench.net/) and add your own.

## Playing an animation on a player

| Command | What it does |
|---|---|
| `/hy player play <model> <animation>` | Plays an animation on you |
| `/hy player play <model> <animation> <player>` | Plays it on another player |
| `/hy player stop [player]` | Stops it and shows the player again |

| Flag | What it does |
|---|---|
| `--loop` | Keeps the animation going until it is stopped |
| `--lock` | Holds the player in place while it plays. For sitting, lying down, dances. |
| `--no-walk` | Leaves the model's `idle` and `walk` animations off |
| `--stop-on-sneak` | Ends it when the player sneaks |
| `--stop-on-damage` | Ends it when the player takes damage |

The commands are for trying things out and for command blocks, menus and other plugins that run
commands. A plugin of your own does the same through the api, see
[API Player Models](API-Player-Models).

### How long it lasts

That depends on the animation's loop mode in Blockbench:

| Loop mode | What happens |
|---|---|
| **Once** | The player is shown again when the animation is over. For an emote. |
| **Hold** | The animation stays on its last frame until it is stopped. For hands up, sitting. |
| **Loop** | The animation repeats until it is stopped. For a dance, a limp. |

`--loop` makes any animation repeat. An animation also ends when the player dies, leaves or
changes worlds.

### While walking

The player keeps walking, and the model plays its `walk` animation while they move and `idle`
while they stand, under the animation you played.

Animations add up. So an animation that moves only the arms, like `hands_up`, keeps the hands up
while the legs walk. That is how you make a player run with their hands in the air: animate the
arms only, and leave the legs to `walk`.

An animation that moves the whole body is better played with `--no-walk` or `--lock`.

### Standing still

`/hy player play player sit --lock` holds the player where they are. They can still look around.

## Making a player model

A player model is a normal Hyperion model with one rule: **faces that use a texture named
`player_skin` are drawn with the player's skin.**

So the easiest start is the template. To make your own:

1. Give the Blockbench project a 64 by 64 texture named `player_skin`. Any skin works as its
   image: it is only what you see while you work, and what shows on servers older than 1.21.4.
2. Lay the faces out on it as on a Minecraft skin. The template has the right UVs for every
   part.
3. Animate it like any other model.

Other textures work as usual, so a player model can carry props: the stolen egg, a guitar,
wings, a chair. Only the `player_skin` faces change with the player.

### Slim and wide arms

Skins come with two arm shapes: wide, 4 pixels, and slim, 3 pixels. A player model has both, and
Hyperion shows the one that fits the skin:

| Name ends in | Drawn for |
|---|---|
| `_slim` | Skins with slim arms |
| `_wide` or `_classic` | Skins with wide arms |
| anything else | Every skin |

This works on a cube's name and on a bone's name. The template names its arm cubes
`right_arm_wide`, `right_arm_slim` and so on, inside one `right_arm` bone, so you animate each
arm once.

### The rig of the template

```
player
└── hip
    ├── body
    │   ├── head
    │   ├── right_arm
    │   │   └── right_forearm
    │   └── left_arm
    │       └── left_forearm
    ├── right_leg
    │   └── right_foreleg
    └── left_leg
        └── left_foreleg
```

Arms and legs are in two parts, so elbows and knees bend. The `head` bone turns with where the
player looks, like any [head bone](Bone-Behaviors).

The model is 32 pixels tall. On a player it is drawn a little smaller, as the game draws a
player.

## Good to know

- **Keep your animations in one model.** A player model adds about 1 MB and 3,000 small files to
  the resource pack, because every skin pixel is its own face. One model with twenty animations
  costs that once; twenty models cost it twenty times.
- **Your own view.** You see all of your own model in third person (F5). In first person you see
  the parts that come in front of you, such as a raised arm. Others never see your real body
  while an animation plays, but in your own third-person view your armor and held item still
  show. Turn `player-animations.show-to-self` off to not see your own model at all.
- **Armor and capes** are not drawn on player models yet.
- **Bedrock players** see the skin saved in the model's file.
- **Not only on players.** A player model can be spawned like any model, for an NPC or a statue.
  It wears the skin in its file until a plugin gives it another, see
  [API Player Models](API-Player-Models).

## Settings

| Setting | Default | What it does |
|---|---|---|
| `player-animations.show-to-self` | `true` | Show a player the model that stands in for them |
| `player-animations.stop-on-sneak` | `false` | End an animation on a player when they sneak, unless the plugin or command chose |
| `player-animations.stop-on-damage` | `false` | End an animation on a player when they take damage, unless the plugin or command chose |
| `player-animations.fade-ticks` | `3` | How many ticks an animation takes to blend in and out |

## Permissions

| Permission | Allows |
|---|---|
| `hyperion.command.player` | Playing and stopping animations on yourself |
| `hyperion.command.player.others` | Playing and stopping animations on other players |

Both are for operators by default.
