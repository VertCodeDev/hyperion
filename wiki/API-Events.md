# API Events

Every event is a normal Bukkit event in `dev.vertcode.hyperion.api.event`. Listen with
`@EventHandler` as usual.

Events fire on the thread that owns the model or player, so you may touch them directly. The two
load events are async.

## Models

| Event | When | Useful methods |
|---|---|---|
| `ModelSpawnEvent` | A model was spawned or attached | `model()` |
| `ModelRemoveEvent` | A model was removed | `model()`, `reason()` |
| `ModelViewerEvent` | A player starts or stops seeing a model | `model()`, `player()`, `shown()` |
| `ModelInteractEvent` | A player hit or clicked a model. Cancellable. | `model()`, `player()`, `bone()`, `type()`, `hand()` |
| `BlueprintLoadEvent` | A model file loaded or reloaded. Async. | `blueprint()`, `reload()` |

```java
@EventHandler
public void onClick(ModelInteractEvent event) {
    if (event.type() == InteractionType.RIGHT_CLICK) {
        event.player().sendMessage("You clicked " + event.model().blueprint().key());
        event.setCancelled(true);   // the entity under the model does not get the click
    }
}
```

`bone()` is the hitbox bone that was hit, when the model has hitbox bones.

## Animations

| Event | When | Useful methods |
|---|---|---|
| `AnimationStartEvent` | An animation started | `model()`, `animation()`, `viewer()` |
| `AnimationEndEvent` | An animation ended or was stopped | `model()`, `animation()`, `finished()`, `viewer()` |
| `AnimationKeyframeEvent` | An animation passed a timeline keyframe from Blockbench | `model()`, `animation()`, `script()`, `viewer()` |

`viewer()` is the player when the animation plays in one player's view, and null when it plays for
everyone.

Timeline keyframes are how an animation tells your plugin "now": put a timeline keyframe in
Blockbench at the moment a sword lands, and deal the damage when the event fires.

```java
@EventHandler
public void onKeyframe(AnimationKeyframeEvent event) {
    if (event.script().equals("slam")) {
        // hurt players near event.model().location()
    }
}
```

## Effects

| Event | When | Useful methods |
|---|---|---|
| `EffectStartEvent` | An effect started playing | `effect()` |
| `EffectEndEvent` | An effect ended or was stopped | `effect()`, `finished()` |
| `EffectKeyframeEvent` | An effect passed a keyframe of an `event` part | `effect()`, `name()`, `location()` |

## Armor

Armor events fire however the armor changed, and also when a player joins or leaves wearing it.
All of them except `ArmorLoadEvent` have `getPlayer()` and `set()`.

| Event | When | Useful methods |
|---|---|---|
| `ArmorEquipAttemptEvent` | A player tries to put a piece on. Cancellable. | `piece()`, `item()`, `method()` |
| `ArmorEquipEvent` | A piece went on | `piece()`, `item()`, `cause()` |
| `ArmorUnequipEvent` | A piece came off | `piece()`, `item()`, `cause()` |
| `ArmorSetCompleteEvent` | The player now wears every piece of a set | `cause()` |
| `ArmorSetBreakEvent` | The player no longer wears every piece of a set | `cause()` |
| `ArmorLoadEvent` | The armor sets loaded or reloaded. Async. | `sets()` |

The equip attempt covers every way to put armor on: right-click, placing it in the slot,
shift-click, drag, hotbar and offhand swaps, and dispensers. Cancel it to keep the piece off:

```java
@EventHandler
public void onAttempt(ArmorEquipAttemptEvent event) {
    int needed = event.set().data().getInt("level", 0);
    if (event.getPlayer().getLevel() < needed) {
        event.setCancelled(true);
        event.getPlayer().sendMessage("You need level " + needed + " to wear this.");
    }
}
```

`cause()` says why the armor changed: `PLAYER`, `DISPENSER`, `BREAK`, `DEATH`, `JOIN`, `QUIT` or
`OTHER`. Use it to tell a player taking armor off from the armor breaking.

Because the events also fire on join and quit, a set bonus given in `ArmorSetCompleteEvent` and
taken away in `ArmorSetBreakEvent` stays correct over relogs without extra code.
