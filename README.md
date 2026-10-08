# Myriad Boze

Every [Boze](https://boze.dev) module in Myriad's menu. Install it next to Myriad and Boze, and each Boze module
appears as a Myriad module: the same toggle, its settings as Myriad settings, its keybind in the Keybinds panel and
on the module card, searchable from the launcher, with Myriad's windows, workspaces and themes. Boze keeps working as
before: this addon is a second front end for it, not a replacement.

Needs [Myriad](https://myriadclient.dev/) ([source](https://github.com/fyzz-dev/myriad)) (0.2.0 or later: it relies on `Module.isMirror()` and on
registries that stay open after startup), Myriad Boze and Boze (through its loader) in `mods/`. Boze's addon API has
to be present, which it is whenever Boze itself runs; without Boze the addon loads, notes that there's nothing to
bridge, and does nothing.

## Building

Myriad comes from its maven (`https://fyzz-dev.github.io/myriad`), so a checkout builds on its own:

```bash
./gradlew build                 # build/libs/myriad-boze-<v>.jar
./gradlew test                  # the translation and sync, against fake Boze modules
./gradlew runClient -PopenDesktop   # Myriad + Essentials + this addon (Boze itself can't be run this way)
```

## What you get

- **Windows per category.** Boze's modules land in the shared categories (*Combat · Boze*, *Movement · Boze*, …) next to
  your other addons' windows, placed on the workspace that already has that category. Boze's API doesn't say which
  of its GUI categories a module is in, so the category is guessed from the module's package and name; the rest go
  in a *Boze* category. A module in the wrong window is a cosmetic miss: add its name to `BozeCategories.NAMES`.
- **Settings, translated.** Boze pages and folders become setting groups ("Render", "Misc · Sounds"); sub-options of a
  toggle show only while it's on, and options that only apply in some modes show only in those modes, as in Boze's
  own GUI (Aura in Grim mode shows 78 of its 94 settings, Auto Mine 48 of 79). Toggles, sliders, two-handle range sliders, modes, binds and colours (with a
  separate outline opacity where Boze has one) all edit in place.
- **Keybinds.** The module's bind is Boze's: set it in either GUI, it shows in both, and Boze handles the key. Hold
  mode and *Show In List* are Boze's "only while holding" and "visible" flags.
- **Two-way, live.** Toggle or edit in Boze's GUI, through its commands or by loading one of its profiles, and Myriad
  shows it within half a second. Edits in Myriad reach Boze at once.
- **Modules from Boze's own addons** are bridged too, in the category their addon declared.
- **One friends list.** Myriad's and Boze's friends are kept the same: on first contact the two lists are merged,
  then a friend added or removed on either side (Myriad's Friends panel or `.friend`, Boze's GUI or command) is
  added or removed on the other within a second.

## What stays Boze's

Boze owns the state. These modules are *mirrors* (`Module.isMirror()`): Myriad doesn't save them with its profile
(Boze's config and profiles hold everything), and `.profile` switches in Myriad leave them alone. `.panic` turns
them off like any other module. Chat feedback starts off, since Boze announces toggles itself.

Boze binds have no modifier keys, so a bind set in Myriad with Ctrl or Alt is stored as the bare key. Boze colours can
be animated (rainbows, gradients); Myriad shows the colour as it was when it last changed in Boze, and a colour picked
in Myriad is handed to Boze as a plain colour.

## How it works

Boze starts on its own schedule, possibly after Myriad, so the bridge waits: once Boze reports its modules, each one
is wrapped in a `BozeModule` and registered. There are no change events on Boze's side, so a tick handler reads
every module's state each tick and every option's value every half second (plain getters, a few thousand a second at
most, nothing measurable) and copies what changed into the Myriad settings; the settings' change listeners write
the other way.

| File | Does |
|---|---|
| `BozeAddon.java` | the entrypoint; the only class that runs without Boze; waits for Boze, then hands over |
| `BozeBridge.java` | finds Boze's modules (and ones its addons add later), registers mirrors, runs the sync |
| `BozeModule.java` | one mirror: toggle, keybind, hold and list visibility both ways |
| `OptionBindings.java` | Boze options to Myriad settings: groups, visibility, each type's binding |
| `BozeVisibility.java` | when Boze's own GUI shows an option (its per-mode conditions), found on Boze's internal options |
| `BozeCategories.java` | which category a Boze module goes in |
| `FriendSync.java` | the two friend lists reconciled by diffing against the last sync |
| `RangeSetting.java` | a low/high setting for Boze's range sliders |

The tests build Boze modules from the published API jar and check the translation and the sync without the client.
A real run needs a Boze account: put `boze-loader` in `run/mods` and `./gradlew runClient`; the loader signs in and
keeps its token in `~/.local/share/boze`. Seen on a real run (Boze loader 1.0.27, Minecraft 26.2): Boze starts after
Myriad and the bridge picks it up a moment later (245 modules); its client modules live in `dev.boze.client`; and
`getOptions()` lists every option flat, with parents set. Boze loads its profiles from the account, so settings
changed in a test run may reach your real Boze config: test with the settings you're willing to change.

Boze's client modules don't use the API's option visibility (`isVisible()` is always true for them); their GUI's
conditions sit on Boze's internal options, which `BozeVisibility` finds by shape (Boze is obfuscated, so names change
between versions). If a Boze update changes that shape, options simply all show again, as before.
