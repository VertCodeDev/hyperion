# Resource Pack

Hyperion builds one resource pack from every loaded model and armor set, for the version your
server runs. It rebuilds after startup and after every `/hy reload`.

The same models always give the same zip, so players who already have the pack do not download it
again.

## How players get the pack

Pick one with `pack.hosting` in `config.yml`:

| `pack.hosting` | What happens | Use it when |
|---|---|---|
| `self-host` (default) | Hyperion serves the pack from a small web server on your machine | You can open a port |
| `url` | You upload the zip yourself and give Hyperion the link | You cannot open a port, or you want a CDN |
| `off` | Hyperion sends nothing | You merge Hyperion's pack into your own server pack |

### self-host

```yaml
pack:
  hosting: self-host
  self-host:
    port: 8163
    address: ""
```

- Open port `8163` in your firewall, or allocate it in your host's panel.
- Players download from the address they joined with. If the web server sits behind a different
  address, set `address` to the public IP or domain of the machine the server runs on.
- **Behind a proxy (Velocity, BungeeCord), always set `address`.** Players join the proxy, but
  the web server runs on the backend server.

### url

1. Run `/hy pack export`. It writes `plugins/Hyperion/hyperion-pack.zip`.
2. Upload the zip somewhere that gives a direct download link.
3. Put the link in `pack.url` and set `hosting: url`.
4. Upload it again after every change to your models, or downloads fail.

### off

Run `/hy pack export` and merge the zip into your own pack.

## Other settings

```yaml
pack:
  # Offer the pack to players when they join.
  send-on-join: true
  # Kick players who decline the pack or fail to download it.
  required: false
  # Send the new pack to everyone online when a reload changes it.
  resend-on-change: true
  # The item every model part is shown as; players never see the item itself.
  base-item: paper
  # The first custom model data number Hyperion uses on 1.21 to 1.21.3.
  custom-model-data-start: 10000
```

On 1.21 to 1.21.3, pick a `base-item` no other pack changes. Only a dyeable item such as
`leather_horse_armor` can be tinted on those versions. From 1.21.4 any item works the same.

## Commands

| Command | What it does |
|---|---|
| `/hy pack status` | Hosting, link, size and hash, and how players are doing with the pack |
| `/hy pack build` | Builds the pack again |
| `/hy pack send [players]` | Offers the pack again, to some players or everyone |
| `/hy pack export` | Writes the pack to `plugins/Hyperion/hyperion-pack.zip` |

## "Failed to download"

The client could not fetch the zip, or the file it got did not match. When this happens, Hyperion
logs a warning in the console that says which case it was:

| The console says | What it means | Fix |
|---|---|---|
| No request from them reached the pack server | The port is not reachable at the address the player was sent | Open the port, or set `pack.self-host.address`. Behind a proxy you need the address. |
| The pack server sent it, but the download failed | The download was cut off, or something between the player and the server changed the file | Check proxies, CDNs and firewalls in between |
| The pack server answered 404 | The link was for an older pack | Run `/hy pack send` |
| The pack server is not running | It could not start, usually because the port is in use | Look at the startup log, pick another `port` |
| With `hosting: url`: could not download from the link | The link gives a web page instead of the zip, or the upload is old | Use a direct link, and upload again after `/hy pack export` |

To test by hand, run `/hy pack status` and open the link it shows in your browser. If the zip
downloads, the port and address are right.

## Using Hyperion with another pack

Hyperion's pack loads next to your server's own pack; it does not replace it. If you already send
a pack and want one download, use `hosting: off` and merge the exported zip into yours.
