# Configuration

Hyperion has two files in `plugins/Hyperion`: `config.yml` for settings and `messages.yml` for
every chat message. `/hy reload` reads both again.

When an update adds a setting or a message, it is written into your file on the next start. The
lines you changed stay as they are.

## config.yml

### models

| Setting | Default | What it does |
|---|---|---|
| `models.view-distance` | `48` | How far away, in blocks, players see a model |
| `models.interpolation` | `2` | Over how many ticks a client smooths each move of a bone. 0 snaps; higher is smoother but trails behind. |
| `models.spawn-entity` | `pig` | The mob `/hy spawn` puts a model on |
| `models.auto-animations.enabled` | `true` | Models on entities play idle and walk by themselves |
| `models.auto-animations.idle` | `idle` | The animation played while the entity stands still |
| `models.auto-animations.walk` | `walk` | The animation played while the entity moves |
| `models.auto-animations.entities` | `{}` | A choice per entity type that wins over `enabled`, such as `armor_stand: false` |

A plugin that spawns a model can give it its own values.

### behaviors

See [Bone Behaviors](Bone-Behaviors) for what each behavior does.

| Setting | Default | What it does |
|---|---|---|
| `behaviors.prefixes.<behavior>` | see the file | The name starts that give a bone each behavior, ignoring case |
| `behaviors.head.max-yaw` | `50` | How far a head turns left or right, in degrees |
| `behaviors.head.max-pitch` | `40` | How far a head turns up or down, in degrees |
| `behaviors.hitboxes.mode` | `bones` | `bones`, `model` or `none` |
| `behaviors.hitboxes.forward-damage` | `true` | Hits on a model on an entity hurt the entity |
| `behaviors.seats.click-to-ride` | `true` | Right-clicking a model with a free seat sits the player in it |
| `behaviors.seats.steering.enabled` | `false` | The player in the first seat steers the entity |
| `behaviors.seats.steering.mode` | `walking` | `walking` moves like a horse and jumps; `flying` follows where the rider looks |
| `behaviors.seats.steering.speed` | `0.25` | Blocks per tick |
| `behaviors.seats.steering.jump` | `0.5` | How high a jump goes, in blocks per tick upwards |
| `behaviors.seats.steering.entities` | `{}` | A choice per entity type that wins over `enabled`, such as `horse: true` |

### effects

| Setting | Default | What it does |
|---|---|---|
| `effects.view-distance` | `32` | How far away players see an effect that is not on a model |
| `effects.particle-multiplier` | `1.0` | Scales every particle count. 0.5 halves them, 0 sends none. |

### debug and gallery

| Setting | Default | What it does |
|---|---|---|
| `debug.interval` | `4` | Ticks between redraws of `/hy debug` views |
| `debug.labels` | `true` | Names as floating text. Turn off for very busy models. |
| `debug.axis-length` | `0.4` | How long a bone's axes are, in blocks |
| `gallery.preview-seconds` | `120` | How long a private preview stays |

### armor

See [Custom Armor](Custom-Armor).

| Setting | Default | What it does |
|---|---|---|
| `armor.shader` | `false` | Draw chestplates, leggings and boots in 3D with a core shader, from 1.21.2 |
| `armor.render-distance` | `32` | How far from the camera armor is drawn in 3D. Beyond it, flat. |
| `armor.max-layers` | `16` | The most equipment layers a set may use for 3D |

### pack

See [Resource Pack](Resource-Pack).

| Setting | Default | What it does |
|---|---|---|
| `pack.hosting` | `self-host` | `self-host`, `url` or `off` |
| `pack.self-host.port` | `8163` | The port the pack's web server listens on |
| `pack.self-host.address` | empty | The address players download from. Empty uses the address each player joined with. |
| `pack.url` | empty | The link to your uploaded zip, for `hosting: url` |
| `pack.send-on-join` | `true` | Offer the pack to players when they join |
| `pack.required` | `false` | Kick players who decline the pack or fail to download it |
| `pack.resend-on-change` | `true` | Send the new pack to everyone online when a reload changes it |
| `pack.base-item` | `paper` | The item every model part is shown as |
| `pack.custom-model-data-start` | `10000` | The first custom model data number used on 1.21 to 1.21.3 |

### bedrock

See [Bedrock Support](Bedrock-Support).

| Setting | Default | What it does |
|---|---|---|
| `bedrock.enabled` | `true` | Show models to Bedrock players through the Geyser extension |
| `bedrock.link-port` | `8164` | The port the Geyser extension connects to |

## messages.yml

Every chat message is in `messages.yml`, written in
[MiniMessage](https://docs.advntr.dev/minimessage/format.html). The help menu and the command
error texts come from the same file.

- The `palette` section sets the colors. Every message uses them as tags, such as `<primary>`,
  `<muted>` and `<error>`, so changing one color changes it everywhere.
- `<prefix>` puts the prefix in front of a message.

The one text you cannot change is the short card `/hy` shows players without the help permission.
It tells players which plugin draws the models.
