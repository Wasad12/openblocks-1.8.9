# OpenBlocks 1.12.2 → 1.8.9 — Port Status

> Continuity file. A new AI session must read this file first (§16), then `FEATURES.md`,
> then the recent section of `PORTING_LOG.md`, then recent Git commits.

- **Source version:** OpenBlocks 1.7.4 for Minecraft 1.12.2 (`OpenBlocks-1.12.X/`, Forge 14.23.0.2537, MCP snapshot_20171003)
- **Source lib:** OpenModsLib 0.11.4 for 1.12.2 (`OpenModsLib-1.12.X/`, 540 .java files)
- **Target version:** Minecraft 1.8.9 (ForgeGradle 2.1, Forge 11.15.1.1722, MCP **stable_22**, Java 8)
- **Current branch:** master
- **GitHub:** `https://github.com/Wasad12/openblocks-1.8.9` (public) — receives ONLY the
  `OpenBlocks-1.8.9/` subtree (`git subtree push --prefix OpenBlocks-1.8.9 origin master`),
  docs included (they live at `OpenBlocks-1.8.9/docs/`). Reference trees stay local-only.
  Last push 2026-09-12 (`c8fd7d5`, code + docs, verified: 39 files incl. `docs/`).
- **Current feature:** none (Hang Glider COMPLETED 2026-09-12; awaiting next instruction)
- **Feature status:** Idle — no active feature.
- **Last completed feature:** Hang Glider (user-confirmed COMPLETED 2026-09-12)
- **Current problem:** none — all issues closed, tree clean, no TEMPORARY code.
  History in `PORTING_LOG.md`; full tracker in `KNOWN_ISSUES.md`.
- **Last successful build:** `OpenBlocks-1.8.9-1.0.0.jar` (60,224 bytes, reobfuscated, stable_22) — BUILD SUCCESSFUL 2026-09-12 (unstackable glider)
- **Last deployment:** same JAR copied to `1.8.9(6)/minecraft/mods` 2026-09-12 03:12 (unrelated mods untouched)
- **Last user test result (2026-09-12):** Hang Glider COMPLETED (user-confirmed; incl. GUI parity
  + unstackable glider). No active testing.
- **Known issues:** see `KNOWN_ISSUES.md`
- **Important discoveries:**
  - `OpenBlocks-1.12.X/OpenModsLib/` submodule directory is EMPTY — the lib source of truth is the
    sibling `OpenModsLib-1.12.X/` tree. First feature scaffold must wire the lib accordingly
    (submodule checkout, copy, or Gradle project dependency) and record the choice in ARCHITECTURE.md.
  - OpenBlocks 1.12.2 tree: 372 .java files. OpenModsLib 1.12.2 tree: 540 .java files. (VERIFIED by file count)
  - Java 8 toolchains present: `C:\Program Files\Eclipse Adoptium\jdk-8.0.482.8-hotspot`,
    `jdk-8.0.492.9-hotspot`. Default `java` is Temurin 11 — 1.8.9 ForgeGradle builds MUST run under Java 8.
  - Test instance: `C:\Users\wassi\AppData\Roaming\PrismLauncher\instances\1.8.9(6)\minecraft\mods`
    (contains `1.8.9/` subdir + a few unrelated 1.8.9 mods; do not touch them per §20).
- **Next exact action (in order):**
  1. SESSION HANDOFF — new session starts here: tree clean at `a941c94`, debug audit passed
     (zero markers), GitHub subtree pushed + verified (`d204756`). Read §16 order, then await
     user's next feature instruction (Hang Glider COMPLETED).
- **Last known-good Git commit:** `623e748` (session handoff; code checkpoint `61977ae`, built + deployed, 2026-09-12)
- **Repo hygiene (IMPORTANT):** `OpenBlocks-1.12.X/` + `OpenModsLib-1.12.X/` are flagged
  `assume-unchanged` (`git ls-files -v` shows `h`) because environmental mtime churn made
  `git status` permanently list 1369 phantom-modified reference files (content PROVEN identical via
  `git hash-object` vs `git rev-parse HEAD:`, `git diff` empty). Trees are READ-ONLY sources of truth.
  To verify integrity: pick a file, compare `git hash-object <file>` vs `git rev-parse HEAD:<file>`.
  To lift flags: `git update-index --no-assume-unchanged` (then status noise returns).
  Gradle wrapper MUST run with `-g C:\Users\wassi\.gradle-189` (isolated gradle home) + Java 8
  (`JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-8.0.492.9-hotspot`); offline `--offline` works
  after first download. PowerShell: NEVER pipe paths into `git ... --stdin` (CRLF breaks it with
  `Ignoring path`); pass path args directly in batches. `jar xf` extracts into CWD — check for and
  delete stray dirs afterwards.
