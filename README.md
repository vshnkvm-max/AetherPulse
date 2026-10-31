# AetherPulse — Living 3D Monuments

An evolving magical mechanism built entirely from vanilla Minecraft display entities. Crystal Core combines a layered amethyst heart, three independently tilted segmented rings, orbiting shards, a holographic progress indicator, and budgeted particle effects. Server progress wakes the monument through five distinct stages.

No client mods, resource pack, NMS, ProtocolLib, or external visual plugins are required.

## Key Features

- **Five stages:** Dormant, Awakening, Energized, Unstable, and Ascended.
- **Procedural composition:** BlockDisplay crystal facets and rings, ItemDisplay accents and fragments, TextDisplay hologram, and an Interaction hitbox.
- **Continuous animation:** quaternion rotations, nested transforms, bobbing, breathing, orbiting fragments, smooth component reveals, and an Ascended lift/spin/settle sequence.
- **Interactive cores:** right-click or hit the central hitbox to inspect progress, hear a chime, and trigger a visual response.
- **Progression:** configurable thresholds, administrative commands, and survival block-breaking contributions to the nearest monument.
- **Persistence:** asynchronous SQLite writes, ordered updates, a final shutdown flush, and chunk-aware reconstruction.
- **Performance controls:** one animation task, distance culling, a fair global particle budget, bounded display counts, and no per-tick entity spawning.

## Requirements

- Java **21 JDK** to build; Java 21 runtime to host.
- Spigot **1.21.4** or a corresponding Paper server. Other versions are not certified.
- Maven **3.9+** to build.
- Access to Maven Central and `hub.spigotmc.org` during the first build.

SQLite JDBC is bundled in the shaded JAR, including native libraries. JOML and Bukkit are supplied by the server. No external database is needed.

## Installation

1. Build with `mvn clean verify` or obtain the resulting release JAR.
2. Copy `target/AetherPulse-1.0.0.jar` to the server's `plugins/` directory. Do not install the `original-` JAR.
3. Start the server and wait for `AetherPulse is ready` in the console.
4. Stand in an open area and run `/ap create crystal1` as an operator.
5. Run `/ap addpoints crystal1 100` to reveal the first ring. Use 400, 1000, and 2500 total points to unlock subsequent stages.

The installation generates `plugins/AetherPulse/config.yml` and `monuments.db`. Back up the data directory while the server is stopped; live SQLite backups must include the WAL correctly. Shut down cleanly before replacing the plugin. Prefer full server restarts over the server-wide `/reload` command.

## Commands

All commands have the alias `/aetherpulse` and require `aetherpulse.admin`. Console can use all except `create`.

| Command | Purpose |
| --- | --- |
| `/ap create <id>` | Place a Dormant core at your feet (visual center is 2.1 blocks higher) |
| `/ap remove <id>` | Remove its scene and stored record |
| `/ap list` | List registered monuments, including inactive worlds |
| `/ap info <id>` | Show stage, points, position, and active entity count |
| `/ap addpoints <id> <amount>` | Add nonnegative points, saturating at the maximum Java long |
| `/ap setprogress <id> <amount>` | Set nonnegative points and recalculate stage |
| `/ap stage <id> <1-5>` | Set a persisted **visual override** for testing; points stay unchanged |
| `/ap reload` | Validate YAML and rebuild scenes using the new settings |
| `/ap help` | Show command usage |

IDs are case-insensitive, normalized to lowercase, and contain 1–32 ASCII letters, digits, underscores, or hyphens. Tab completion suggests commands, registered IDs, and stages. Any point update, including adding zero, clears the visual stage override. A rejected configuration reload preserves the currently running configuration.

## Permissions

| Permission | Default | Purpose |
| --- | --- | --- |
| `aetherpulse.admin` | Operators | All administrative commands |
| `aetherpulse.interact` | Everyone | Interact with the hitbox and inspect a core |

Block contributions do not require admin permission. Only uncancelled Survival-mode block breaks count. If multiple cores are in range, only the nearest receives points; ties use registry order. Contributions respect cancellation by protection plugins. The MVP counts repeatable placed-block breaks as well; choose block values appropriate to your server economy.

## Configuration

See [`config.yml`](src/main/resources/config.yml) for the complete annotated defaults. Distances are in blocks; ring speeds are radians per second; intervals and interaction cooldowns are server ticks.

```yaml
performance:
  animation-update-interval: 2
  particle-update-interval: 4
  activation-radius: 48
  max-particles-per-cycle: 100
  max-display-entities: 100
progress:
  thresholds: [0, 100, 400, 1000, 2500]
```

- Five thresholds must start at zero and strictly increase. Progress is nonnegative and stored as a 64-bit integer.
- Rings have individual radius and signed rotation speed. `visual.segments` controls the number of pieces **per ring**.
- `visual.fragments` controls the orbiting shard count. Core size, dust color, intensity, effect switches, and sounds are configurable.
- The per-monument display budget must cover `5 + 3 × segments + fragments`. There is one additional Interaction entity. Defaults allocate **71 displays + 1 Interaction** at full scene creation, including hidden components.
- All components are allocated once, including those hidden by stage. Stages animate their scale from zero; blocks do not support real alpha fading. The hologram remains visible.
- `visual.particles.intensity` is bounded from 0 to 3; `max-particles-per-cycle` is a **global** cap shared fairly by active monuments, including level bursts.
- Block material keys must be valid block names and point awards must be positive. Set `progress.blocks.enabled: false` to disable automatic contributions.
- `/ap reload` removes existing scenes and reconstructs them on the next eligible update without forcing chunks to load. Existing points and explicit stage overrides survive.

Display brightness is fullbright; these entities **do not emit world light**. Dust particles and vanilla models provide the visual glow. The implementation does not promise shaders or translucent block geometry.

## Technical Architecture

```text
src/main/java/dev/aetherpulse/
├── AetherPulsePlugin             composition and lifecycle
├── animation/                    central scheduler, reusable tracks, easing, sequences
├── visual/                       parented TRS, matrix rendering, factories, scene ownership
├── monument/                     stage model, registry, procedural Crystal Core
├── progress/                     pure progression curve, provider API, block integration
├── interaction/                  hitbox events and per-player cooldowns
├── storage/                      immutable snapshots and serial SQLite worker
├── command/                      validation, permissions, routing, completion
└── config/                       validated immutable settings snapshot
src/main/resources/               plugin.yml and config.yml
src/test/java/dev/aetherpulse/     math, progression, input, and real SQLite tests
```

All Bukkit world/entity access runs on the main server thread. SQLite owns one dedicated executor and connection. Progress snapshots are coalesced every 100 ticks (five seconds at 20 TPS), so sudden process termination can lose recent awards; clean shutdown queues a final complete snapshot and drains pending database operations. A failed save is logged and retried. Failed deletions retain an ID reservation and retry before ID reuse. Disk failure or a shutdown timeout is reported, not treated as successful persistence.

Each entity is tagged with the monument ID through PersistentDataContainer and marked nonpersistent. Scene owners remove their resources on unload, removal, rebuild, and disable. Tagged stale entities in loaded chunks are cleaned during startup and chunk load. Missing worlds remain registered and dormant until they become available. No chunk tickets or force-loading are used.

Transforms use local translation/quaternion/scale and recursive world matrices. Matrix composition preserves parent rotations and nonuniform scaling. Cyclic parent relationships are rejected. Animation clips can be composed concurrently or sequenced with local time; the core uses a shared root and independent ring parents. Display interpolation spans the configured animation interval.

### Integration API

`ProgressProvider` is the extension contract. The built-in `ProgressManager` exposes `award(player, monumentId, points)` and can be obtained from the enabled `AetherPulsePlugin` via `progress()`. Invoke it on the primary server thread after the storage-ready log. External plugins should declare a dependency or soft dependency on AetherPulse and marshal asynchronous results back to the main thread. Point updates validate input and use the same persistence and visual transition paths as commands.

## Performance Notes

- Exactly one repeating Bukkit task services all monuments; no task per display or monument.
- Animation and particle intervals are independent. Prefer the default two-tick animation interval: the client interpolates intermediate frames.
- No animation or particle updates run without a nearby player. The local animation clock pauses, preventing a catch-up jump on return.
- Existing entities remain stationary while viewers are absent; they are removed on chunk unload. New scenes spawn only for loaded chunks with nearby players (or an explicit create command).
- If an entity disappears externally, the next active update reconstructs the scene as a unit.
- Default full-stage scene: 72 entities. Many cores increase entity tracking, rendering, and network costs even with animation culling. Profile representative player counts before choosing production limits.
- Registry/viewer checks are linear in monument and world-player counts; the MVP targets a modest number of showcase cores. Large deployments should add a spatial index before raising counts substantially.

## Building from Source

```sh
java -version
javac -version
mvn --batch-mode clean verify
```

The default build compiles against **Spigot API 1.21.4**, runs JUnit 5 tests, and shades SQLite JDBC into `target/AetherPulse-1.0.0.jar`. It does not shade server-provided Bukkit/JOML. TLS and artifact checks remain enabled. The GitHub Actions workflow runs the same verification and uploads the plugin artifact after a successful build.

Tests cover exact stage boundaries, overflow saturation, malformed command input, parent/child matrices, quaternion interpolation, easing, combined tracks, orbit geometry, appearance sequences, SQLite reopen durability, stale snapshot protection, ordered delete/recreate, and storage failure propagation. They do not replace a running-server test of Bukkit entity behavior.

To run only the Bukkit-independent logic/storage suite when the Spigot repository is unavailable:

```sh
mvn -f tools/logic-tests/pom.xml clean verify
```

This compiles the real pure-Java source files and runs the same tests. Its small verification JAR is **not a Minecraft plugin**, and success does not establish Bukkit API compatibility or replace the default build.

Current execution evidence and environment limitations are recorded in [`docs/VALIDATION.md`](docs/VALIDATION.md). See [`docs/SERVER-TEST.md`](docs/SERVER-TEST.md) for a reproducible in-game acceptance checklist.

## Showcase

1. Choose a dark open chamber with a dark stone floor, at least 9 × 9 × 9 blocks of free space, and no competing particle effects.
2. Create a core at floor level: `/ap create showcase`. Move back 6–8 blocks so the layered heart reads clearly.
3. Record the Dormant core from a slow orbit. Right-click the center to show the hologram and interaction response.
4. Use `/ap setprogress showcase 100`, then 400 and 1000, waiting 4–5 seconds between commands. Capture each ring revealing over 2.5 seconds and fragments entering their orbits.
5. Frame the complete mechanism at a three-quarter angle. Set progress to 2500 and capture the final lift, accelerated spin, settling, burst, and Ascended accents.
6. Take a close-up of the segmented rings rotating on different planes, then a wide shot showing the orbiting shards and hologram.
7. Use `/ap setprogress showcase 0` to reset for another take. Keep default particles initially; increase intensity only after checking client frame rate.

Use an ordinary Minecraft 1.21.4 client to demonstrate the no-resource-pack experience. This repository does not contain a recorded gameplay showcase or a claim that visual acceptance has been completed.

## Future Extensions

- Quest, economy, PlaceholderAPI, and server-wide progress integrations using the provider API.
- Additional monument types and configurable scene presets.
- Persistent anti-farming rules for player-placed blocks.
- Per-player visibility and localization, editor GUI, and paginated management for large installations.
- Spatial indexing, profiling-guided allocation reduction, and optional multi-server storage.

These are expansion ideas; the Crystal Core implementation does not require any of them.
