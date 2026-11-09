FuftiAC
FuftiAC is a Minecraft server-side anti-cheat plugin for detecting selected combat, movement, interaction, and packet-related anomalies.
Development status: UNSTABLE

FuftiAC is currently under development. This version is unstable and has not been fully validated in production environments. Detection accuracy, compatibility, and reliability are not guaranteed. False positives, missed detections, and unexpected behavior may occur. Use it at your own risk and test it on a staging server before deployment.

Detection checks
The repository currently contains the following checks.
Combat
Check	Purpose
Reach	Checks attack distance against a configured reach threshold.
Hitbox	Checks attacks where the player's view ray does not adequately intersect the target hitbox.
WallHit	Checks attacks when a solid block obstructs the line between the player and target.
NPC	Uses a spawned NPC-style target and tracks repeated hits as a combat heuristic.
TargetStrafe	Checks movement patterns around a recently attacked target, including repeated circular movement.


Movement
Check	Purpose
Fly	Checks movement and airborne state for flight-like behavior.
Speed	Checks horizontal movement speed against contextual conditions.
NoFall	Checks suspicious ground claims and fall-damage-related movement states.
NoClip	Checks movement through solid blocks.
Jesus	Checks movement over liquid surfaces.
Step	Checks unusually large vertical movement steps.
AirJump	Checks suspicious jumps while airborne.
AirStuck	Checks abnormal movement patterns while airborne.
Spider	Checks climbing behavior, including movement near recently placed blocks.
NoWeb	Checks movement speed while inside cobwebs.
NoSlow	Checks movement while using items or under slowing conditions.
Velocity	Checks whether expected velocity is reflected in subsequent player movement.
Velocity Legit	Performs an additional velocity-response check using the recorded expected and actual horizontal movement.
BoatFly	Checks boat movement for flight-like behavior.
ElytraRecast	Checks suspicious movement associated with repeated or abnormal Elytra state changes.


Player interaction
Check	Purpose
ElytraSwap	Checks Elytra/chestplate swapping behavior and related movement.
GuiMove	Checks inventory interaction while the player is moving.
Scaffold	Checks block placement patterns associated with bridging, placement direction, placement distance, and placement rate.
Nuker	Checks block-breaking distance, aiming toward the block, and rapid multi-break patterns.


These descriptions summarize the implemented logic at a high level. They do not represent a guarantee that every cheat using the corresponding technique will be detected.
Technical overview
- Platform API: Paper API 1.21.4-R0.1-SNAPSHOT in the Maven build.
- Language: Java 21.
- Build system: Maven.
- Packet handling: PacketEvents 2.7.0.
- Configuration: Bukkit YAML configuration.
- Player state: Runtime in-memory data keyed by player UUID.
- Scheduling: Bukkit scheduler with a Folia-specific scheduler path.
- Alerts: In-game staff alerts and server log output.
- Punishments: Optional configured commands and Bukkit name-ban operations.
The project metadata declares a wider server-version range, but the current Maven build is compiled against the Paper 1.21.4 API. Compatibility with other server versions and implementations must be verified independently; it should not be assumed from the metadata alone.
Requirements
- Java 21 runtime.
- A server implementation compatible with the APIs used by the plugin.
- Maven to build the project from source.
The Maven build includes PacketEvents as a shaded dependency. The project also declares optional integrations in plugin.yml, including ProtocolLib, ProtocolSupport, ViaVersion, ViaBackwards, ViaRewind, Geyser-Spigot, WorldEdit, WorldGuard, and FastAsyncWorldEdit. These are declared as soft dependencies; they are not all required to build the plugin.
Building
Clone the repository and run:
mvn clean package
The Maven configuration sets the output artifact name to:
target/FuftiAC.jar
Copy the generated JAR into the server's plugins directory and restart the server.
Configuration
On first startup, FuftiAC creates plugins/FuftiAC/config.yml from the bundled default configuration.

Each check can be configured under checks:
checks:
  FuftiACReach:
    enabled: true
    max-vl: 4
    punish: ""
    max-reach: 3.1
    buffer: 2
Common check settings:
Setting	Description
enabled	Enables or disables the check.
max-vl	Violation threshold configured for the check.
punish	Command configured to run when the punishment path is reached. An empty value means no command is configured.
Check-specific settings	Additional thresholds, buffers, and timing values used by individual checks.


Not every check uses the same additional settings. Refer to the bundled config.yml and the corresponding check implementation for the exact options.
Violation levels
Checks report violations through the shared check system. Player violation levels are stored in runtime memory and periodically decay according to vl-decay-seconds.

Player check data is cleared when the plugin shuts down. The current implementation does not provide persistent violation-history storage.
Alerts
Staff alerts can be customized using alerts.format and alerts.hover. Available placeholders in the default configuration include:
- %player%
- %check%
- %vl%
- %max%
- %bar%
- %detail%
- %ping%
- %world%
- %x%, %y%, %z%
- %prefix%
The alert message includes hover information and suggests a teleport command when clicked.
Punishments
Automatic punishments are disabled by default:
punishments:
  enabled: false
To use automatic punishments, enable the global setting and configure a command for the relevant check. Review all thresholds and command templates before enabling this feature.

The plugin also provides manual ban and unban commands. These use the server's Bukkit name-ban list.
Commands
Command	Description
/fuftiac	Displays command usage.
/fuftiac alerts	Toggles alerts for the executing player.
/alerts	Toggles FuftiAC alerts.
/fuftiac info <player>	Displays the online player's violation levels.
/fuftiac ban <player> <duration> [reason]	Bans an online or offline player.
/fuftiac unban <player>	Removes a name ban.
/fuftiac punish on	Enables automatic punishments.
/fuftiac punish off	Disables automatic punishments.
/fuftiac reload	Reloads the plugin configuration.


Supported duration formats include seconds (s), minutes (m), hours (h), days (d), and weeks (w). Permanent duration aliases include perm, permanent, and forever.
Permissions
Permission	Default	Description
fuftiac.alerts	true	Allows use of the /alerts command and alert-related permission checks.
fuftiac.ban	op	Allows /fuftiac ban.
fuftiac.unban	op	Allows /fuftiac unban.
fuftiac.info	op	Allows /fuftiac info.
fuftiac.admin	op	Grants administrative command access, including reload and punishment toggling.


fuftiac.admin inherits the alert, ban, unban, and info permissions through the plugin permission declaration.
Known limitations
- This is an unstable development version and has not been established as production-ready.
- Detection is heuristic and may produce false positives or fail to detect some cheat implementations.
- Server version, protocol translation, latency, server performance, plugins, world conditions, and player effects can influence movement and combat behavior.
- The current Maven build targets the Paper 1.21.4 API. Broad compatibility claims in plugin metadata have not been independently verified.
- Violation data is held in memory and is not persisted across plugin restarts.
- Automatic punishments are disabled by default and require explicit configuration.
- The presence of a check class does not guarantee that every possible exploit or bypass is covered.
Development and testing
Before using FuftiAC on a production server:
1. Test each enabled check on the exact server version and software fork in use.
2. Test with different latency values, movement effects, vehicles, liquids, blocks, and game modes.
3. Review staff alerts and violation levels before enabling automatic punishments.
4. Adjust check-specific thresholds only after collecting test results.
5. Keep automatic punishments disabled until false-positive behavior has been evaluated.
License
No license file was included in the provided repository archive. Unless a license is added to the repository, all rights remain with the copyright holder and reuse or redistribution is not automatically granted.
