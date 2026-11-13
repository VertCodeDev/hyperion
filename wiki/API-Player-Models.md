# API Player Models

Play animations on players in their own skin, give any model a player's skin, and react to an
animation starting and ending. For what a player model is and how to make one, see
[Player Models](Player-Models).

## Animations on players

```java
ModelBlueprint rig = Hyperion.models().find(Key.key("hyperion", "player")).orElseThrow();

Hyperion.playerAnimations().play(player, rig, "wave");
```

The model takes the player's place, wears their skin, and goes when the animation is over.

`play` runs on the thread that owns the player. From a command or another thread, go through the
player's scheduler:

```java
player.getScheduler().run(plugin, task -> Hyperion.playerAnimations().play(player, rig, "wave"), null);
```

### A pose that lasts

An animation that loops or holds stays until you stop it. That is how a player keeps their hands
up for as long as they carry something:

```java
public void pickUpEgg(Player player) {
    Hyperion.playerAnimations().play(player, rig, "hands_up", options -> options.loop(LoopMode.HOLD));
}

public void dropEgg(Player player) {
    Hyperion.playerAnimations().stop(player);
}
```

The player walks and runs as normal meanwhile. The model plays its `walk` and `idle` animations
under yours, and animations add up: `hands_up` moves only the arms, so the legs keep walking.

### Options

```java
Hyperion.playerAnimations().play(player, rig, "sit", options -> options
        .locked(true)
        .loop(LoopMode.HOLD)
        .stopOnSneak(true));
```

| Option | What it does |
|---|---|
| `loop(LoopMode)` | `ONCE` shows the player again when the animation is over. `LOOP` and `HOLD` keep it going until it is stopped. By default the animation's own mode from Blockbench. |
| `locked(boolean)` | Holds the player in place. Off by default: they walk and the model walks with them. |
| `walkAnimations(boolean)` | Plays the model's `idle` and `walk` animations under yours. On by default. |
| `stopOnSneak(boolean)` | Ends it when the player sneaks. `config.yml` sets the default, which is off. |
| `stopOnDamage(boolean)` | Ends it when the player takes damage. `config.yml` sets the default, which is off. |
| `speed(float)` | Plays the animation faster or slower |

`play` returns an empty `Optional` when a plugin cancelled it.

### What is playing

```java
Optional<PlayerAnimation> playing = Hyperion.playerAnimations().playing(player);

playing.ifPresent(current -> {
    current.animation();     // "hands_up"
    current.model();         // the live model
    current.locked();
    current.stop();
});

Hyperion.playerAnimations().stop(player);   // true when one was playing
```

A player has one at a time. A new one replaces the one before.

### More than one animation at once

`model()` is a normal model, so play more on it. A player with their hands up can still wave a
leg, and a bone can hold an item:

```java
Hyperion.playerAnimations().play(player, rig, "hands_up", options -> options.loop(LoopMode.HOLD))
        .ifPresent(current -> {
            current.model().animator().play("limp");
            current.model().bone("right_forearm").ifPresent(hand -> hand.item(new ItemStack(Material.DRAGON_EGG)));
        });
```

### Events

`PlayerAnimationStartEvent` fires before one starts and can be cancelled.
`PlayerAnimationEndEvent` fires when it ends, with whether it played to its end.

```java
@EventHandler
public void onStart(PlayerAnimationStartEvent event) {
    if (inCombat(event.getPlayer())) {
        event.setCancelled(true);
    }
}

@EventHandler
public void onEnd(PlayerAnimationEndEvent event) {
    if (event.finished() && event.animation().equals("bow")) {
        // the bow was not cut short
    }
}
```

## Skins

A model wears a skin on every face that uses the `player_skin` texture. A model on a player
wears that player's skin by itself. Everything else wears the skin saved in its file until you
give it one.

```java
Model statue = rig.spawn(location);

Hyperion.skins().of(Bukkit.createProfile("Notch")).thenAccept(statue::skin);
```

| Call | What it gives |
|---|---|
| `Hyperion.skins().of(player)` | The skin an online player wears now |
| `Hyperion.skins().of(profile)` | The skin of any profile; it is completed first when it has no textures |
| `Hyperion.skins().of(image, slim)` | A skin from your own 64 by 64 image |
| `Hyperion.skins().fallback()` | The plain skin used when one cannot be downloaded |
| `Hyperion.skins().supported()` | Whether this server draws skins: 1.21.4 or newer |

The first two return a `CompletableFuture`. Skins are downloaded off the tick threads and kept
for a while, so asking again is cheap. `Model#skin` is safe from any thread, so `thenAccept` can
set it directly.

```java
model.skin(skin);         // wear it
model.skin(null);         // back to the skin in the model's file
model.skin();             // what it wears now, if it was given one
```

Or choose the skin as the model spawns. The skin arrives on another thread, so go back to the
entity's own before attaching:

```java
Hyperion.skins().of(player).thenAccept(skin ->
        npc.getScheduler().run(plugin, task -> rig.attach(npc, options -> options.skin(skin)), null));
```

`ModelBlueprint#skinned()` tells whether a blueprint is a player model.

### An NPC with a player's skin

```java
public void spawnNpc(Location location, String name) {
    ModelBlueprint rig = Hyperion.models().find(Key.key("hyperion", "player")).orElseThrow();
    Model npc = rig.spawn(location, options -> options.animation("idle"));

    Hyperion.skins().of(Bukkit.createProfile(name)).thenAccept(npc::skin);
}
```
