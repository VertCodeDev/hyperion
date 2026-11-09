# Hyperion menu

What a Hyperion menu shows, without drawing it: a title, three rows of buttons that page when
there are more, and a row of controls between the page arrows. The plugin builds its menus from
these types once; the menu modules draw them.

| Module | Draws with | Servers |
|---|---|---|
| `menu-invui1` | InvUI 1.49 | 1.21 to 1.21.11 |
| `menu-invui2` | InvUI 2 | 26.x, Java 25 |

InvUI is not inside the plugin jar. The plugin's loader has the server download the InvUI release
for its version on start, and the plugin loads the module that fits. A new menu library only
needs a new `MenuRenderer`.
