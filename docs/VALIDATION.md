# Validation evidence and outstanding acceptance

## Current outcome

The implementation and documentation are present, but the deliverable is **not yet a verified Minecraft plugin release**. The development environment supports the pure Java and SQLite test workflow. The full Spigot workflow remains blocked at dependency resolution.

| Check | Observed result |
| --- | --- |
| JDK | Eclipse Temurin 21.0.8+9 JDK installed; official SHA-256 verified |
| Maven | 3.9.9 installed; Maven Central SHA-512 verified |
| TLS | Verification enabled; installed JDK uses the managed environment's system Java CA trust store |
| `mvn -f tools/logic-tests/pom.xml clean verify` | **PASS — 43 tests, 0 failures, 0 errors, 0 skipped** |
| Repeated logic suite using the setup script | **PASS — 43 tests** |
| Java syntax parse | **PASS — 27 main/test source files**, without type attribution |
| Maven XML and resource YAML parse | **PASS** |
| Default `mvn clean verify` | **BLOCKED — HTTP 403 resolving Spigot API 1.21.4 from hub.spigotmc.org** |
| Bukkit API type compatibility | Unverified until the default build runs |
| Shaded plugin JAR | **Not produced** |
| Running Minecraft server / vanilla client | **Not run** |

The lightweight `tools/logic-tests` Maven project points at the **actual** production pure-Java source files and the same test directory. It does not contain Bukkit substitutes or mock API classes. Its verification JAR must not be installed on a server.

## Test coverage actually executed

- Progression: **15** cases including threshold boundaries, invalid curves, saturation, remaining points, stage validation, and defensive copying.
- Command argument validation: **12** cases including malformed IDs, negative/fractional/overflow amounts, normalization, and stage range checks. Bukkit command dispatch and permissions still require server acceptance.
- Transform and animation math: **11** cases including hierarchy, cycle detection, quaternion interpolation, five easing curves, composite tracks, orbit radius, and sequence/reveal timing.
- SQLite: **5** tests against real temporary databases, including restart persistence, overrides, deletion, stale revisions, ordered delete/recreate, invalid state, and propagation of failed database opening.

## Environment blocker and recovery

The managed instance reports its network policy as enforced. The live custom allowlist does not yet contain `hub.spigotmc.org`; an additive draft rule for that hostname has been saved. Review and save this rule in environment settings, then publish the environment as required by the onboarding flow. Saving the draft alone did not change the running network policy.

With the rule applied, use the existing checkout; no worktree is needed. The prepared toolchain can run:

```sh
export JAVA_HOME=/workspace/tools/jdk-21.0.8+9
export PATH="$JAVA_HOME/bin:/workspace/tools/apache-maven-3.9.9/bin:$PATH"
export MAVEN_OPTS="${MAVEN_OPTS:-} -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts"
cd /workspace/AetherPulse
mvn -s /workspace/tools/maven-settings.xml --batch-mode -U clean verify
```

`-U` retries the previously failed snapshot lookup. Keep the inherited proxy and CA configuration. Do not disable TLS, signatures, or checksums, or substitute an unrelated API artifact.

A successful full build should produce `target/AetherPulse-1.0.0.jar`. Inspect it for `plugin.yml`, `config.yml`, plugin classes, SQLite classes and native libraries; confirm Bukkit/JOML are excluded. Then execute [SERVER-TEST.md](SERVER-TEST.md) against Spigot/Paper 1.21.4 and record actual results before release. The visual composition, interaction event behavior, client interpolation, and lifecycle cleanup need this acceptance run.

The remote repository had no refs when the initial implementation was prepared. Environment draft persistence, publishing a filesystem snapshot, and pushing source commits are separate operations. Repository source and CI build results should be reviewed independently of the environment setup status above.
