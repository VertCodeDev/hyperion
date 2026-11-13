# Staff Tools

Three tools for building and checking models in game: the gallery, the model menu and the debug
views.

## Gallery

`/hy gallery [filter]` opens a menu with every model whose key contains the filter.

| Click | What it does |
|---|---|
| Left-click | Spawns the model on a mob where you stand, like `/hy spawn` |
| Right-click | Spawns it on its own, like `/hy spawn --static` |
| Shift-left-click | Shows a private preview: a copy three blocks in front of you that only you see |
| Shift-right-click | Opens the model's page: play, loop or stop its animations on your preview, and play its own effects |

You have one preview at a time. It goes after `gallery.preview-seconds` (120 by default), when you
leave, or with the barrier button.

### Effects page

`/hy gallery --effects`, or the star at the bottom of the gallery.

| Click | What it does |
|---|---|
| Left-click | Plays the effect on the nearest model |
| Right-click | Plays it where you stand |
| Shift-click | Plays it with its camera on you |

### Armor page

`/hy gallery --armor`, or the chestplate at the bottom of the gallery.

| Click | What it does |
|---|---|
| Left-click | Gives you the whole set |
| Right-click | Puts the set on. What you wore goes to your inventory. |

### Permissions

The gallery needs `hyperion.command.gallery`. Spawning from it also needs
`hyperion.command.spawn`, playing effects `hyperion.command.effect`, and armor
`hyperion.command.armor`.

## Model menu

Sneak and right-click a model to open its menu. It needs `hyperion.menu`.

- **Animations:** left-click plays once, right-click loops, shift-click stops.
- **Freeze:** stops the mob carrying the model from walking.
- **Idle and walk:** turns the automatic animations on or off.
- **Stop all:** stops every animation.
- **Scale and glow.**
- **Remove.**

A normal right-click still does what it always does, such as sitting on a free seat.

## Debug views

`/hy debug <view>` draws what a model is made of. Only you see it, on every model and effect near
you. Run it again to hide that view.

| View | What it draws |
|---|---|
| `bones` | Each bone's pivot as a white dot, its X, Y and Z axes in red, green and blue, and its name |
| `hitboxes` | Each hitbox outline: green when it can be clicked, gray when not |
| `locators` | Each locator as a pink dot with a line the way it faces, and its name |
| `seats` | Each seat: aqua when empty, orange with the rider's name when taken |
| `effects` | Each running effect's anchor, with its key and tick, and where each part is |

`/hy debug all` turns every view on, `/hy debug off` turns them all off, and `/hy debug` alone
says which are on.

Settings, under `debug` in `config.yml`:

| Setting | Default | What it does |
|---|---|---|
| `interval` | `4` | Ticks between redraws. Higher sends fewer particles. |
| `labels` | `true` | Names as floating text. Turn off for very busy models. |
| `axis-length` | `0.4` | How long a bone's axes are, in blocks |

## Checking a model

| Command | What it shows |
|---|---|
| `/hy list` | Every loaded model |
| `/hy info <model>` | Source, size, bone tree, animations, locators and warnings |
| `/hy validate [model]` | The warnings and errors from the last load |
