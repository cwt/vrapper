---
type: lessons_learned
title: "Minor notes (build hardening, code smells, and test baseline)"
description: "Non-critical observations from the JDK 25 codebase and Maven/Tycho audit."
status: stable
sources:
  - .mvn/extensions.xml
  - releng/net.sourceforge.vrapper.releng.target/net.sourceforge.vrapper.releng.target.target
  - releng/net.sourceforge.vrapper.releng.target/net.sourceforge.vrapper.releng.target.tpd
  - plugins/net.sourceforge.vrapper.core/src/net/sourceforge/vrapper/utils/AbstractPosition.java
  - plugins/net.sourceforge.vrapper.core/src/net/sourceforge/vrapper/vim/commands/SubstitutionOperation.java
  - plugins/net.sourceforge.vrapper.core/src/net/sourceforge/vrapper/vim/commands/motions/CPreProcessorMove.java
  - tests/net.sourceforge.vrapper.core.tests/src/net/sourceforge/vrapper/core/tests/cases/NormalModeTests.java
verified: unverified
tags: [bugs, build, hardening, minor-notes]
generated:
  by: antigravity
  at: 2026-11-16T09:20:00Z
---

# Minor notes (build hardening, code smells, and test baseline)

Observations from the code and build audit that do not warrant standalone bug numbers alongside the [bug tracker](index.md):

- **Deprecated Tycho extension ([`.mvn/extensions.xml`](../../.mvn/extensions.xml#L5-L10)):** Tycho `5.0.4` logs a warning that `org.eclipse.tycho.extras:tycho-pomless` will be replaced by `org.eclipse.tycho:tycho-build` in future Tycho releases.
- **Target definition `.tpd` drift & offline build failure ([`.target`](../../releng/net.sourceforge.vrapper.releng.target/net.sourceforge.vrapper.releng.target.target#L69-L72), [`.tpd`](../../releng/net.sourceforge.vrapper.releng.target/net.sourceforge.vrapper.releng.target.tpd#L67-L69)):**
  - `net.sourceforge.vrapper.releng.target.tpd` still points `eclipse_pydev` to `12.0.0` while `.target` points to `13.1.0`.
  - Even though PyDev was moved to [`optional/`](../../optional/), leaving `eclipse_pydev` `13.1.0` in `.target` causes `mvn -o` (offline mode) to fail because `www.pydev.org/update_sites/13.1.0/` redirects to GitHub release asset URLs with expiring `release-assets.githubusercontent.com` JWT query strings.
- **Java `equals`/`hashCode` contract violation ([`AbstractPosition.java`](../../plugins/net.sourceforge.vrapper.core/src/net/sourceforge/vrapper/utils/AbstractPosition.java#L11-L21)):** Overrides `equals(Object)` while throwing `UnsupportedOperationException` in `hashCode()` (marked `// FIXME: FindBugs`); any `Position` placed in a `HashSet` or `HashMap` will crash.
- **Single-line `lineReplaceCount` ([`SubstitutionOperation.java`](../../plugins/net.sourceforge.vrapper.core/src/net/sourceforge/vrapper/vim/commands/SubstitutionOperation.java#L42-L79)):** `lineReplaceCount` is never incremented in the `range.getStartLine() == range.getEndLine()` branch; harmless only because `else` prints `" on 1 line"`.
- **`CPreProcessorMove` regex ordering ([`CPreProcessorMove.java`](../../plugins/net.sourceforge.vrapper.core/src/net/sourceforge/vrapper/vim/commands/motions/CPreProcessorMove.java#L20)):** `^\\s*#\\s*(if|ifdef|else|elif|endif)` places `if` before `ifdef` without `\b`, so `#ifdef` and `#ifndef` capture `"if"` (handled equivalently by `IF.equals(word)`).
- **Pre-existing ignored tests ([`NormalModeTests.java`](../../tests/net.sourceforge.vrapper.core.tests/src/net/sourceforge/vrapper/core/tests/cases/NormalModeTests.java#L358-L1215)):** Two unit tests are marked `@Ignore`: `test_diw_whitespace` (line 358, `#474`) and `test_dit_containsOpening` (line 1207).
- **Upstream `#889` / `#827` live-IDE behavior:** Beyond the confirmed `cursorBeforeMapping` defect ([Bug 4](004.md)), JDT method-call argument guessing / `LinkedModeUI` text-erasure reports in `#889` require reproduction inside a live Eclipse JDT instance.
