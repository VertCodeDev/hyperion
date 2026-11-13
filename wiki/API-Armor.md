# API Armor

Find armor sets, make their items, ask what a player wears, and build set bonuses on the events.
For making a set, see [Custom Armor](Custom-Armor).

Hyperion draws the armor and tells you what happens to it. What a set does is up to your plugin.

## Sets and pieces

```java
ArmorRegistry armor = Hyperion.armor();

Optional<ArmorSet> frost = armor.find(Key.key("hyperion", "frost"));
Collection<ArmorSet> all = armor.all();
```

Sets from `plugins/Hyperion/armor` are in the `hyperion` namespace, named after their file.

| `ArmorSet` | What it gives |
|---|---|
| `key()` | The set's key |
| `displayName()` | The `name` from its `.yml` |
| `pieces()` | The pieces the set has |
| `piece(ArmorSlot)` | One piece: `HEAD`, `CHEST`, `LEGS` or `FEET` |
| `dyeable()` | Whether players can dye it |
| `data()` | The free values from its `.yml` |

A set only has the pieces it has bones for, so `piece(slot)` is an `Optional`.

| `ArmorPiece` | What it gives |
|---|---|
| `set()` | The set it belongs to |
| `slot()` | Its slot |
| `stats()` | Its armor, toughness and knockback resistance |
| `item()` | A new item of this piece |
| `item(Color)` | The same, dyed |

## Items

```java
ArmorSet set = armor.find(Key.key("hyperion", "frost")).orElseThrow();

// One piece
set.piece(ArmorSlot.HEAD).ifPresent(helmet -> player.getInventory().addItem(helmet.item()));

// The whole set
for (ArmorPiece piece : set.pieces()) {
    player.getInventory().addItem(piece.item());
}

// A dyed piece
ItemStack red = set.piece(ArmorSlot.CHEST).orElseThrow().item(Color.RED);
```

The items are normal `ItemStack`s: put them in loot tables, shops or crafting recipes like any
other item. You can rename, enchant, dye or trim one and it stays the same piece.

Make items through the api rather than copying their components. What an item is made of differs
per Minecraft version, and the api gives the right one for the server.

## Is this item custom armor?

```java
if (armor.isArmor(item)) {
    // ...
}

armor.pieceOf(item).ifPresent(piece -> {
    ArmorSet set = piece.set();
    ArmorSlot slot = piece.slot();
});
```

Both take null, so you can pass a slot's item straight in.

## What a player wears

```java
WornArmor worn = armor.worn(player);

worn.piece(ArmorSlot.HEAD);        // the custom piece on their head, if any
worn.count(set);                   // how many pieces of a set they wear
worn.hasFullSet(set);              // every piece the set has
worn.sets();                       // every set they wear a piece of
```

`hasFullSet` counts the pieces the set actually has: a set with only a helmet is full with the
helmet on.

## Data: values for your plugin

The `data` section of a set's `.yml` is free for plugins. It lets server owners tune your features
per set without touching your config.

```yaml
# plugins/Hyperion/armor/frost.yml
data:
  element: ice
  level: 30
  speed-bonus: 0.2
```

```java
ArmorData data = set.data();

String element = data.getString("element", "none");
int level = data.getInt("level", 0);
double bonus = data.getDouble("speed-bonus", 0);
boolean soulbound = data.getBoolean("soulbound", false);
```

Every getter takes a fallback for when the value is missing.

## Set bonuses

`ArmorSetCompleteEvent` fires when a player now wears every piece of a set, and
`ArmorSetBreakEvent` when they no longer do. Both also fire on join and quit, so a bonus stays
correct over relogs without extra code.

```java
public final class FrostBonus implements Listener {

    private static final Key FROST = Key.key("hyperion", "frost");

    @EventHandler
    public void onComplete(ArmorSetCompleteEvent event) {
        if (event.set().key().equals(FROST)) {
            event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, -1, 0));
        }
    }

    @EventHandler
    public void onBreak(ArmorSetBreakEvent event) {
        if (event.set().key().equals(FROST)) {
            event.getPlayer().removePotionEffect(PotionEffectType.SPEED);
        }
    }
}
```

A bonus that works for every set, driven by the set's data:

```java
@EventHandler
public void onComplete(ArmorSetCompleteEvent event) {
    double bonus = event.set().data().getDouble("speed-bonus", 0);
    if (bonus > 0) {
        // add a speed attribute modifier of "bonus"
    }
}
```

## Bonuses per piece

`ArmorEquipEvent` and `ArmorUnequipEvent` fire for each piece, with why it changed.

```java
@EventHandler
public void onEquip(ArmorEquipEvent event) {
    if (event.piece().slot() == ArmorSlot.FEET) {
        // these boots just went on
    }
}

@EventHandler
public void onUnequip(ArmorUnequipEvent event) {
    if (event.cause() == ArmorChangeCause.BREAK) {
        event.getPlayer().sendMessage("Your " + event.set().displayName() + " piece broke!");
    }
}
```

## Stopping a player from wearing a piece

`ArmorEquipAttemptEvent` fires before a piece goes on, however the player tries: right-click,
placing it in the slot, shift-click, drag, hotbar and offhand swaps, and dispensers. Cancel it to
keep the piece off.

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

`method()` says how they tried: `RIGHT_CLICK`, `INVENTORY_CLICK`, `SHIFT_CLICK`, `DRAG`,
`HOTBAR_SWAP`, and more.

## When the sets reload

`ArmorLoadEvent` fires after the sets load and after every `/hy reload`. It is async. Use it to
rebuild anything you keep per set:

```java
@EventHandler
public void onLoad(ArmorLoadEvent event) {
    for (ArmorSet set : event.sets()) {
        // read set.data() again
    }
}
```

See [API Events](API-Events) for every event and its methods.
