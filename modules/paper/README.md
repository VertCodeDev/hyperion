# Hyperion plugin

The plugin server owners install: it loads Blockbench models, builds the resource pack and shows
models to players. One jar runs on Paper, Folia and Canvas, from Minecraft 1.21 to 26.3.

Hyperion is a Paper plugin (`paper-plugin.yml`). The jar carries the [api](../api/README.md) and
the [menu modules](../menu/README.md); its libraries are downloaded by the server:

| Library | How it ships | What Hyperion uses it for |
|---|---|---|
| [Incendo Cloud](https://github.com/Incendo/cloud) | Downloaded on first start | The `/hyperion` commands, registered through Paper's Brigadier api |
| [InvUI](https://github.com/NichtStudioCode/InvUI) | Downloaded on first start, the release that fits the server | The menus |
| [PacketEvents](https://github.com/retrooper/packetevents) | Separate plugin, required | Sending model packets, and editing the server's own packets to hide parts of an entity or add seats |

`HyperionLoader` runs before the plugin and has Paper download Cloud and InvUI into its
`libraries` folder, so the first start needs internet access and later starts do not. Paper keeps
them on Hyperion's own classpath, apart from other plugins' copies. InvUI 1.49 covers 1.21 to
1.21.11 and each InvUI 2 release one 26.x line, so the loader picks the release by the server's
version, and Hyperion loads the menu module made for it.

PacketEvents stays a separate plugin so a new Minecraft version only needs a PacketEvents update,
not a new Hyperion release. Hyperion does not start without it.

## Packages

| Package | Holds |
|---|---|
| `dev.vertcode.hyperion` | `HyperionPlugin`: startup and shutdown only; `HyperionLoader`: the libraries the server downloads |
| `blueprint` | `BlueprintRegistry` (loading and reloading), `Blueprint` and its bones and locators, load reports |
| `blueprint.bbmodel` | `BbmodelFormat` and `BbmodelReader`: `.bbmodel` JSON into plain records |
| `blueprint.data` | The plain records a file is read into: cubes, groups, textures, locators, animations |
| `blueprint.metadata` | `JsonMetadata`: the extra fields Blockbench plugins store, read only |
| `command` | One class per command, `HyperionCommands` to register them, `HyperionPermissions` |
| `config` | `HyperionConfig`, `ModelSettings`, `BehaviorSettings` and `PackSettings` from `config.yml`; `HyperionMessages`, one constant per line of `messages.yml`, and the `MessageBook` they render from |
| `pack` | `PackBuilder` turns blueprints into the resource pack; `PackManager` builds, hosts and sends it |
| `model` | `ModelManager` (spawning, the tracker, following reloads), `HyperionModel` and its bones, locators and viewers, and the api's model registry |
| `render` | The bone and part matrices, the item display's entity data, the items parts show, and the packets |
| `behavior` | `HyperionBehaviorRegistry`, the behavior types plugins register, and `BuiltinTypes`: the head, hitbox, seat, name tag, hand and ghost behaviors Hyperion ships |
| `listener` | `PackListener` for the pack, `ModelListener` for tracking and removal, `SeatListener` for seats; the PacketEvents listeners `BaseEntityListener`, which hides an entity's parts, `HitboxListener`, which turns hits on hitboxes into events, and `RiderInputListener`, which notes the keys riders hold |
| `animation` | `AnimationBaker` bakes keyframes into per-tick samples on load; `ModelAnimator` plays them on a live model |
| `animation.molang` | The part of Molang Blockbench keyframes use |
| `ui` | The model menu, and `Menus`, which loads the InvUI module that fits the server |
| `bedrock` | Telling Bedrock players apart, the Bedrock pack, and the link the Geyser extension follows |

## Models

Put `.bbmodel` files in `plugins/Hyperion/models/`, in subfolders if you like. The path becomes
the model's key: `models/bosses/dragon.bbmodel` loads as `hyperion:bosses/dragon`. Capitals and
spaces are turned into lowercase and `_` with a warning; any other character a key cannot hold
stops that file from loading.

Models load on the async scheduler when the plugin starts and on `/hy reload`. A file with
problems never stops the others: what Hyperion cannot use (meshes, missing textures, unknown
keyframe channels) is a warning in `/hy validate`, and only a file that cannot be read at all
fails. A failed reload keeps the model that was loaded before.

Both group layouts Blockbench has saved are read: groups inside the outliner (before 5.0) and
the top-level `groups` list (5.0 and later). Fields Blockbench itself does not use are kept as
metadata on the project, groups and locators, which is how a Blockbench plugin's settings reach
the server.

## Commands

The root is `/hyperion`, with the aliases `/hyp` and `/hy`.

| Command | Permission | What it does |
|---|---|---|
| `/hy` | none | Staff get the help menu; everyone else sees which plugin draws the models |
| `/hy help [query]` | `hyperion.command.help` | The paged help menu, filtered by a search |
| `/hy list [filter] [--page <page>]` | `hyperion.command.list` | Every loaded model with its counts, ten to a page with buttons between pages, each a click away from its info |
| `/hy info <model>` | `hyperion.command.info` | Source, size, bone tree, animations, locators and warnings |
| `/hy reload [model]` | `hyperion.command.reload` | Load every model again, or one, and show what changed |
| `/hy validate [model]` | `hyperion.command.validate` | The warnings and errors from the last load |
| `/hy spawn <model> [scale] [--entity <mob>] [--static]` | `hyperion.command.spawn` | Spawn a model on a mob where you stand, so it walks around; `--static` places it on its own |
| `/hy attach <model> [scale]` | `hyperion.command.spawn` | Put a model on the entity you look at |
| `/hy near [radius]` | `hyperion.command.remove` | The models around you, nearest first, each a click away from removal |
| `/hy remove [radius]` | `hyperion.command.remove` | Remove the nearest model, or every model within the radius |
| `/hy animate play <animation> [speed]` | `hyperion.command.animate` | Play an animation on the nearest model |
| `/hy animate stop [animation]` | `hyperion.command.animate` | Stop one animation, or all of them, on the nearest model |
| `/hy pack build` | `hyperion.command.pack.build` | Build the resource pack again |
| `/hy pack send [players]` | `hyperion.command.pack.send` | Offer the pack again, to some players or everyone |
| `/hy pack status` | `hyperion.command.pack.status` | Hosting, link, size and hash, and how players are doing with it |
| `/hy pack export` | `hyperion.command.pack.export` | Write the pack to `plugins/Hyperion/hyperion-pack.zip` |

A model argument may leave out `hyperion:`, so `/hy info bosses/dragon` works. `/hy reload` also
reads `config.yml` and `messages.yml` again and rebuilds the pack.

`/hy spawn` puts the model on a new mob, `models.spawn-entity` in `config.yml` (a pig unless you
change it), so it walks around and plays its idle and walk animations. The mob is hidden, silent
and not saved: it goes when the model is removed or the server restarts.

Every optional value suggests itself while you type: scales, speeds, radii and mob names.

Sneaking and right-clicking a model on a mob opens its menu (`hyperion.menu`): its animations to
play once (left-click), loop (right-click) or stop (shift-click), and controls to freeze the mob,
switch its idle and walk animations, stop every animation, scale it, make it glow and remove it.

`hyperion.admin` grants every Hyperion permission. Every node defaults to operators.

## Live models

A model is only packets: one item display for each part of each bone, sent to the players who
can see it. The server never ticks, saves or collides with it, and models are not kept over a
restart; a plugin puts them back, on `EntitiesLoadEvent` for models on entities.

Every tick a model runs a frame on the thread that owns it. The frame works out the scale, the
rotation and every bone, and sends each viewer only what changed: a bone that did not move costs
nothing, and a model nobody sees works out nothing but its behaviors. Clients smooth each move
over `models.interpolation` ticks.

- **On an entity**, the displays ride the entity, so the client moves them with it and no
  movement is sent. They sit on the entity's seat, at the top of its hitbox, so the model is
  lowered by the entity's height. The entity's own packets are changed on their way out to hide
  its body, equipment, name, fire and outline from the players who see the model; the entity
  itself is never touched, so removing Hyperion leaves it as it was. Viewers follow the server's
  own tracking of the entity, within `models.view-distance`.
- **On its own**, a model stands at a location and moves with `Model#teleport`. Every half second
  it looks for players within `models.view-distance`.

A model leaves when it is removed, its entity dies or unloads, its blueprint is unregistered, or
the plugin disables. After a reload, live models move onto the new blueprint once the new pack is
built, keeping what was set on bones that are still there.

## Behaviors

A bone gets a behavior when its name starts with one of the behavior's prefixes in the
`behaviors.prefixes` section of `config.yml`, ignoring case, or when it carries the behavior's key
as a tag. Add the names another engine uses to load its models as they are, such as `h_` for
`head`. Prefixes are read as a model spawns, so a change reaches models spawned after
`/hy reload`.

| Behavior | Default prefix | What it does |
|---|---|---|
| Head | `head` | Turns with where a living entity looks, within `behaviors.head`, or towards a point a plugin sets |
| Hitbox | `hitbox` | A box players hit and click, sized by the bone's own cubes, which are not drawn |
| Seat | `seat` | A spot a player sits on, moving with the bone |
| Name tag | `nametag` | The entity's custom name, or text a plugin sets, following the bone |
| Hands | `main_hand`, `off_hand` | What the entity holds, shown on the bone; its own cubes while the hand is empty |
| Ghost | `ghost` | Never drawn, kept for its children, locators and logic |

**Hitboxes** are interactions only the model's viewers know about, moved to their bone after
every frame. `behaviors.hitboxes.mode` picks a hitbox per hitbox bone (`bones`), or one box around
the whole model (`model`); a model without hitbox bones gets the box either way. A hit or click
fires `ModelInteractEvent` on the model's thread. Unless it is cancelled, the server is handed the
same packet aimed at the entity carrying the model, so damage, reach checks and clicks work as if
the player touched the entity. Riders never get the model's hitboxes, which would catch their
clicks from inside.

**Seats** exist only while someone sits: the rider sits on an empty item display, which rides the
entity carrying the model so the server carries the rider along. The model's viewers are told the
display rides nothing and see it moved to the seat's bone after every frame, so the rider sits
where the model shows the seat. Right-clicking a model with a free seat sits the player in it
(`behaviors.seats.click-to-ride`); sneaking gets them off, unless a plugin locked the seat. With
`behaviors.seats.steering` on, globally or per entity type, the rider of the first seat steers:
the entity walks and jumps, or flies, with the rider's keys, and its own AI waits until they get
off.

Sneaking and right-clicking a model on its own opens its menu, as on an entity.

Head, name tag and steering are Java only for now: Bedrock players see the model's animations and
ride its entity, but not the head turning or the name tag on the bone.

## Animations

Every animation is baked when its model loads, on the async scheduler: each bone it moves gets
one sample per tick of its offset, angles and stretch, worked out with Blockbench's own math
(linear, smooth, step and bezier keyframes, angles added before they turn the bone). Every model
playing it shares the samples, so playing only reads and blends numbers.

Keyframes may hold Molang: arithmetic, comparisons, `math.*`, `query.anim_time` and
`query.life_time`, and variables. Anything else is a warning in `/hy validate` and reads as 0.

Several animations play at once and add up per bone, as in Blockbench; one marked to override
replaces those started before it. Each can play faster or slower, loop, play once or hold its
last frame, and fade in and out. An animated bone is sent every tick; bones no animation moves
still send nothing.

Models on entities play `idle` while the entity stands and `walk` while it moves, when they have
those animations. `models.auto-animations` in `config.yml` sets the names, turns it off for
every model, or per entity type (`armor_stand: false`); a plugin can turn it off per model.

A player's view of a model (`Model#view`) has its own animator and look overrides: what it plays
and sets only that player sees, on top of what everyone sees.

## Bedrock

Bedrock players see models as native Bedrock entities through the
[Geyser extension](../geyser/README.md), wherever Geyser runs. Hyperion builds a Bedrock pack next
to the Java one, from the same models, and serves it with the model list on its Bedrock link
(`bedrock.link-port`, default 8164), guarded by the token in `plugins/Hyperion/bedrock-token.txt`.

Bedrock players get no display packets. The entity carrying a model is sent to them with the model
in its UUID, and Geyser shows the model in its place; a model on its own gets a stand-in entity
only they are sent. Which animations play goes to the extension as a stream of events, so they run
on the player's device.

A player counts as Bedrock when the extension reports them, when Floodgate says so, or when their
UUID starts with 64 zero bits, as Floodgate's do. Without the extension, Bedrock players see the
plain mob a model rides.

## Resource pack

Every loaded model is built into one resource pack, for the version the server runs, after
startup and after every `/hy reload`. The pack format comes from the server jar itself, so a new
Minecraft version needs no update for it. The same models give the same zip and hash, so players
who already have the pack do not download it again.

Each bone becomes one item model centered on its pivot. Item models only hold cubes within -16
to 32 pixels, turned on one axis by 22.5 or 45 degrees, so a bone that is too big is shrunk and
scaled back up by its display, and a cube turned any other way becomes a part of its own that the
display turns. Those are 1.21's limits, so a model renders the same on every version. From 1.21.4
each part is an item definition; on 1.21 to 1.21.3 it is a custom model data number on the base
item.

`config.yml` decides how players get the pack:

| `pack.hosting` | What happens |
|---|---|
| `self-host` (default) | A small web server built into Hyperion serves it on `pack.self-host.port`. Players download from the address they joined with, unless `pack.self-host.address` says otherwise. |
| `url` | You upload the zip from `/hy pack export` and put its link in `pack.url`. Upload it again after every change. |
| `off` | Nothing is sent; merge the exported zip into your own pack. |

`pack.required` kicks players who decline or fail the download, `pack.send-on-join` and
`pack.resend-on-change` decide when it is offered, and `pack.base-item` (default `paper`) is the
item parts are shown as. On 1.21 to 1.21.3 only a dyeable base item such as
`leather_horse_armor` can be tinted.

## Messages

Every chat message lives in `plugins/Hyperion/messages.yml`, written in
[MiniMessage](https://docs.advntr.dev/minimessage/format.html). Cloud's own error texts and the
help menu come from the same file. The `palette` section sets the colors, which every message
uses as tags (`<primary>`, `<muted>`, `<error>`...), and `<prefix>` puts the prefix in front.
Keys a newer version adds are written into the server's file on the next start; lines the server
changed are left alone. `/hy reload` reads the file again.

In code, each line is a constant whose name spells its key (`RELOAD__MODEL_DONE` is
`reload.model-done`), and values are passed by name:

```java
HyperionMessages.RELOAD__DONE.send(sender, value("loaded", 40), value("time", 312));
```

The one text not in the file is the about card `/hy` shows players without the help permission:
it is how players learn which plugin draws the models.

## Versions

One jar supports every version from 1.21 to 26.3. To keep it that way:

- The code is compiled for Java 21, the oldest Java those servers run on.
- It is compiled against the Paper 1.21 api, so a method only newer servers have cannot be used
  by accident.
- `./gradlew build` compiles it a second time against the 26.3 api, so a method newer servers
  removed fails the build too.
- A difference between versions is a `Feature` in the api, named once with the version it arrived
  in. Code asks `Feature.ITEM_MODEL_DEFINITIONS.available()` rather than comparing version numbers,
  and never names a Paper constant some versions lack: the scale attribute, for one, is looked up
  by the key `Feature.PLAIN_ATTRIBUTE_KEYS` says the server uses.
- A Paper method only newer servers have will go in a small module per version range, compiled
  against that version's api and loaded only on servers that have it.

## Threading

Cloud runs a command on the thread that sent it: a player's command on that player's region, the
console's on the global region. A command that changes something owned elsewhere hops to its
owner first.

A model is owned by its entity's scheduler, or by the region of its location. Its frames run
there, and `spawn`, `attach`, `teleport` and `remove` must be called there. Every getter and
appearance setter is safe from any thread and takes effect on the next frame. Packets are sent
with PacketEvents, which is safe from any thread; the PacketEvents listeners run on Netty threads
and read only concurrent state. A hit on a hitbox is taken off the Netty thread and handled on
the thread that owns the model.

## Building

```bash
./gradlew build
```

The jar is `build/libs/hyperion-paper-v<version>.jar`. NullAway checks the JSpecify annotations on every
compile, so a null mistake fails the build.

The tests read small hand-written models from `src/test/resources/models`. To check that a folder
of your own models loads, point `HYPERION_MODELS` at it:

```bash
HYPERION_MODELS=~/my-models ./gradlew :paper:test --tests '*RealModelsTest'
```
