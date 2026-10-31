# Running-server acceptance checklist

Use a disposable Spigot 1.21.4 or Paper 1.21.4 server with Java 21. Build the default Maven target first. Never point acceptance tests at a production world. Check the console for exceptions after each step.

1. Install only the shaded AetherPulse JAR. Start the server and wait for the storage-ready message. Join with a vanilla client and operator permissions.
2. At a clear site run `/ap create crystal1`. Confirm a layered amethyst/purpur core, end-crystal accent, readable hologram, and no block placement in the world. A duplicate ID (including uppercase) must be rejected. Console create must report that a player is required.
3. Run `/ap setprogress crystal1 99`, then 100. Confirm Awakening appears with one slowly revealing ring. Test 399 → 400, 999 → 1000, and 2499 → 2500. Confirm two rings plus orbiting shards, three rings plus streams, and the final Ascended lift/spin/settle. Observe smooth interpolation and no new entity creation during steady-state animation.
4. Set progress back to zero. Components should shrink away smoothly. Set `/ap stage crystal1 5`: confirm points are unchanged and override is labeled. Add zero points: the automatic stage must return.
5. Right-click and hit the central Interaction hitbox. Verify stage, points, next natural threshold, sound, and pulse. Repeated interactions inside the cooldown must not spam output. A player without `aetherpulse.interact` must get no interaction response; one without admin permission must not mutate monuments.
6. Test malformed commands: missing/extra arguments, unknown ID, negative/overflow/fractional amounts, invalid stage, and illegal IDs. Check tab completion as operator and as an ordinary player.
7. With progress zero and Survival mode, break an allowed block inside contribution radius. Confirm only the nearest monument receives the configured points. Creative breaks, disallowed materials, outside-radius breaks, and protection-cancelled breaks must not contribute.
8. Move all players beyond activation radius while keeping the chunk loaded. Observe that animations and particles pause. Return and confirm they resume without duplicating entities. Unload the chunk and revisit; the scene should reconstruct once. Repeat at negative coordinates and chunk boundaries.
9. Cleanly stop/restart the server after awarding points. Verify ID, location, world, points, and stage/override. Load and unload chunks multiple times. Inspect tagged entities for duplicates. Keep a world unavailable and confirm the record remains dormant, then restore that world.
10. Delete one scene entity administratively and confirm the next active update rebuilds exactly one scene. Remove the monument: all displays and its Interaction must disappear. Restart to confirm deletion persisted.
11. Change valid ring speeds, particle switches, and intervals, then `/ap reload` several times. Confirm new settings and stable entity counts. Introduce invalid YAML, thresholds, or entity budget: reload must fail clearly while existing scenes continue. Restore the config.
12. Create multiple cores and use a server profiler to check the shared scheduler and particle budget. Reduce the budget to a small value and verify fair rotation of effects; no scene should exceed its display limit. Stop the server and confirm all plugin tasks and entities are removed.

Report the exact server build, client version, test outcomes, and logs. A passing unit suite or a successful JAR build alone does not establish these results.
