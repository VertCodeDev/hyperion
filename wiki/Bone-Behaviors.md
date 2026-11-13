# Bone Behaviors

A bone gets a behavior from its name. Name a group `head` in Blockbench and it turns with where
the entity looks. No extra setup is needed.

## The behaviors

| Behavior | Name starts with | What it does |
|---|---|---|
| Head | `head` | Turns with where a living entity looks |
| Hitbox | `hitbox` | A box players hit and click, sized by the bone's own cubes. The cubes are not drawn. |
| Seat | `seat` | A spot a player sits on, moving with the bone |
| Name tag | `nametag` | Shows the entity's custom name, following the bone |
| Main hand | `main_hand` | Shows what the entity holds in its main hand |
| Off hand | `off_hand` | Shows what the entity holds in its off hand |
| Ghost | `ghost` | Never drawn. For bones that only hold other bones, locators or logic. |

Names are matched by how they start, ignoring case: `head`, `Head_top` and `head2` are all heads.

## Using another engine's names

If your models come from another engine, add its prefixes instead of renaming every bone:

```yaml
behaviors:
  prefixes:
    head: [head, h_]
    seat: [seat, p_]
```

Prefixes are read when a model spawns, so a change reaches models spawned after `/hy reload`.

## Heads

A head bone turns left and right, and up and down, within limits:

```yaml
behaviors:
  head:
    max-yaw: 50
    max-pitch: 40
```

## Hitboxes

Hitboxes decide where players can hit and click a model.

```yaml
behaviors:
  hitboxes:
    # bones  A hitbox per hitbox bone. A model without hitbox bones gets one box around all of it.
    # model  One box around the whole model.
    # none   No hitbox: players click straight through the model.
    mode: bones
    # Hits on a model on an entity hurt the entity, as if the player hit it.
    forward-damage: true
```

To make a hitbox, add a group named `hitbox` (or `hitbox_body`, `hitbox_tail`, and so on) and put
a cube in it with the size you want. The cube sets the size of the box and is not drawn.

A hit or click on a model on an entity is passed on to that entity, so damage, reach checks and
right-click actions work as if the player touched the entity itself.

## Seats

A seat is a spot a player can ride.

- Right-clicking a model with a free seat sits the player in it.
- Sneaking gets them off.

```yaml
behaviors:
  seats:
    click-to-ride: true
    # The player in the first seat steers the entity carrying the model.
    steering:
      enabled: false
      # walking moves like a horse and jumps; flying follows where the rider looks.
      mode: walking
      speed: 0.25
      jump: 0.5
      # A choice per entity type that wins over "enabled".
      entities:
        horse: true
```

With steering on, the rider in the first seat controls the entity with their movement keys, and
the entity's own AI waits until they get off.

## Name tags and hands

- A `nametag` bone shows the entity's custom name where the bone is, instead of above the
  entity's head.
- A `main_hand` or `off_hand` bone shows the item the entity holds. While the hand is empty, the
  bone's own cubes show.

## Seeing what a model has

`/hy debug hitboxes` and `/hy debug seats` draw them for you alone, and `/hy info <model>` lists a
model's bones. See [Staff Tools](Staff-Tools).

## Bedrock

Head turning, name tags on bones and steering reach Java players only for now. Bedrock players see
the model's animations and can ride its entity. See [Bedrock Support](Bedrock-Support).

## For developers

Plugins can read and control these behaviors, and register their own behavior types. See
[Developer API](Developer-API).
