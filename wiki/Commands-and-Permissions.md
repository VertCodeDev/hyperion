# Commands and Permissions

The root command is `/hyperion`, with the aliases `/hyp` and `/hy`.

Every permission defaults to operators. `hyperion.admin` grants all of them.

A model or effect argument may leave out `hyperion:`, so `/hy info bosses/dragon` works. Optional
values suggest themselves while you type: scales, speeds, radii and mob names.

## General

| Command | Permission | What it does |
|---|---|---|
| `/hy` | none | Staff get the help menu. Everyone else sees which plugin draws the models. |
| `/hy help [query]` | `hyperion.command.help` | The paged help menu, filtered by a search |
| `/hy reload [model]` | `hyperion.command.reload` | Loads every model again, or one. Also reads the config, messages and effects again and rebuilds the pack. |
| `/hy validate [model]` | `hyperion.command.validate` | The warnings and errors from the last load |

## Models

| Command | Permission | What it does |
|---|---|---|
| `/hy list [filter] [--page <page>]` | `hyperion.command.list` | Every loaded model, ten to a page, each a click away from its info |
| `/hy info <model>` | `hyperion.command.info` | Source, size, bone tree, animations, locators and warnings |
| `/hy spawn <model> [scale] [--entity <mob>] [--static]` | `hyperion.command.spawn` | Spawns a model on a mob where you stand. `--static` places it on its own. |
| `/hy attach <model> [scale]` | `hyperion.command.spawn` | Puts a model on the entity you look at |
| `/hy near [radius]` | `hyperion.command.remove` | The models around you, nearest first, each a click away from removal |
| `/hy remove [radius]` | `hyperion.command.remove` | Removes the nearest model, or every model within the radius |

## Animations and effects

| Command | Permission | What it does |
|---|---|---|
| `/hy animate play <animation> [speed]` | `hyperion.command.animate` | Plays an animation on the nearest model |
| `/hy animate stop [animation]` | `hyperion.command.animate` | Stops one animation, or all of them, on the nearest model |
| `/hy effect play <effect> [--here] [--camera]` | `hyperion.command.effect` | Plays an effect on the nearest model, or where you stand. `--camera` lets its camera take your view. |
| `/hy effect stop` | `hyperion.command.effect` | Stops every effect within 16 blocks |
| `/hy effect list [--page <page>]` | `hyperion.command.effect` | Every loaded effect, each a click away from playing it |

## Armor

| Command | Permission | What it does |
|---|---|---|
| `/hy armor list` | `hyperion.command.armor` | Every armor set and its pieces |
| `/hy armor give <player> <set> [piece]` | `hyperion.command.armor` | Gives a player a whole set, or one piece of it |

## Staff tools

| Command | Permission | What it does |
|---|---|---|
| `/hy gallery [filter] [--effects] [--armor]` | `hyperion.command.gallery` | Browse, preview and spawn every model, effect or armor set in a menu |
| `/hy debug [view]` | `hyperion.command.debug` | Shows or hides `bones`, `hitboxes`, `locators`, `seats` or `effects` near you, `all` or `off` |
| Sneak and right-click a model | `hyperion.menu` | Opens the model's menu |

The gallery uses your other permissions too: spawning from it needs `hyperion.command.spawn`,
playing effects `hyperion.command.effect`, and giving armor `hyperion.command.armor`.

## Resource pack

| Command | Permission | What it does |
|---|---|---|
| `/hy pack status` | `hyperion.command.pack.status` | Hosting, link, size and hash, and how players are doing with the pack |
| `/hy pack build` | `hyperion.command.pack.build` | Builds the resource pack again |
| `/hy pack send [players]` | `hyperion.command.pack.send` | Offers the pack again, to some players or everyone |
| `/hy pack export` | `hyperion.command.pack.export` | Writes the pack to `plugins/Hyperion/hyperion-pack.zip` |

## All permissions

| Permission | Grants |
|---|---|
| `hyperion.admin` | Everything below |
| `hyperion.command.help` | `/hy help` |
| `hyperion.command.list` | `/hy list` |
| `hyperion.command.info` | `/hy info` |
| `hyperion.command.reload` | `/hy reload` |
| `hyperion.command.validate` | `/hy validate` |
| `hyperion.command.spawn` | `/hy spawn`, `/hy attach` |
| `hyperion.command.remove` | `/hy near`, `/hy remove` |
| `hyperion.command.animate` | `/hy animate` |
| `hyperion.command.effect` | `/hy effect` |
| `hyperion.command.debug` | `/hy debug` |
| `hyperion.command.gallery` | `/hy gallery` |
| `hyperion.command.armor` | `/hy armor` |
| `hyperion.command.pack.build` | `/hy pack build` |
| `hyperion.command.pack.send` | `/hy pack send` |
| `hyperion.command.pack.status` | `/hy pack status` |
| `hyperion.command.pack.export` | `/hy pack export` |
| `hyperion.menu` | Opening a model's menu |
