# Modules

## Render modules

| ID | Category | Description | Settings | Limitation |
| --- | --- | --- | --- | --- |
| `fullbright` | Render | Locally increases brightness with reversible gamma or Night Vision. | `use_gamma`, `night_vision`, `brightness` | Gamma is constrained to Minecraft's 0.0–1.0 option range; Night Vision is visual and client-local. |
| `anti_blind` | Render | Hides selected local impairment visuals. | `blindness`, `darkness`, `nausea`, `pumpkin_overlay`, `powder_snow_overlay` | Does not remove effects from the local player or change server state; it only filters their 26.2 render paths. |
| `anti_totem_animation` | Render | Hides the local death-protection item activation overlay. | none | It suppresses only the local `GameRenderer` visual activation for an item with Minecraft's death-protection component. Totem effects, particles, sound, inventory changes, and server processing remain unchanged. |
| `dinnerbone` | Render | Applies Minecraft's upside-down transform to selected local living-entity models. | `players`, `hostiles`, `passive` | It affects only the local `LivingEntityRenderer` result and preserves vanilla's existing Dinnerbone/Grumm behavior. Players and `Monster` subclasses are distinct selectable categories; other living entities use the passive category. It does not change pose, data, collision, or packets. |
| `rainbow_enchant` | Render | Tints the local enchantment glint emitted for item stacks. | `color`, `rainbow`, `rainbow_speed` | It wraps only the 26.2 `ItemFeatureRenderer` foil vertex color. The item, enchantments, textures, server state, and worn armor-layer glint remain unchanged. |
| `better_crosshair` | Render | Draws a local four-arm HUD crosshair. | `size`, `gap`, `thickness`, `outline`, `color`, `dynamic_movement`, `hide_vanilla` | Dynamic movement uses local horizontal velocity; hit detection and server packets are unaffected. |
| `better_nametags` | Render | Draws bounded local facts as stacked, color-coded billboard rows above nearby rendered players: name/friend, fraction-colored health, armor, then distance and held item. | `health`, `distance`, `armor`, `held_item`, `friend_status`, `range` | It processes only frustum-visible nearby players, skips the local player, and reads already-loaded health/equipment/friend facts. Zero armor and an empty hand produce no row. It does not modify vanilla name tags or expose hidden/unloaded players. |
| `saturation_display` | Render | Displays the local player's observed hunger saturation as a HUD readout. | none | It reads only Minecraft's local `FoodData` value, rounds for display, and never changes hunger, saturation, food consumption, or server state. Its enabled state and placement are local HUD-editor choices. |
| `entity_esp` | Render | Locally highlights selected rendered entities in Outline, Box, Glow, or Shader mode. | `players`, `hostiles`, `passive`, `items`, `projectiles`, `friends`, `range`, `maximum_entities`, `mode`, `line_width`, `color`, `friend_color`, `fill_color` | It selects at most 512 targets per frame/tick and classifies every non-hostile living entity as passive. Outline draws unfilled local Gizmo wireframes and Box adds the fill color. Glow and Shader route the same selection through Minecraft's genuine entity-outline post-processing pass by answering `Minecraft.shouldEntityAppearGlowing` for Helikon's tick-snapshotted targets only: Glow keeps the vanilla team-derived outline color, while Shader overrides the extracted `EntityRenderState.outlineColor` with the module/friend color (forced opaque, because the vanilla pass treats color 0 as no outline). No mode calls `Entity.setGlowingTag` or mutates server-provided entity flags; disable, mode changes away from Glow/Shader, and world changes clear the local target snapshot, which restores vanilla glow behavior exactly. Glow/Shader use vanilla's outline rendering rather than per-entity width, so `line_width` affects only Outline/Box and `fill_color` affects only Box. |
| `block_esp` | Render | Incrementally scans a bounded local cube for configured block IDs and draws Gizmo boxes. | `blocks`, `block_colors`, `horizontal_range`, `vertical_range`, `scan_budget`, `tracers`, `line_width`, `color`, `fill_color` | It checks 64–2048 blocks per client tick and retains at most 512 matches. `block_colors` accepts bounded `block=#RRGGBB` or `block=#AARRGGBB` entries; malformed IDs/colors fall back to the shared local color. |
| `tracers` | Render | Draws local eye-to-entity lines for selected nearby rendered entities. | `players`, `hostiles`, `passive`, `items`, `projectiles`, `friends`, `range`, `maximum_entities`, `line_width`, `color`, `friend_color` | It renders at most 512 local lines per frame. Lines are a visual aid only and do not imply visibility, targeting, or reach through blocks. |
| `breadcrumbs` | Render | Draws a bounded local trail of recent player positions. | `maximum_points`, `maximum_age_seconds`, `sampling_distance`, `always_on_top`, `line_width`, `color` | The trail is session-only, clears on disable or world change, and has no persistent export. |
| `trajectories` | Render | Predicts local paths for visible in-flight projectile entities. | `arrows`, `tridents`, `snowballs`, `eggs`, `ender_pearls`, `splash_potions`, `maximum_steps`, `maximum_projectiles`, `line_width`, `color`, `impact_color` | It simulates only currently frustum-visible in-flight projectiles, uses verified family physics/tick ordering, and stops at the first local block collision. It does not alter projectile state, predict entity collisions, or provide a held-item aiming preview. |
| `true_sight` | Render | Draws local translucent boxes around selected invisible entities. | `players`, `hostiles`, `passive`, `range`, `maximum_entities`, `transparency`, `line_width`, `color` | It draws a box only when the entity is invisible to the local player and frustum-visible, without forcing vanilla invisible models to render. This safe initial visualization supports living categories only and does not change entity state. |
| `radar` | Render | Draws a compact local overhead entity radar HUD with an optional terrain minimap. | `shape` (`circle`, `square`), `minimap`, `rotate`, `zoom`, `players`, `hostiles`, `passive`, `items`, `friends`, `maximum_entities`, `background_color`, `entity_color`, `friend_color` | Its enabled state and placement are local HUD-editor choices. It reads only currently loaded entities and surface blocks, caps entity points at 512, and never reveals unloaded data. The cached minimap terrain stays underneath the existing entity dots. |
| `xray` | Render | Locally rebuilds chunk geometry to retain only configured block models. | `blocks`, `opacity` | The list accepts at most 32 valid semicolon-delimited block IDs; if none are valid, the module keeps normal geometry rather than hiding every block. Enable/disable and setting changes rebuild loaded local geometry; it does not request chunks or alter world data. Some block-model/material edge cases may need port-specific validation after Minecraft updates. |
| `storage_esp` | Render | Incrementally scans bounded loaded chunks for selected block entities and draws local boxes. | `chests`, `barrels`, `shulkers`, `furnaces`, `hoppers`, `spawners`, `custom_block_entities`, `horizontal_range`, `vertical_range`, `scan_budget`, `line_width`, `color`, `fill_color` | It checks 64–2048 blocks per client tick, retains at most 512 matches, and requires both a selected ID and a local block entity. Boxes are frustum-culled; newly loaded or changed storage can take one scan pass to appear or disappear. |
| `mini_player` | Render | Draws a transparent local HUD miniature of the current player model. | `rotation`, `scale`, `armor` | Its enabled state and placement are local HUD-editor choices. It intentionally draws no panel background or outline. Armor off clears only the temporary extracted render state; player inventory/equipment is never changed. |
| `damage_indicators` | Render | Draws fading local damage labels for observed nearby living entities. | `players`, `hostiles`, `passive`, `range`, `maximum_tracked_entities`, `maximum_indicators`, `lifetime_seconds`, `rise_distance`, `color` | It reports only a locally observed positive health delta while Minecraft reports a current hurt state, so some damage may not have an amount available. Nearest observations and labels are independently bounded, frustum-culled, and never affect combat. |

## Combat modules

| ID | Category | Description | Settings | Limitation |
| --- | --- | --- | --- | --- |
| `anti_bot` | Combat | Locally marks player entities matching conservative bot heuristics. | `tab_list_presence`, `impossible_state`, `minimum_spawn_age`, `duplicate_names`, `invisible`, `missing_profile` | It reads only already-loaded player/tab-list facts. A heuristic is not proof of a bot, is never sent anywhere, and is used only as a local exclusion input. |
| `trigger_bot` | Combat | Requests one normal attack only for the current eligible crosshair target. | `delay_ticks`, `weapon_required`, `players`, `hostiles`, `passive`, `exclude_friends` | It requires locally observed line of sight, normal attack cooldown, and optionally a conventional melee-item ID. It never changes reach or creates an attack packet. |
| `bow_aim_assist` | Combat | Smooths local aim toward one eligible target while Use is held with a bow. | `players`, `hostiles`, `passive`, `exclude_friends`, `range`, `field_of_view`, `prediction`, `projectile_speed`, `gravity`, `adjustment_speed` | It adjusts only local view rotation at a bounded rate, draws one local Gizmo outline, and never draws/releases a bow, sends a custom rotation packet, or targets through blocks. Prediction is an estimate from visible local velocity. |
| `critical_assist` | Combat | Times a normal held attack during an ordinary falling critical opportunity. | `delay_ticks`, `exclude_friends` | It requires the user to hold Attack, a normal cooldown, downward fall, no ground/water/climb/fall-flying state, and local line of sight. It never moves the player or uses packet exploits. |
| `auto_potion` | Combat | Selects and normally uses a local restorative hotbar potion at low health. | `health_threshold`, `potion_whitelist`, `kind` (`splash`, `drink`, `both`), `delay_ticks` | It accepts only hotbar potions whose observed effects include Instant Health and whose potion ID is whitelisted. It uses Minecraft's ordinary held-item path, never throws/drinks a non-restorative potion, and restores only a slot it still owns. |
| `target_hud` | Combat | Displays a locally observed crosshair/attack target. | none | The HUD placement is a local editor choice; it shows name, health, armor, distance, held item, and up to four observed effect description IDs. It does not query a server or imply target validity. |
| `kill_aura` | Combat | Requests one normal attack for a selected locally observed target. | `players`, `hostiles`, `passive`, `exclude_friends`, `range`, `field_of_view`, `delay_ticks`, `rotation_speed`, `target_mode` (`single`, `switch`), `priority` (`distance`, `health`, `angle`) | It applies a bounded local rotation adjustment and requires local line of sight, so it does not attack through solid blocks. The shared bridge permits at most one Helikon ordinary attack per client tick; server cooldown, reach, and combat validation remain authoritative. Invisible-player eligibility is controlled by the AntiBot heuristic rather than an unconditional visibility rejection. |
| `reach_display` | Combat | Shows the locally measured distance of Helikon's last ordinary attack request. | none | It is a fixed local HUD readout, not a reach modifier. Physical attacks and server-accepted distances are not inferred or claimed. |

## Movement modules

| ID | Category | Description | Settings | Limitation |
| --- | --- | --- | --- | --- |
| `auto_sprint` | Movement | Requests ordinary local sprinting while its conditions pass. | `always`, `forward_only`, `hunger_check`, `collision_check` | It sends no custom packets and cannot make a server accept sprinting or movement it rejects. It releases only sprint state it requested. |
| `auto_walk` | Movement | Applies continuous local forward input. | `continue_forward`, `stop_on_gui`, `allow_steering` | It changes only the freshly polled local input record. `stop_on_gui` is on by default; turning it off intentionally permits movement while screens are open. |
| `auto_sneak` | Movement | Applies a local sneaking policy. | `mode` (`toggle`, `hold`, `edge_only`) | Toggle holds sneak while enabled; Hold reserves its bound key as input-only after enabling the module through the GUI or a local command; Edge-only holds sneak while moving so vanilla careful movement guards ledges. It is inactive while a screen is open. |
| `auto_parkour` | Movement | Requests normal Jump input at safe locally observed ledges. | `minimum_movement_speed` | It requires forward motion, ground contact, a loaded empty ledge, a non-lava landing no more than two blocks below with a sturdy upper face, and no screen. It reads no unloaded blocks, does not move the player directly, and never creates a packet. This first conservative mode handles only a cardinal block in front of the local player. |
| `inventory_walk` | Movement | Allows configured physical keyboard movement in the vanilla player inventory. | none | It is confined to `InventoryScreen`, preserves inventory Shift behavior rather than turning it into sneak, and pauses whenever any inventory widget has focus (including text entry). Mouse/scancode movement bindings and non-inventory screens are intentionally unsupported. |
| `anti_afk` | Movement | Performs sparse selected ordinary local actions after idle time. | `rotation`, `jump`, `short_movement`, `interval_seconds` | It is disabled by default, waits 5–300 seconds after manual movement or any screen, turns at most 15 degrees, requests a jump only while locally grounded, and requests at most one forward-input tick per interval. It uses no custom packets and can still be limited by normal server rules. |
| `no_slow` | Movement | Selectively removes configured local vanilla movement slowdowns. | `eating`, `blocking`, `bow_use`, `sneaking`, `soul_sand`, `honey`, `cobwebs` | It changes only this client's use/sneak/block slowdown calculations and has no bypass presets. Soul-sand/honey checks use the local support block; cobweb handling is limited to Minecraft's normal local stuck-in-block callback. Servers retain all movement authority. |
| `fast_ladders` | Movement | Applies a bounded local climb velocity on climbable blocks. | `climb_speed` | It acts only while Minecraft reports the local player as climbable and normal forward/jump/sneak input is present. The server may correct unsupported movement. |
| `water_jump` | Movement | Requests ordinary local Jump input at a suitable water edge. | none | It requires local water, forward input, a loaded sturdy block directly ahead, two clear replaceable blocks above it, and no open screen. It does not directly move the player, load chunks, or create a packet. |
| `jesus` | Movement | Holds the local player steadily at the actual surface of ordinary water. | `catch_depth` | It snaps the player's feet to the loaded fluid surface with zero vertical velocity instead of applying recurring buoyancy. Jump releases upward and Sneak permits normal diving. It does nothing while riding, ability-flying, Elytra-flying, in screens, or outside ordinary water; multiplayer servers can correct unsupported motion. |
| `spider` | Movement | Applies bounded local upward velocity while moving into a horizontal collision. | `climb_speed` | It yields to normal ladder/climbable movement, sneaking, riding, ability flight, Elytra flight, and open screens. It changes only local velocity; multiplayer servers can correct unsupported wall climbing. |
| `high_jump` | Movement | Raises the local velocity of one ordinary ground jump. | `jump_velocity` | It applies one local upward-velocity adjustment only after a normal grounded jump while Jump is held, preserving stronger existing upward velocity. It does not sustain ascent and yields to screens, fluids, climbing, riding, ability flight, and Elytra flight. Multiplayer servers can correct unsupported movement. |
| `step` | Movement | Raises the local player's normal collision step height. | `step_height` | It is capped at 3.0 blocks and applies only to the local player's ordinary collision path through the LivingEntity step-height override; no step packets or bypass modes are used. |
| `speed` | Movement | Applies local acceleration, strafe assistance, or multiplier motion. | `mode` (`vanilla_acceleration`, `strafe_assist`, `multiplier`), `multiplier`, `acceleration`, `maximum_speed` | Defaults use Multiplier mode at the 3.0× ceiling, maximum acceleration (0.08), and the maximum 0.90 horizontal cap. Multiplayer servers can reject or correct movement; this module displays no anti-cheat presets. |
| `bunny_hop` | Movement | Requests normal local jump input while moving and caps horizontal momentum. | `auto_jump`, `speed_limit` | It is inactive in screens and never adds a jump packet. Its cap is local only; server movement rules remain authoritative. |
| `flight` | Movement | Player ability flight where Minecraft permits it and local player velocity flight elsewhere. | `flying_speed`, `survival_velocity`, `velocity_speed` | With `mayfly` (creative/spectator/permitted) it uses normal abilities and restores only state Helikon changed. Without `mayfly`, `survival_velocity` sets ordinary local player velocity each tick (hover on no input, hold while a screen is open). It does not control ridden boats or the camera. |
| `boat_flight` | Movement | Velocity-steers a locally driven ridden boat, including jump-key ascent and sneak-key descent. | `speed` | This is independently toggleable from Flight. It uses only normal client movement updates—no packet manipulation or anti-cheat bypass—so servers that enforce movement can reject, rubber-band, or kick. Opening a screen holds the controlled boat stationary. |
| `freecam` | Movement | Detaches a local-only camera without moving the player or sending player movement input. | `speed` | This is independently toggleable from Flight. It creates an invisible camera entity that is never added to the level, captures movement/look input locally, and restores the player camera when disabled or the world becomes unavailable. |
| `no_fall` | Movement | Prevents ordinary fall damage by resetting accumulated fall distance. | none | While falling normally it sends Minecraft's standard grounded movement status and clears the matching local accumulator. It does not run while riding, ability-flying, or Elytra-flying. Servers remain authoritative and may reject or penalize the reported status. |
| `extra_elytra` | Movement | Adds local Elytra pitch/landing assistance plus a speed and durability HUD readout. | `pitch_assist`, `target_pitch`, `pitch_adjustment`, `safer_landing`, `landing_distance`, `durability_warning`, `speed_display` | It changes only local view pitch while fall-flying and reads existing local velocity/equipment. The fixed HUD is hidden by panic; no flight physics or durability is changed. |
| `scaffold` | Movement | Requests normal held-block placement below or ahead while Use is held. | `placement` (`below`, `ahead`), `select_hotbar_block`, `rotate_to_target`, `tower`, `edge_safety`, `placement_delay_ticks` | It selects only player-provided hotbar blocks, requires a loaded replaceable target/support and vanilla use cooldown, then calls one normal block interaction. Rotation/jump/sneak requests are local and server placement/reach rules remain authoritative. |
| `timer` | Movement | Applies a bounded local client game-time multiplier. | `tick_multiplier` | The range is 0.5×–1.25×, it resets on world leave, and it does not alter server tick rate, packets, or multiplayer authority. |

## Player modules

| ID | Category | Description | Settings | Limitation |
| --- | --- | --- | --- | --- |
| `auto_tool` | Player | Selects the best safe hotbar tool while the user is already mining. | `minimum_durability`, `restore_prior_slot` | It changes only the local selected hotbar slot, ignores bare-hand-equivalent items and guarded durability, and uses Minecraft's normal mining path. It never creates a mining or inventory packet. |
| `auto_eat` | Player | Uses existing safe hotbar food when local hunger or health is low. | `hunger_threshold`, `health_threshold`, `food_priority`, `avoided_foods`, `combat_rule` | It selects regular food below full hunger, or an eligible always-edible food at full hunger when health is low. It only holds the normal local Use key when no screen is open and the player is not already using an item manually; an overlapping physical hold is preserved. It restores only a slot it still owns and releases module-owned Use state immediately on disable or panic. |
| `auto_armor` | Player | Equips the strongest strictly better locally observed armor piece. | `prefer_durability`, `protect_binding_curse`, `delay_ticks`, `minimum_improvement` | It runs only in the player's open vanilla inventory with an empty carried cursor, uses normal pickup clicks, and leaves a protected Binding Curse piece in place. Attribute scoring is a conservative local estimate. |
| `auto_eject` | Player | Drops configured unwanted inventory items through normal vanilla clicks. | `blacklist`, `drop_stack`, `delay_ticks`, `protected_slots` | It runs only in the player's open vanilla inventory, never selects configured protected inventory slots, and cannot drop an item without Minecraft processing the normal throw click. |
| `auto_fish` | Player | Reels an observed local bobber bite and recasts the selected rod. | `reel_delay_ticks`, `recast_delay_ticks`, `minimum_durability`, `open_water_only` | It is inactive in screens, only uses the currently selected player-provided fishing rod, waits for a local hook bite, and stops at the durability reserve. Hook/open-water observations are client-side and server fishing rules remain authoritative. |
| `auto_reconnect` | Player | Offers bounded automatic retries after a multiplayer disconnect. | `countdown_seconds`, `maximum_attempts` | It shows a local Cancel button, uses Minecraft's normal connection screen for the last valid multiplayer target, and declines local targets or a disconnect that does not show the ordinary disconnect screen. It does not reconnect after an explicit user leave. |
| `auto_totem` | Player | Swaps an existing inventory totem into the offhand at local safety thresholds. | `health_threshold`, `fall_threshold`, `restore_previous_offhand`, `delay_ticks` | It runs only in the player's open vanilla inventory with an empty carried cursor. It restores a previous item only when that exact item remains in the recorded source slot, or restores an originally empty offhand only while that source is still empty. |
| `inventory_manager` | Player | Conservatively organizes existing inventory contents with normal clicks. | `sort_inventory`, `drop_junk`, `junk_items`, `preferred_hotbar`, `preserve_named`, `preserve_enchanted`, `minimum_durability`, `delay_ticks` | It runs only in the player's open vanilla inventory. The initial sorter makes one deterministic adjacent item-ID improvement at a time; protected named/enchanted/low-durability items are not dropped. |

## World modules

| ID | Category | Description | Settings | Limitation |
| --- | --- | --- | --- | --- |
| `fast_place` | World | Lowers the ordinary local item-use cooldown while Use is held. | `use_delay`, `item_filter` (`all`, `blocks`, `non_blocks`), `safe_minimum_delay` | It only lowers a cooldown Minecraft has already created for a non-empty held item, and restores an unchanged module-owned cooldown immediately on disable or panic. It never generates uses, clicks, or packets; the server still controls all interaction rate limits. |
| `fast_break` | World | Lowers an existing normal local block-destroy cooldown. | `break_delay`, `blocks` | It acts only while Attack is held over a loaded visible non-air target. The optional semicolon-delimited local filter is validated; it does not start or repeat block destruction by itself, fabricate a packet, or override server mining rules. |
| `nuker` | World | Makes a bounded set of ordinary destroy requests for filtered loaded blocks. | `radius`, `blocks_per_tick`, `all_blocks`, `whitelist`, `blacklist`, `tool_selection`, `minimum_tool_durability`, `line_of_sight`, `rotate`, `safety_limit` | It is disabled by default, requires Attack and no screen, and treats a blank whitelist as all non-air blocks. The `all_blocks` checkbox targets every non-air block regardless of the whitelist (which it hides while on); otherwise a nonblank whitelist restricts targets. The blacklist always excludes matches. Creative mode caps requests at two per tick; survival mode continues one ordinary target at a time so mining progress is retained. With line of sight on, it ray-checks only the 32 nearest eligible blocks per tick. It uses Minecraft's normal destroy path and optional existing-hotbar selection; the server can reject attempts or correct client prediction. |
| `anti_cactus` | World | Slides normal local self-movement away from nearby loaded cactus collision boxes. | None | It is off by default, checks at most 64 local blocks per bounded move, and leaves world-driven, spectator, passenger, large, and unknown-chunk movement alone. It never prevents cactus damage or changes server movement authority. |
| `chest_steal` | World | Transfers eligible chest items through normal quick-move clicks. | `delay_ticks`, `whitelist`, `blacklist`, `priority` (`highest_value`, `lowest_slot`), `close_after_completion` | It runs only in an open vanilla chest with an empty carried cursor. Minecraft/server inventory rules decide each quick move; the module can close only after its locally eligible list is exhausted. |
| `builder_assist` | World | Previews and places small local line, floor, and wall plans with held blocks. | `mode` (`single`, `horizontal_line`, `vertical_line`, `floor`, `wall`), `length`, `width`, `height`, `repeat_placement`, `placement_delay_ticks`, `preview_color`, `preview_fill_color` | It previews only loaded replaceable positions while a player-provided block is selected. It acts only while Use is held and vanilla's use cooldown is clear, then asks Minecraft for one ordinary block interaction; reach, placement, and server rules remain authoritative. |
| `block_selection` | World | Highlights Minecraft's current visible block target locally. | `outline_color`, `fill`, `line_width`, `distance_label` | It is off by default and renders at most one loaded target box. Fill uses a fixed translucent alpha derived from the outline color; the optional label is a bounded local eye-to-block distance. It never changes a target, block, reach, or packet. |

## Chat modules

| ID | Category | Description | Settings | Limitation |
| --- | --- | --- | --- | --- |
| `chat_prefix` | Chat | Adds configured text before safe ordinary outgoing chat. | `prefix`, `separator`, `exclude_commands`, `exclude_private_messages` | Helikon commands, slash commands, likely authentication commands, and private-message commands are preserved verbatim. Formatting is declined if it would exceed the vanilla normal-chat limit. |
| `chat_suffix` | Chat | Appends configured text to safe ordinary outgoing chat. | `suffix`, `separator`, `per_server_suffixes`, `random_suffixes`, `exclude_commands`, `exclude_private_messages` | A normalized local `server=suffix` entry takes priority; otherwise one random-list entry is chosen locally or the fallback suffix is used. Protected input and over-limit messages are preserved verbatim. |
| `chat_mute` | Chat | Locally hides selected incoming chat or game-message categories. | `global_chat`, `system_messages`, `death_messages`, `advancement_messages`, `join_leave_messages`, `command_feedback`, `repeated_messages`, `custom_text_filters` | It uses top-level vanilla translation keys where available and otherwise only local text. Hiding affects this client HUD only. |
| `chat_filter` | Chat | Matches bounded incoming keyword, player, or regex rules locally. | `keyword_filters`, `regex_filters`, `player_filters`, `case_sensitive`, `hide_matches`, `highlight_matches`, `sound_matches`, `hud_notifications` | Regex input is locally bounded and rejects lookarounds, backreferences, and all quantified groups to prevent catastrophic backtracking. A match can hide, highlight, sound, or post local feedback; a server-associated local profile supplies per-server preferences. The planned desktop notification is intentionally replaced by local HUD feedback so no OS notification integration is required. |
| `chat_spammer` | Chat | Sends configured ordinary chat at a conservative local interval. | `messages`, `delay_ticks`, `random_order`, `pause_in_gui`, `counter`, `timestamps`, `session_message_cap` | It rejects local/slash command forms, enforces a minimum 40-tick delay even across toggles, pauses in screens by default, stops after disconnect or three observed local cancellations, and caps messages per client session. The optional counter appends this session's message number and the optional timestamp appends the local `HH:mm:ss` send time; decorated messages are truncated to the ordinary 256-character chat limit. Servers can still reject or punish spam. |
| `private_message_helper` | Chat | Sends validated configured private-message commands and recognizes common inbound PM forms locally. | `message_command`, `reply_command`, `recent_limit`, `notifications`, `sound`, `highlight` | It recognizes only `From <name>:`, `<name> whispers to you:`, and `<name> -> you:` forms, retains a bounded non-persistent incoming/outgoing history, and can locally highlight, sound, and notify. Standard vanilla names suggest the configured normal PM command locally. Recent conversations are a bounded per-name history list; a separate tabbed conversation UI is not implemented. |
| `mention_notifier` | Chat | Provides local feedback for local-name or configured-term mentions. | `mention_terms`, `include_own_name`, `case_sensitive`, `cooldown_seconds`, `sound`, `hud_notification`, `highlight` | It checks ordinary player chat only, ignores messages attributed to the local player, applies a local cooldown, and can independently highlight, sound, or show local feedback. Taskbar and desktop notifications remain optional platform features. |
| `auto_reply` | Chat | Sends one safe configured ordinary-chat reply for a matching incoming player message. | `trigger`, `match_mode` (`exact`, `contains`, `regex`), `reply`, `cooldown_seconds`, `whitelist`, `blacklist`, `server_restriction`, `replies_per_minute`, `pause_in_gui` | It is off by default, accepts only a bounded safe regex, ignores self/invalid senders and recent local reply text, caps responses to ten per minute, and does not send commands, packets, or retries. This initial slice supports one rule and no multi-rule editor. |
| `anti_spam` | Chat | Locally suppresses duplicate, rapid, or burst join/leave incoming messages. | `stack_duplicates`, `hide_repeats`, `repeat_window_seconds`, `rapid_message_limit`, `rapid_window_seconds`, `collapse_join_leave`, `whitelisted_message_types` | It tracks at most 512 message identities and 256 sender windows in memory, and only affects this client HUD. With stacking enabled, it directly collapses consecutive visible duplicates with a bounded local counter; BetterChat takes precedence when enabled. |
| `chat_timestamps` | Chat | Prepends a locally rendered time label to incoming player and server-system chat. | `twenty_four_hour`, `include_seconds`, `brackets`, `color`, `relative_mode` | It preserves the original message component and sends nothing. Relative mode reports elapsed client-session time at insertion; it does not update old line labels as they age. Client-only Helikon feedback is not timestamped. |
| `chat_color` | Chat | Applies a local palette to displayed incoming chat. | `normal_message_color`, `player_name_color`, `timestamp_color`, `mention_color`, `system_message_color`, `private_message_color`, `background_opacity`, `text_shadow` | It rebuilds only local stored display components after Minecraft logs the original. Mention and private-message detection is text-based and conservative; `player_name_color` applies to vanilla's structured `chat.type.text` format, while custom server formats keep their own name styling. Foreground alpha is retained in configuration but Minecraft's text components use RGB. |
| `better_chat` | Chat | Expands and locally improves the Minecraft chat display. | `history_limit`, `clickable_names`, `stack_duplicates`, `message_counters`, `visibility_seconds`, `fade_seconds`, `compact_mode`, `smooth_scroll` | All effects are client-display-only and off by default. Duplicate stacking applies only to immediately consecutive identical lines and is capped at `[x9999]`. Clickable names require vanilla `chat.type.text`, preserve an existing server click action, and only suggest `/msg <name>`; custom server formats are never flattened. `.chat` history/search/copy is session-memory-only. |
| `chat_history` | Chat | Retains a bounded local chat record with search, copy, player-name copy, and sent-draft reopen commands. | `history_limit`, `persistent_logging`, `retention_days` | It is off by default. Persistence is also disabled by default; when explicitly enabled, accepted non-overlay incoming lines and ordinary sent chat are saved only at lifecycle boundaries in bounded schema-versioned per-server local files. `.` commands are never recorded or sent. |
| `announcer` | Chat | Sends a bounded ordinary chat line for individually selected local gameplay moments. | `death`, `kill`, `item_pickup`, `distance_traveled`, `block_mined`, `dimension_change`, `join`, `leave`, `advancement`, `low_health`, `totem_use`, `distance_blocks`, `low_health_threshold`, `minimum_delay_seconds`, `session_message_cap`, `pause_in_gui`, `message_template` | All triggers are disabled by default. It uses only normal client chat with a 1–300 second local cooldown and 1–100 session cap; slash/local-command templates are rejected. Kills require a locally observed direct melee attempt followed by a dead entity unload. Leave is local Helikon feedback because it occurs after the normal server connection is closed. |
| `local_translator` | Chat | Displays an additional offline translation for visible incoming chat. | `glossary` | It is off by default and has no HTTP/API mode. `glossary` accepts up to 64 exact `source=translation` entries. Original chat remains unchanged. |

## Miscellaneous modules

| ID | Category | Description | Settings | Limitation |
| --- | --- | --- | --- | --- |
| `twerk` | Miscellaneous | Alternates local sneak input. | `half_cycle_ticks` | It only changes a fresh local input snapshot, never runs with a screen open, and releases immediately when disabled or panicked. Physical sneak input is preserved. |
| `annoy` | Miscellaneous | Makes sparse ordinary main-hand swings. | `interval_ticks` | Disabled by default; it requires a local player and no screen, waits 20–600 ticks between swings, never attacks/targets/chats, and uses only Minecraft's ordinary swing path. A server can observe or reject the normal swing. |
| `one_click_friends` | Miscellaneous | Enables the local middle-click friend gesture. | none | It records an edge before applying the gate, so enabling it while the button is held cannot change a friend. It never consumes or changes normal middle-click behavior. |
| `skin_blinker` | Miscellaneous | Alternates the local player skin-layer options. | `half_cycle_ticks` | It changes no saved or broadcast option, pauses for screens, and restores layer values it still owns on disable, panic, or world exit. |
| `debug_overlay` | Miscellaneous | Displays opt-in local diagnostic timing and state. | `page` | It records module timing only while enabled, shows 10 module rows per local page, two bounded render-cache sizes, event subscriber count, and global-save state. No diagnostic is persisted or transmitted. |
| `local_cape` | Miscellaneous | Replaces only the local player's transient cape render texture. | `primary_color`, `accent_color` | Its small pattern is generated in memory from locally saved settings, applies only in this client, and neither loads nor publishes cape assets. Minecraft's solid cape layer ignores the configured alpha. |
| `local_cosmetics` | Miscellaneous | Draws one bounded local aura ring at the local player's feet. | `color`, `radius`, `segments` | It renders 12–48 local Gizmo lines for the local player only. It has no asset download, player lookup, or network path. |
| `inventory_preview` | Miscellaneous | Renders a local read-only grid of storage slots and optional hotbar. | `rows`, `include_hotbar` | It reads only the verified 36 local non-equipment item slots, renders at most 36 cells, and never opens or alters an inventory. |
| `durability_warnings` | Miscellaneous | Displays local low-durability held-item and armor warnings. | `threshold_percent`, `held_item`, `armor` | It checks at most five already-loaded damageable item facts and changes no durability, inventory, or packet. |
| `death_coordinates` | Miscellaneous | Retains one local death position for the session. | none | It records only while enabled, uses the last safe local waypoint location, is visible only in the same server/world scope, makes no waypoint/file, and resets on restart. |
| `logout_coordinates` | Miscellaneous | Retains one local disconnect position for the session. | none | It records only while enabled, uses the last safe local waypoint location, is visible only in the same server/world scope, makes no waypoint/file, and resets on restart. |

Every production module will document its stable ID, category, settings,
limitations, acceptance criteria, and automated or manual test coverage here.

Module IDs are lowercase and stable; display names are not used as identifiers.

## ClickGUI

Modules appear in the ClickGUI (Right Shift) under their `ModuleCategory`. The
**Active** row above the categories lists enabled modules from every category;
its row checkbox turns a utility off without leaving the view.
Selecting a module shows its name, category, ID, description, an enabled
toggle, and controls for its `BooleanSetting`, `NumberSetting`, `ColorSetting`,
and finite `EnumSetting` values. Colors use strict `#AARRGGBB` tokens; click
an enum row to cycle its documented choices. Integer, string-list, selector,
multi-select enum, range, regex, and keybind settings use compact validated
text fields. Lists/selectors use `;`, multi-enums use `,`, ranges use
`minimum..maximum`, and keybind values use
`keyboard|mouse:code:toggle|hold|press_once[:shift+control+alt+super]`.
Invalid text is red and leaves the last valid stored value unchanged.
Toggles are dispatched through `ModuleRegistry`, so a module that throws
during `onEnable`/`onDisable` is isolated and force-disabled instead of
crashing the client. Setting edits and enabled state persist to
`config/helikon/global.json` when the screen closes.

The settings panel also has **Reset module** plus small **R** buttons for each
visible setting. Reset operations use the existing validated setting defaults.
When a module has more controls than fit in the panel, hover the right-hand
panel and use the mouse wheel; controls, editable fields, and the scrollbar
move together inside the clipped viewport.
Its **Bind** row captures a keyboard key or mouse button locally with any held
Shift, Control, Alt, or Super modifiers; Escape cancels capture,
Backspace/Delete clears the bind, and the key that opens the Helikon GUI is
rejected. Existing activation mode (`toggle`, `hold`, or `press_once`) is
preserved when rebinding, and a local warning identifies exact module-bind
collisions. Drag an unused portion of the header to move the
window, or its bottom-right handle to resize it. The clamped top-left position
and custom dimensions persist in `global.json`.

Select **Theme** from the ClickGUI header to open its local palette selector.
The initial themes are Midnight, High Contrast, and Ocean; selecting one
changes the panel immediately and persists it locally. Color settings also
show an in-panel ARGB picker below their validated text value; drag any of the
four channel tracks to update that one local color channel.

When no text field is focused, the ClickGUI supports keyboard navigation:
Left/Right changes the category (and exits search), Up/Down moves the selected
module row with wrapping and automatic list scrolling, and Enter or Space
toggles the selected module through `ModuleRegistry`.

## Commands and keybinds

Modules can also be controlled through local chat commands (`.toggle`,
`.setting`, `.reset`, `.bind`, `.unbind`, `.profile` — see the README) and
per-module keybinds with `toggle`, `hold`, or `press_once` activation. Profile
commands save/load/duplicate/rename the complete local module and ClickGUI
snapshot without transmitting it. The default profile is applied at startup,
and a matching local server/world association overrides it on join. Import/export uses Helikon's local
`imports/` and `exports/` folders rather than arbitrary paths. A profile can
also be marked as the persisted default. Keybinds never fire while a screen is
open. Server and singleplayer associations are also local profile preferences;
they neither inspect nor send server data beyond the user-provided identifier.

## Friends

Friends are local player-name entries, managed through `.friend list`,
`.friend add`, `.friend remove`, and `.friend color`. Enable
**OneClickFriends**, then middle-click a targeted player in the game world to
add or remove that name without opening chat.
Each entry stores a local ARGB render color in `friends.json`; no friend data is
sent to a server. Targeting modules will use the friend list for their default
friend-exclusion policy when those modules are introduced.

## Waypoints

`.waypoint add <name>` saves the current block position; supplying `x y z`
saves manual coordinates in the currently loaded server/world and dimension.
`.waypoint list` orders enabled local entries by distance and reports a compass
direction. Waypoints can be removed, renamed, toggled, recolored, or given a
small optional icon token with local commands. The minimal Waypoints HUD shows
up to three nearest enabled entries with distance and direction, and hides
entries from any other server/world or dimension.

Death and logout coordinates are now separate enabled-only session snapshots;
they deliberately do not create automatic waypoints. The first HUD indicator
has no separate HUD-editor position or world-space beacon; those rendering and
layout controls remain future work.

## Macros

Macros are configured through `.macro`. Create a global or currently connected
server-scoped macro, then append explicit `local`, `chat`, `command`, or
`delay` actions. A local action must start with `.` and stays inside the local
dispatcher; chat text cannot start with `.` or `/`; Minecraft command text
omits `/`; delay actions are bounded to 1-12,000 ticks. Run one macro at a time
with `.macro run <name>` and stop it with `.macro stop`. The runner pauses while
a screen is open, executes no more than one action per client tick, and stops a
server-scoped macro if the current server changes.

Macros are not a scripting engine: they cannot execute code, read files, or
download content. They do not bypass server authority, rate limits, or normal
chat/command packet formats.

## Panic

`.panic` disables every enabled module through `ModuleRegistry`, so current and
future modules run their own `onDisable` restoration (for example brightness,
timer, FOV, or step changes). It closes any Helikon GUI screen, stops queued
macro work, and hides all custom HUD for the current session without deleting
or changing user configuration. Use `.panic restorehud` to show HUD again
without re-enabling modules. `.panic bind <key>` stores a separate local
keyboard/mouse panic bind; chat and ordinary screens suppress it, while Helikon
screens permit it as an emergency close control.

## HUD

| Element | Behavior | Persistence | Limitation | Coverage |
| --- | --- | --- | --- | --- |
| Active Modules | Lists enabled Helikon modules with registry, alphabetical, or width sorting. | `hud.json`: enabled state, position, scale, padding, backdrop/shadow, sort/alignment/color modes. | Its rainbow color mode gives each active-module line a separate animated hue. It is a local display only. | `ActiveModulesTest`, `HudEditorStateTest`, `HudConfigurationManagerTest`, manual HUD checklist. |
| Other custom HUD | Lets the editor toggle, position, and style every registered non-Active-Modules HUD renderer. | `hud.json`: enabled state, anchor/offset, scale, alignment, background, padding, shadow, ARGB color, and rainbow mode. | The preview uses a styled local label rather than live game data. | `HudElementPlacementTest`, `HudConfigurationManagerTest`, manual HUD checklist. |
| Plan telemetry | Opt-in direction, FPS, Ping, local TPS estimate, Speed, armor/held durability, effects, clock, biome, server address, and totem-count readouts. | `hud.json`: independent placement and presentation preferences for every readout. | TPS reflects observed local client tick cadence, not a server-reported TPS; all values use already-loaded client state. | `ClientTpsEstimateTest`, `TelemetryTextTest`, manual HUD checklist. |

The HUD editor is opened through the **HUD** button in the ClickGUI header. It
is a drag-only canvas: it shows the Active Modules preview even when no module
is enabled plus a handle for every enabled element, and clamps/snaps dragging
so each complete scaled preview remains on screen. Its **HUD settings** button
opens a separate screen with the listed Active Modules controls and the HUD
element selector for toggling and styling every registered local HUD element.
Both screens are client-only and send no network traffic.
