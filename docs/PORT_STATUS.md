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
  Last push 2026-09-12 (`f4a74bb`, docs handoff fixes).
- **Current feature:** none — Auto Anvil COMPLETED (user-confirmed 2026-09-12).
  Awaiting next feature instruction.
- **Feature status:** no active feature. May revisit completed features if issues surface later.
- **Last completed feature:** Auto Anvil (user-confirmed COMPLETED 2026-09-12).
  Prior: Auto Enchantment Table, XP Drain + XP Shower, Tank, Hang Glider (all user-confirmed).
- **Current problem:** none — no open problems, no TEMPORARY code.
  History in `PORTING_LOG.md`; full tracker in `KNOWN_ISSUES.md`.
- **Last successful build:** `OpenBlocks-1.8.9-1.0.0.jar` (463,781 bytes, reobfuscated, stable_22) — BUILD SUCCESSFUL 2026-09-12 (Auto Anvil Phase C, first try, zero compile iterations)
- **Last deployment:** same JAR copied to `1.8.9(6)/minecraft/mods` 2026-09-12 (unrelated mods untouched)
- **Last user test result (2026-09-12):** Auto Enchantment Table COMPLETED ("works exactly
  like 1.12.2" — visuals/tabs identical, settings persist, auto-drink works). Prior: XP
  Drain/Shower COMPLETED + Tank COMPLETED + glider fixes verified.
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
  1. Await user's next feature instruction — no active feature, tree clean, all issues closed.
  2. GitHub (`Wasad12/openblocks-1.8.9`) RECEIVED subtree push `f4a74bb` 2026-09-12
     (all local work through `2375167` now visible). Future pushes need explicit
     request: `git subtree push --prefix OpenBlocks-1.8.9 origin master`.
- **Last known-good Git commit:** `1339263` (Auto Anvil Phase C log; code at `bfa3bb8` matches
  deployed 463,781-byte JAR).
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
