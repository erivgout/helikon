# Helikon — Session Handoff Prompt

Copy everything below this line into a fresh session to continue the project.

---

Continue the Helikon Fabric client project at:

```text
C:\Users\ewhee\OneDrive\Documents\HelikonUtilityMod
```

Helikon is a clean-room, open-source, client-only Fabric utility client for
Minecraft Java Edition 26.2. The complete specification, feature roadmap, and
policies are in `PLAN.md` at the repo root — read it, plus `README.md` and
everything in `docs/`, before writing code.

## Current state (baseline)

Git history (all builds green, 61/61 unit tests passing):

```text
207cdb5 fix: harden keybind validation per codex review findings
592074d feat: add local commands, module keybinds, and chat notifications
aefef8d feat: implement functional ClickGUI foundation
d2234ec docs: add original Helikon build plan as PLAN.md
5240876 feat: bootstrap Helikon Fabric client core
```

Measured against `PLAN.md` §29:

- **Milestone 0 (Bootstrap): done.** Fabric project, Gradle Kotlin DSL,
  client entrypoint, logging, CI build workflow (`.github/workflows/build.yml`),
  README, LICENSE, SECURITY.md, docs/ (architecture, configuration, modules,
  testing, networking, privacy, version-porting).
- **Milestone 1 (Core Systems): done.** `Module`/`ModuleRegistry` with
  failure isolation, `BooleanSetting`/`NumberSetting`, `EventBus`
  (client tick pre/post only so far), `ConfigurationManager` (atomic writes,
  backups, corrupt-file recovery, keybind persistence), Right Shift GUI
  KeyMapping, local `.`-prefix chat commands (`.help .modules .toggle .search
  .setting .reset .bind .unbind .gui .panic`), `KeybindManager`
  (toggle/hold/press_once, suppressed while any screen is open), and
  `ChatNotifier` (local chat feedback + module-failure notices).
- **Milestone 2 (GUI and HUD): roughly half done.** A functional ClickGUI
  exists (category sidebar, scrollable searchable module list, toggle via
  registry, metadata + boolean/number setting editor, save-on-close).
  **Missing:** HUD editor, ActiveModules HUD list, theme editor, and ClickGUI
  extras from §13 (in-GUI keybind assignment, draggable/resizable window,
  reset buttons, color picker, keyboard navigation, saved window positions).
- **Milestones 3–10: not started.** Profiles, friends, waypoints, macros,
  panic keybind (only the `.panic` command exists), all real gameplay modules
  (`FullbrightStub` is intentionally inert), chat utilities, ESP/render tools,
  movement, automation, combat, stabilization. Remaining setting types from
  §8 (enum, color, string, keybind, selectors, …) and most events from §9 are
  also unbuilt.

## What to do

Work through the remaining milestones **in plan order, one small milestone at
a time** (PLAN.md §30: smallest complete feature, tests, docs, summary).
Suggested next steps, in order:

1. Finish Milestone 2: ActiveModules HUD element + minimal HUD editor shell,
   then ClickGUI extras (in-GUI bind assignment, reset buttons, dragging).
2. Milestone 3: profiles, friends, waypoints, macros, panic keybind — all
   local JSON stores under `config/helikon/` following the
   `ConfigurationManager` patterns (schema version, atomic write, backup,
   corrupt-file recovery, validation).
3. Milestone 4 onward: real modules, starting with Fullbright (Phase B in
   PLAN.md §18) — it must restore original brightness on disable.

## Non-negotiable workflow rules

1. **One commit per milestone.** Before each commit: build + tests green,
   then run `codex review --uncommitted` (Codex CLI, defaults to
   gpt-5.6-sol), fix real findings, re-review until clean, then commit.
   End commit messages with
   `Co-Authored-By: <your model name> <noreply@anthropic.com>` or equivalent.
2. **Never write Minecraft 26.2 API calls from memory.** The 26.2 mappings
   differ from pre-2026 training data. Verify every class/method with `javap`
   against the deobfuscated jars in
   `~\.gradle\caches\fabric-loom\minecraftMaven\net\minecraft\minecraft-clientonly-deobf\26.2\`
   and `minecraft-common-deobf\26.2\`. Use `javap -c` (bytecode) when
   parameter conventions are ambiguous. Facts already confirmed:
   - Screens render via `extractRenderState(GuiGraphicsExtractor, mouseX,
     mouseY, delta)`; the framework draws `extractBackground` first, then
     widgets — draw custom fills at the start of an `extractRenderState`
     override, then call `super` so widgets land on top.
   - Input arrives as objects: `keyPressed(KeyEvent)`,
     `mouseClicked(MouseButtonEvent, boolean)`, `charTyped(CharacterEvent)`;
     `mouseScrolled(x, y, scrollX, scrollY)` keeps doubles.
   - The current screen lives on `Minecraft.gui`: `client.gui.screen()` /
     `setScreen`; open screens with `client.setScreenAndShow(...)`.
   - `GuiGraphicsExtractor.outline(x, y, width, height, color)`;
     `enableScissor(minX, minY, maxX, maxY)`; `fill`, `text`, `centeredText`,
     `textWithWordWrap`, `setTooltipForNextFrame` exist.
   - `net.minecraft.resources.Identifier` (not ResourceLocation);
     `Component.literal/translatable`; `MutableComponent.withStyle(ChatFormatting)`.
   - `InputConstants.isKeyDown(Window, int)`, `InputConstants.getKey(String)`
     ("key.keyboard.<name>"), `InputConstants.Type.KEYSYM.getOrCreate(int)`;
     `KeyMapping.matches(InputConstants.Key)` (no public bound-key getter).
   - Fabric: `ClientSendMessageEvents.ALLOW_CHAT` (return false to cancel),
     `ClientTickEvents`, `ClientLifecycleEvents`, `KeyMappingHelper`.
   - Local chat output: `client.player.sendSystemMessage(Component)`
     (null-check player; fall back to the logger).
3. **Build with `.\gradlew.bat build` (PowerShell, Windows).** No JDK 25 is
   installed locally; the foojay-resolver-convention plugin in
   `settings.gradle.kts` auto-provisions the pinned Java 25 toolchain. Run
   client for manual checks: `.\gradlew.bat runClient`.
4. **Architecture invariants** (see `docs/architecture.md`):
   - All module enable/disable goes through `ModuleRegistry` (failure
     isolation). Never call `module.enable()/disable()` directly from UI or
     input code.
   - Keep decision logic in Minecraft-free classes and unit-test it
     (existing examples: `ClickGuiState`, `NumberSettingText`, `ModuleSearch`,
     `CommandDispatcher` + commands via `KeyNameResolver`/`IntPredicate`
     injection, `KeybindManager` via `KeyStateReader`). Only thin wiring
     touches Minecraft.
   - Module keybinds and commands must never fire while a screen is open or
     leak to the server. `.`-prefixed chat is always cancelled locally.
   - Configuration: never write files per frame/tick — save on GUI close and
     client stop; atomic replace + `.bak` + `global.corrupt-<ts>.json`
     recovery; invalid values reset to safe defaults and log.
   - `Keybind` validates key codes against defined GLFW keyboard token ranges;
     `.bind` rejects the GUI key (injected reserved-key predicate).
5. **Constraints:** client-only (no server entrypoint), no external
   HTTP/WebSocket/backend/telemetry, no anti-cheat bypasses or malformed
   packets, no copying proprietary-client code/assets, four-space Java
   indentation, tests for new logic, update README + relevant docs/ pages
   every milestone (docs/testing.md gets manual checklists for UI-only
   behavior).

## Reporting

After each milestone, report: changed files, build/test output, review
findings and how they were addressed, known limitations, and the commit hash.

## Known limitations to inherit (documented, not bugs)

- ClickGUI: settings panel doesn't scroll; invalid number text stays red
  until selection changes (stored value is always the last valid one);
  selected category/module resets each open; no dragging/resizing/themes.
- Commands: no tab completion; `.setting` only handles boolean/number;
  `.bind` is keyboard-only (no mouse buttons/modifier combos).
- In-game behavior so far was verified by API-level checks and unit tests;
  the manual smoke checklists in `docs/testing.md` have not been run in a
  live client.
