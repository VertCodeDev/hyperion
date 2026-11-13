# Models

A model is a Blockbench project (`.bbmodel`) that Hyperion shows in game with display entities.

## Adding a model

Put the `.bbmodel` file in `plugins/Hyperion/models/`, in subfolders if you like, then run
`/hy reload`.

The path becomes the model's key:

| File | Key |
|---|---|
| `models/dragon.bbmodel` | `hyperion:dragon` |
| `models/bosses/dragon.bbmodel` | `hyperion:bosses/dragon` |

Capitals and spaces in a file name are turned into lowercase and `_`, with a warning. Any other
character a key cannot hold stops that file from loading. In commands you may leave out
`hyperion:`, so `/hy info bosses/dragon` works.

## What Hyperion reads

- Cubes, with any rotation and size.
- Groups, which become the model's bones, with their pivots and rotations.
- Several textures per model.
- Animations, see [Animations](Animations).
- Locators, the named points effects attach to, see [Effects](Effects).
- Files saved by Blockbench before 5.0 and by 5.0 and later.

Meshes are not supported. What Hyperion cannot use becomes a warning in `/hy validate`, and the
rest of the model still loads. Only a file that cannot be read at all fails, and a failed reload
keeps the model that was loaded before.

## Bone names matter

A bone whose name starts with a known word gets a behavior: `head` turns with the entity's look,
`hitbox` can be clicked, `seat` can be sat on, and so on. See [Bone Behaviors](Bone-Behaviors).

## Putting a model in the world

| Command | What it does |
|---|---|
| `/hy spawn <model> [scale]` | Spawns the model on a hidden mob where you stand, so it walks around |
| `/hy spawn <model> --entity zombie` | The same, on a mob you pick |
| `/hy spawn <model> --static` | Places the model on its own, standing still |
| `/hy attach <model> [scale]` | Puts the model on the entity you look at |
| `/hy near [radius]` | Lists the models around you, each a click away from removal |
| `/hy remove [radius]` | Removes the nearest model, or every model within the radius |

The mob `/hy spawn` uses is `models.spawn-entity` in `config.yml`, a pig by default. It is hidden,
silent and not saved.

You can also browse and spawn every model from a menu with `/hy gallery`, see
[Staff Tools](Staff-Tools).

## Models are not saved

A model is only packets. The server never ticks, saves or collides with it, so models are gone
after a restart. A plugin that wants them to stay puts them back itself, see
[Developer API](Developer-API).

A model leaves when it is removed, when its entity dies or unloads, or when its file is removed
and the models reload.

## On an entity or on its own

- **On an entity**, the model follows the entity: its movement, and its head and body rotation.
  The entity's own body, equipment, name, fire and outline are hidden from players who see the
  model. The entity itself is never changed, so removing Hyperion leaves it as it was.
- **On its own**, the model stands at a location until a plugin moves it.

Players see a model within `models.view-distance` blocks, 48 by default.

## Reloading

`/hy reload` loads every model again, and `/hy reload <model>` loads one. It also reads
`config.yml`, `messages.yml` and the effects again, and rebuilds the resource pack. Live models
move onto the new version once the new pack is built.

## How models become a resource pack

Each bone becomes one item model. Item models have limits, and Hyperion works around them for you:

- A bone that is too big is shrunk in the pack and scaled back up in game.
- A cube turned by an angle item models cannot hold becomes a part of its own.

So a model looks the same on every version. See [Resource Pack](Resource-Pack) for how players get
the pack.
