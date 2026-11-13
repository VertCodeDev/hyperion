# API Behaviors

A behavior is what a bone does besides being drawn: turning like a head, being a seat, being a
hitbox. Hyperion ships seven, and your plugin can add its own. For how bones get a behavior from
their name, see [Bone Behaviors](Bone-Behaviors).

## Reading a model's behaviors

```java
List<SeatBehavior> seats = model.seats();
List<HitboxBehavior> hitboxes = model.hitboxes();
List<HeadBehavior> heads = model.behaviors(HeadBehavior.class);

Optional<SeatBehavior> saddle = model.bone("seat_saddle").flatMap(bone -> bone.behavior(SeatBehavior.class));
```

## Heads

```java
for (HeadBehavior head : model.behaviors(HeadBehavior.class)) {
    head.maxYaw(70f).maxPitch(30f);
    head.target(player.getLocation());   // look at a point
    head.target(null);                   // follow the entity's look again
    head.enabled(false);                 // stop turning
}
```

## Seats

```java
SeatBehavior seat = model.seats().getFirst();

seat.mount(player);                 // false when the seat is taken
seat.passenger();                   // who sits there
seat.locked(true);                  // sneaking no longer gets them off
seat.dismount();
```

Call `mount` and `dismount` on the thread that owns the model.

### Steering

A `SeatController` lets the rider drive the entity under the model:

```java
seat.controller(SeatController.walking(0.25f, 0.5f));   // speed, jump
seat.controller(SeatController.flying(0.4f));
seat.controller(null);                                   // no steering
```

Write your own for anything else. It gets the keys the rider holds and returns the velocity for
this tick:

```java
public final class HoverController implements SeatController {

    @Override
    public Vector steer(Player rider, Entity vehicle, RiderInput input) {
        Vector ahead = rider.getLocation().getDirection().setY(0).normalize();
        double forward = input.forward() ? 0.3 : 0;
        double up = input.jump() ? 0.2 : input.sneak() ? -0.2 : 0;

        return ahead.multiply(forward).setY(up);
    }

    @Override
    public boolean flies() {
        return true;
    }
}
```

`RiderInput` has `forward`, `backward`, `left`, `right`, `jump`, `sneak` and `sprint`.

## Hitboxes

```java
for (HitboxBehavior hitbox : model.hitboxes()) {
    hitbox.hitbox(Hitbox.of(1.5f, 2f));     // width, height
    hitbox.interactable(false);             // clicks go through it
    hitbox.forwardsDamage(false);           // hits no longer hurt the entity
}
```

Hits and clicks fire `ModelInteractEvent`, with the bone that was hit. A boss with a weak spot is
a few lines:

```java
@EventHandler
public void onHit(ModelInteractEvent event) {
    if (event.type() != InteractionType.ATTACK) {
        return;
    }

    boolean weakSpot = event.bone().map(bone -> bone.name().equals("hitbox_core")).orElse(false);
    if (!weakSpot) {
        event.setCancelled(true);   // only the core takes damage
    }
}
```

## Name tags

```java
model.behaviors(NametagBehavior.class).forEach(tag -> tag
        .text(Component.text("Frost Dragon", NamedTextColor.AQUA))
        .seeThrough(true));
```

`text(null)` shows the entity's custom name again.

## Hands

`HandBehavior` shows what the entity holds. To show something else on a bone, set an item on the
bone itself, see [API Bones](API-Bones).

## Writing your own behavior

A behavior is a class that implements `BoneBehavior`. One is created for each matching bone of
each model. Every hook is optional:

| Hook | When |
|---|---|
| `onSpawn()` | The model spawned |
| `onTick()` | Every frame of the model, on the thread that owns it |
| `onShow(viewer)`, `onHide(viewer)` | A player starts or stops seeing the model |
| `onRemove(reason)` | The model is going |

```java
public final class SpinBehavior implements BoneBehavior {

    private final Bone bone;
    private float angle;

    public SpinBehavior(Bone bone) {
        this.bone = bone;
    }

    @Override
    public void onTick() {
        this.angle += 0.1f;
        this.bone.transform(BoneTransform.ofRotation(new Quaternionf().rotateY(this.angle)));
    }
}
```

Register it with a key in your namespace and a matcher that says which bones get it:

```java
Hyperion.behaviors().register(this, BehaviorType.of(
        Key.key("myplugin", "spin"),
        SpinBehavior.class,
        BehaviorMatcher.namePrefix("spin"),
        SpinBehavior::new
));
```

Now every bone whose name starts with `spin` turns by itself, on every model spawned after this.
Register in `onEnable`, before your models spawn. The type goes when your plugin disables.

### Matchers

| Matcher | Matches |
|---|---|
| `BehaviorMatcher.namePrefix("spin", "rotor")` | Bones whose name starts with one of these, ignoring case |
| `BehaviorMatcher.nameEquals("core")` | One exact name |
| `BehaviorMatcher.tagged("myplugin:spin")` | Bones carrying this tag in their `hyperion_behaviors` field in the file |
| `first.or(second)`, `first.and(second)` | Combinations |

A matcher is also a lambda over the loaded bone, so you can match on metadata:

```java
BehaviorMatcher matcher = bone -> bone.metadata().bool("spins").orElse(false);
```

`onTick` runs every tick for every bone that has the behavior. Keep it cheap, and never block in
it.
