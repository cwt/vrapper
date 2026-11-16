---
type: index
title: "Bug Tracker — vrapper (JDK 25 Fork)"
description: "One file per bug (#1–#13), plus ruled-out concerns, minor notes, and verification history."
status: stable
sources:
  - docs/bugs/
verified: unverified
tags: [bugs, tracker, index, issues]
generated:
  by: antigravity
  at: 2026-11-16T09:20:00Z
---

# Bugs — vrapper (JDK 25 Fork)

Source-level and build/test audit findings for the `vrapper` JDK 25 modernization fork, cross-referenced with open upstream `vrapper/vrapper` issues.

- [Ruled out (not Vrapper code bugs)](ruled-out.md)
- [Minor notes (hardening, build notes, unnumbered)](minor-notes.md)
- [Verification history](verification.md)

## Severity legend

- **HIGH** — defect that breaks builds/tests on JDK 25, corrupts text/undo state, or can crash/hang the Eclipse UI thread.
- **MEDIUM** — correctness defect or resource leak affecting realistic editing flows or config files.
- **LOW** — defensive null-safety hardening issue.

## Summary

| Severity | Open | Fixed | Total |
|---|---:|---:|---:|
| HIGH | 2 | 4 | 6 |
| MEDIUM | 6 | 0 | 6 |
| LOW | 1 | 0 | 1 |
| **Total** | **9** | **4** | **13** |

## All Bugs

| # | Title | Severity | Upstream | Status |
|---|---|---|---|---|
| [1](001.md) | `CommandLineParser` splits `" \| "` command chains on every space | HIGH | — | Fixed |
| [2](002.md) | `InsertMode` wipes `mOnLeaveHint`, `count`, and `repetitionCommand` on `RESUME_ON_MODE_ENTER` | HIGH | — | Fixed |
| [3](003.md) | `s/\%V` substitution replaces outside visual-block columns on multi-line selections | HIGH | [#848](https://github.com/vrapper/vrapper/issues/848) | Fixed |
| [4](004.md) | Pending `imap` buffer (`cursorBeforeMapping`) erases autocompleted or inserted text | HIGH | [#827](https://github.com/vrapper/vrapper/issues/827) / [#889](https://github.com/vrapper/vrapper/issues/889) | Open |
| [5](005.md) | `EclipseCursorAndSelection.getPositionByVisualOffset()` overflow, `/ 0`, and infinite-loop hazards | HIGH | — | Open |
| [6](006.md) | `SubstitutionDefinition` fails on regex-metacharacter delimiters | MEDIUM | — | Open |
| [7](007.md) | Nested `if`/`endif`, `func`/`endfunc`, and `try`/`endtry` fail in `.vrapperrc`/`.vimrc` | MEDIUM | [#878](https://github.com/vrapper/vrapper/issues/878) | Open |
| [8](008.md) | `SelectTextObjectCommand` exits `BlockwiseVisualMode` into `VisualMode` | MEDIUM | [#858](https://github.com/vrapper/vrapper/issues/858) | Open |
| [9](009.md) | `BlockwiseVisualMode` skips shorter lines on change and mishandles empty lines with `$` | MEDIUM | [#663](https://github.com/vrapper/vrapper/issues/663) | Open |
| [10](010.md) | `SubwordMotion` uses stale `fromPosition` on multi-step calls and throws on EOF | MEDIUM | [#854](https://github.com/vrapper/vrapper/issues/854) | Open |
| [11](011.md) | Listener and `WeakHashMap` reference leaks on editor close and `MultiPageEditorPart` | MEDIUM | — | Open |
| [12](012.md) | Missing null checks in `EclipsePlatform` undo wrapper and `CLabel.shortenText` | LOW | [#828](https://github.com/vrapper/vrapper/issues/828) | Open |
| [13](013.md) | Core unit tests fail on JDK 25 due to ByteBuddy `1.14.11` / Mockito `5.10.0` target dependency | HIGH | — | Fixed |
