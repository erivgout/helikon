export const meta = {
  name: 'close-issues',
  description: 'Triage every open GitHub issue against RULES.md, close ones that already exist or that the rules forbid, and implement the rest each on its own branch',
  whenToUse: 'When you want to sweep all open Helikon issues: auto-close already-implemented and rules-violating ones, and build the admissible remainder on isolated branches for review.',
  phases: [
    { title: 'Triage', detail: 'one read-only agent per issue: exists? rules-admissible? implement?' },
    { title: 'Close', detail: 'comment + close the already-exists and rules-rejected issues' },
    { title: 'Implement', detail: 'one worktree-isolated agent per admissible feature; branch + gradlew check' },
  ],
}

// ---- Shared context handed to every agent so classification and code match the project ----
const RULES = [
  'Helikon is a clean-room, open-source, CLIENT-SIDE Fabric utility mod. RULES.md and docs/contributing.md are authoritative.',
  'ADMISSION GATE (RULES.md §1): a feature is admissible only if it is client-side, needs no Helikon backend/account/server plugin, is honest about server authority, and adds NO malformed/exploitative packets, auth bypasses, remote code loading, telemetry, or hidden behavior.',
  'HARD REJECTS (RULES.md §1.5, §4, §10, contributing.md): server crash / DoS / resource-exhaustion tools; malformed or exploitative packets; authentication bypass or privilege escalation against servers; anti-cheat bypass presets; "identify as unmodified" / detection-evasion spoofing; telemetry; anything needing a Helikon-operated backend, database, account system, or persistent WebSocket; remote/arbitrary code or JAR loading.',
  'Examples that MUST be rejected-and-closed, not implemented: NocomCrash, CrashChest, CrashTag, LogSpammer (DoS/crash); ForceOP, OP-Sign (auth bypass / privilege escalation); VanillaSpoof (detection evasion); YesCheat+ (anti-cheat bypass). If an issue is fundamentally one of these, classify REJECT even if a "safe subset" is imaginable.',
  'Module rules (RULES.md §5): every module needs a stable lowercase id independent of display name, a category from exactly {Combat, Movement, Player, Render, World, Chat, Miscellaneous}, description, default-disabled unless clearly safe, settings with validation, clean enable/disable that restores all client state, null-safe player/world handling, friend exclusion by default where targeting is involved, and at least one automated test.',
  'Architecture (RULES.md §6, contributing.md): keep decision logic Minecraft-free and unit-tested; only thin adapters / narrowly-scoped mixins touch Minecraft classes; Fabric events before Mixins; no reflection discovery; no catching Throwable; no empty catch; no GUI logic in module logic. Verify every Minecraft API against the mapped jars before use (see docs/version-porting.md and the mc-26-2-api-discovery memory).',
  'Modules live under src/client/java/dev/helikon/client/module/<category>/, are registered in src/client/java/dev/helikon/client/HelikonClient.java, extend dev.helikon.client.module.Module, and are documented as a row in docs/modules.md. Tests live under src/test/java/.../module/<category>/.',
]

const TRIAGE_SCHEMA = {
  type: 'object',
  additionalProperties: false,
  required: ['number', 'title', 'classification', 'reason'],
  properties: {
    number: { type: 'integer' },
    title: { type: 'string' },
    classification: { type: 'string', enum: ['EXISTS', 'REJECT', 'IMPLEMENT'] },
    reason: { type: 'string', description: 'One to three sentences justifying the classification.' },
    ruleCitations: { type: 'array', items: { type: 'string' }, description: 'RULES.md sections violated, for REJECT.' },
    evidence: { type: 'string', description: 'For EXISTS: the file(s)/module id proving it already exists.' },
    category: { type: 'string', description: 'For IMPLEMENT: one of Combat, Movement, Player, Render, World, Chat, Miscellaneous.' },
    suggestedId: { type: 'string', description: 'For IMPLEMENT: proposed lowercase module id.' },
  },
}

const IMPLEMENT_SCHEMA = {
  type: 'object',
  additionalProperties: false,
  required: ['number', 'status', 'branch', 'summary'],
  properties: {
    number: { type: 'integer' },
    status: { type: 'string', enum: ['DONE', 'CHECK_FAILED', 'BLOCKED'] },
    branch: { type: 'string' },
    summary: { type: 'string' },
    filesChanged: { type: 'array', items: { type: 'string' } },
    checkResult: { type: 'string', description: 'Outcome of .\\gradlew.bat check (pass/fail + key errors).' },
    notes: { type: 'string' },
  },
}

const CLOSE_SCHEMA = {
  type: 'object',
  additionalProperties: false,
  required: ['closed', 'failed'],
  properties: {
    closed: { type: 'array', items: { type: 'integer' } },
    failed: { type: 'array', items: { type: 'string' } },
  },
}

const rulesText = RULES.map((r, i) => `${i + 1}. ${r}`).join('\n')

// Run heavy Gradle-building agents in small waves so we never have too many
// parallel Minecraft builds thrashing the machine.
async function inChunks(items, size, fn) {
  const out = []
  for (let i = 0; i < items.length; i += size) {
    const chunk = items.slice(i, i + size)
    const res = await parallel(chunk.map((it, j) => () => fn(it, i + j)))
    out.push(...res)
  }
  return out
}

function coerceNumbers(raw) {
  let v = raw
  if (typeof v === 'string') {
    try { v = JSON.parse(v) } catch (e) { v = v.split(',') }
  }
  if (v && !Array.isArray(v) && Array.isArray(v.issues)) v = v.issues
  if (!Array.isArray(v)) return []
  return v.map((x) => parseInt(x, 10)).filter((x) => Number.isInteger(x))
}
const numbers = coerceNumbers(args)
if (!numbers.length) {
  throw new Error('Pass the open issue numbers as args, e.g. args: [58, 59, 60, ...]')
}
log(`Triaging ${numbers.length} open issues against RULES.md`)

// ---------- Phase 1: Triage (read-only, one agent per issue) ----------
phase('Triage')
const triaged = (await parallel(numbers.map((n) => () =>
  agent(
    `You are triaging Helikon GitHub issue #${n} in this repo.

PROJECT RULES:
${rulesText}

STEPS:
1. Read the issue: run \`gh issue view ${n} --json number,title,body\` and read the full body.
2. Decide if the requested feature ALREADY EXISTS in the codebase. Search src/client/java/dev/helikon/client/module/ and docs/modules.md for an equivalent module or capability. Grep by concept, not just by name. If an equivalent already ships, classification = EXISTS and put the proving file path(s) / module id in "evidence".
3. If it does not exist, evaluate it against the ADMISSION GATE and HARD REJECTS. If the feature is fundamentally a server attack, DoS/crash tool, auth bypass, anti-cheat bypass, detection-evasion spoof, telemetry, or requires a Helikon backend, classification = REJECT and list the violated RULES.md sections in "ruleCitations".
4. Otherwise classification = IMPLEMENT. Pick the correct category (Combat, Movement, Player, Render, World, Chat, or Miscellaneous) and a stable lowercase suggestedId.

Do NOT modify any files. Return only the structured result.`,
    { label: `triage:#${n}`, phase: 'Triage', schema: TRIAGE_SCHEMA },
  ),
))).filter(Boolean)

const exists = triaged.filter((t) => t.classification === 'EXISTS')
const rejected = triaged.filter((t) => t.classification === 'REJECT')
const toBuild = triaged.filter((t) => t.classification === 'IMPLEMENT')
log(`Triage done: ${exists.length} already exist, ${rejected.length} rejected by rules, ${toBuild.length} to implement`)

// ---------- Phase 2: Close the exists + rejected issues ----------
phase('Close')
const closeList = [...exists, ...rejected]
let closeResult = { closed: [], failed: [] }
if (closeList.length) {
  // One agent handles all closes sequentially to avoid gh auth races; closing is I/O-cheap.
  const payload = closeList.map((t) => ({
    number: t.number,
    kind: t.classification,
    reason: t.reason,
    evidence: t.evidence || '',
    ruleCitations: t.ruleCitations || [],
  }))
  closeResult = await agent(
    `Close the following Helikon issues on GitHub using the \`gh\` CLI. For each item:

- If kind is EXISTS: post a comment with \`gh issue comment <number> --body "..."\` stating the feature already exists, citing the evidence, then \`gh issue close <number> --reason "not planned"\` (already implemented).
- If kind is REJECT: post a comment explaining it cannot be implemented because it violates Helikon's RULES.md, quoting the reason and the specific rule sections, then \`gh issue close <number> --reason "not planned"\`.

Keep each comment concise, factual, and professional. Do NOT close any issue that is not in this list. Collect the numbers you successfully closed and any failures.

ITEMS (JSON):
${JSON.stringify(payload, null, 2)}`,
    { label: 'close-issues', phase: 'Close', schema: CLOSE_SCHEMA },
  ) || closeResult
  log(`Closed ${closeResult.closed.length} issues (${closeResult.failed.length} failures)`)
}

// ---------- Phase 3: Implement admissible features, one branch each ----------
phase('Implement')
const built = (await inChunks(toBuild, 4, (t) =>
  agent(
    `Implement Helikon issue #${t.number} ("${t.title}") as a new client-side module on its own branch.

PROJECT RULES (must all be honored):
${rulesText}

CONTEXT: proposed category = ${t.category || '(you decide)'}, proposed module id = ${t.suggestedId || '(you decide)'}. Triage reasoning: ${t.reason}

STEPS:
1. Re-read the issue: \`gh issue view ${t.number} --json title,body\`.
2. Read docs/architecture.md, docs/modules.md, docs/version-porting.md, and at least one existing sibling module in the target category (e.g. a comparable file under src/client/java/dev/helikon/client/module/<category>/) plus its test, to match the established pattern exactly. Verify any Minecraft API you use against the mapped jars per docs/version-porting.md.
3. Create and switch to a branch named \`helikon/issue-${t.number}\` (\`git switch -c helikon/issue-${t.number}\`).
4. Implement the SMALLEST complete, cohesive, functional version that satisfies the issue while honoring every rule: stable lowercase id, correct category, validated settings, clean enable/disable that restores all client state, null-safe player/world handling, friend exclusion by default if it targets entities, no Throwable catches, no GUI logic in module logic, decision logic kept Minecraft-free where practical.
5. Register the module in src/client/java/dev/helikon/client/HelikonClient.java, add a documentation row in docs/modules.md, and add at least one automated test under src/test/java/.../module/<category>/.
6. Run the full gate: \`.\\gradlew.bat check\` (this is a Windows PowerShell/Fabric project). Fix failures until it passes. If deps cannot download or the build is environment-blocked, report status BLOCKED with the error.
7. Commit everything to the branch with a clear message (co-authored, matching repo convention). Do NOT switch back to or modify main. Do NOT merge. Do NOT close the issue.
8. Post a brief comment on the issue: \`gh issue comment ${t.number} --body "..."\` noting it is implemented on branch helikon/issue-${t.number}, pending review/merge, with the gradlew check result.

Return the structured result. status DONE only if the code is committed AND \`gradlew check\` passed.`,
    { label: `build:#${t.number}`, phase: 'Implement', schema: IMPLEMENT_SCHEMA, isolation: 'worktree' },
  ),
)).filter(Boolean)

// ---------- Report ----------
const done = built.filter((b) => b.status === 'DONE')
const checkFailed = built.filter((b) => b.status === 'CHECK_FAILED')
const blocked = built.filter((b) => b.status === 'BLOCKED')

return {
  totals: {
    triaged: triaged.length,
    exists: exists.length,
    rejected: rejected.length,
    toImplement: toBuild.length,
    closed: closeResult.closed.length,
    implemented: done.length,
    checkFailed: checkFailed.length,
    blocked: blocked.length,
  },
  closedIssues: closeResult.closed,
  closeFailures: closeResult.failed,
  existsClosed: exists.map((t) => ({ number: t.number, title: t.title, evidence: t.evidence })),
  rejectedClosed: rejected.map((t) => ({ number: t.number, title: t.title, rules: t.ruleCitations, reason: t.reason })),
  implemented: done.map((b) => ({ number: b.number, branch: b.branch, files: b.filesChanged, summary: b.summary })),
  needsAttention: [...checkFailed, ...blocked].map((b) => ({ number: b.number, status: b.status, branch: b.branch, checkResult: b.checkResult, notes: b.notes })),
}
