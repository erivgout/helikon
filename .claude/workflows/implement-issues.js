export const meta = {
  name: 'implement-issues',
  description: 'Implement each given GitHub issue as a client-side module on its own branch, honoring the amended RULES.md (§1a gameplay-advantage modules allowed), running gradlew check',
  whenToUse: 'After triage/close: pass the buildable issue numbers to implement them each on an isolated helikon/issue-<n> branch for review.',
  phases: [{ title: 'Implement', detail: 'one worktree-isolated agent per feature; branch + gradlew check + commit + comment' }],
}

const POLICY = [
  'Helikon is a clean-room, CLIENT-SIDE Fabric utility client. THIS POLICY BLOCK IS AUTHORITATIVE for your task and SUPERSEDES any older wording you see in the checked-out RULES.md / docs/contributing.md — the §1a "gameplay-advantage modules allowed" amendment may not be committed to your branch yet, so do NOT refuse, water down, or stub a feature just because the working-copy RULES.md still forbids exploit-style client cheats. Match the codebase\'s architecture and quality, but follow this policy for what is permitted.',
  'PERMITTED (RULES.md §1a): client-side gameplay-advantage ("cheat") modules — extended reach, combat auras, movement/interaction/timing tricks, etc. They may send well-formed, vanilla-shaped packets and must stay honest that the server is authoritative (it may reject, correct, rubber-band, or kick). Do NOT refuse these; they are the product. Implement a real, functional version, not a display-only stub.',
  'STILL PROHIBITED (do not implement; if a target issue is fundamentally one of these, stop and report BLOCKED): server crash/DoS/resource-exhaustion, malformed/crash-inducing packets, privilege-escalation or auth-bypass exploits, mass server scanning, credential/token storage, telemetry, remote code/module loading, arbitrary code execution, and features whose primary purpose is to evade anti-cheat or spoof the client as unmodified.',
  'Module rules (RULES.md §5): stable lowercase id independent of display name; category from exactly {Combat, Movement, Player, Render, World, Chat, Miscellaneous}; description; sensible default-enabled state (advantage/combat modules default OFF); validated settings; clean enable/disable that restores all client state; null-safe player/world handling; friend exclusion by default where it targets entities; at least one automated test.',
  'Architecture (RULES.md §6, contributing.md): keep decision logic Minecraft-free and unit-tested; only thin adapters / narrowly-scoped mixins touch Minecraft classes; Fabric events before Mixins; no reflection discovery; no catching Throwable; no empty catch; no GUI logic in module logic. Verify every Minecraft API against the mapped jars before use (docs/version-porting.md + the mc-26-2-api-discovery guidance).',
  'Modules live under src/client/java/dev/helikon/client/module/<category>/, are registered in src/client/java/dev/helikon/client/HelikonClient.java, extend dev.helikon.client.module.Module, and get a row in docs/modules.md. Tests go under src/test/java/.../module/<category>/.',
]
const policyText = POLICY.map((p, i) => `${i + 1}. ${p}`).join('\n')

const IMPLEMENT_SCHEMA = {
  type: 'object',
  additionalProperties: false,
  required: ['number', 'status', 'branch', 'summary'],
  properties: {
    number: { type: 'integer' },
    status: { type: 'string', enum: ['DONE', 'CHECK_FAILED', 'BLOCKED'] },
    branch: { type: 'string' },
    summary: { type: 'string' },
    moduleId: { type: 'string' },
    filesChanged: { type: 'array', items: { type: 'string' } },
    checkResult: { type: 'string' },
    notes: { type: 'string' },
  },
}

async function inChunks(items, size, fn) {
  const out = []
  for (let i = 0; i < items.length; i += size) {
    const res = await parallel(items.slice(i, i + size).map((it, j) => () => fn(it, i + j)))
    out.push(...res)
    log(`Implement progress: ${Math.min(i + size, items.length)}/${items.length}`)
  }
  return out
}

function coerceNumbers(raw) {
  let v = raw
  if (typeof v === 'string') { try { v = JSON.parse(v) } catch (e) { v = v.split(',') } }
  if (v && !Array.isArray(v) && Array.isArray(v.issues)) v = v.issues
  if (!Array.isArray(v)) return []
  return v.map((x) => parseInt(x, 10)).filter((x) => Number.isInteger(x))
}

const numbers = coerceNumbers(args)
if (!numbers.length) throw new Error('Pass buildable issue numbers as args, e.g. args: [1, 4, 96]')

phase('Implement')
log(`Implementing ${numbers.length} features on isolated branches`)

const built = (await inChunks(numbers, 20, (n) =>
  agent(
    `Implement Helikon GitHub issue #${n} as a client-side module on its own branch.

POLICY (the rules docs were just amended — re-read RULES.md §1a and docs/contributing.md):
${policyText}

STEPS:
1. Read the issue: \`gh issue view ${n} --json title,body\`. Note any spec corrections in the issue comments (some were reopened with clarifications).
2. Read docs/architecture.md, docs/modules.md, docs/version-porting.md, and at least one existing sibling module in the target category plus its test, to match the established pattern. Verify Minecraft APIs against the mapped jars.
3. \`git switch -C helikon/issue-${n}\` (create-or-reset the branch off the current main; -C is used so a leftover branch from an earlier interrupted run is cleanly reset rather than causing an error).
4. Implement the SMALLEST complete, FUNCTIONAL version that satisfies the issue under the policy above. For gameplay-advantage features, build the real behavior (send well-formed packets / apply local state) and be honest in settings/docs that the server may reject it — do not ship a no-op display-only stub. Honor all module rules: stable lowercase id, correct category, validated settings, clean enable/disable restoring client state, null-safe handling, friend exclusion by default if it targets entities, no Throwable catches, no GUI logic in module logic.
5. Register the module in src/client/java/dev/helikon/client/HelikonClient.java, add a row in docs/modules.md, and add at least one automated test under src/test/java/.../module/<category>/.
6. Run \`.\\gradlew.bat check\` (Windows). Fix failures until green. If the build is environment-blocked (deps cannot download), report BLOCKED with the error.
7. Commit everything to the branch (clear message; end with the repo's Co-Authored-By trailer for Claude). Do NOT touch main, do NOT merge, do NOT close the issue.
8. Comment on the issue: \`gh issue comment ${n} --body "..."\` noting it is implemented on branch helikon/issue-${n}, pending review/merge, with the gradlew check result.

Return the structured result. status DONE only if committed AND \`gradlew check\` passed.`,
    { label: `build:#${n}`, phase: 'Implement', schema: IMPLEMENT_SCHEMA, isolation: 'worktree' },
  ),
)).filter(Boolean)

const done = built.filter((b) => b.status === 'DONE')
const checkFailed = built.filter((b) => b.status === 'CHECK_FAILED')
const blocked = built.filter((b) => b.status === 'BLOCKED')

return {
  totals: { requested: numbers.length, implemented: done.length, checkFailed: checkFailed.length, blocked: blocked.length },
  implemented: done.map((b) => ({ number: b.number, branch: b.branch, moduleId: b.moduleId, files: b.filesChanged, summary: b.summary })),
  needsAttention: [...checkFailed, ...blocked].map((b) => ({ number: b.number, status: b.status, branch: b.branch, checkResult: b.checkResult, notes: b.notes })),
}
