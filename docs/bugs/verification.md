---
type: attested_computation
title: "Verification history"
description: "Baseline Maven/Tycho build and unit test verification runs on JDK 25."
status: stable
sources:
  - pom.xml
  - .mvn/extensions.xml
  - releng/net.sourceforge.vrapper.releng.target/net.sourceforge.vrapper.releng.target.target
verified: machine-confirmed
tags: [bugs, verification, testing, jdk25, maven]
generated:
  by: antigravity
  at: 2026-11-16T09:20:00Z
---

# Verification history

Recorded for the [bug tracker](index.md). Re-run `mvn clean verify` after fixing [Bug 13](013.md) or any core/eclipse defects.

## Baseline audit run (`2026-11-16`, JDK `25.0.5.0.3`, Maven `3.10.1`, Tycho `5.0.4`)

1. **`mvn -B -o verify --file pom.xml` (offline mode):**
   - **Result:** FAILED in `2.667 s` during target platform resolution on `[bundle] Vrapper Core Library`.
   - **Cause:** `https://www.pydev.org/update_sites/13.1.0/` redirects to an expiring signed `release-assets.githubusercontent.com` URL that is not resolved in offline mode (see [minor notes](minor-notes.md)).

2. **`mvn -B verify --file pom.xml` (online mode):**
   - **Result:** FAILED in `31.397 s` in `net.sourceforge.vrapper.core.tests` (`net.sourceforge.vrapper.plugin.surround.tests` and `releng` skipped after failure).
   - **Compiled successfully:** All 33 active plugin/feature bundles plus `net.sourceforge.vrapper.testutil` and `net.sourceforge.vrapper.core.tests`.
   - **Test summary (`net.sourceforge.vrapper.core.tests`):** `Tests run: 309, Failures: 17, Errors: 258, Skipped: 2`.
     - **Passing suites (32/32 tests):** `KeyMapTests` (`1/1`), `PatternUtilsTests` (`10/10`), `SimpleKeyStrokeTests` (`4/4`), `StateAndTransitionTests` (`14/14`), `UtilityTests` (`3/3`).
     - **Skipped (2):** Pre-existing `@Ignore` tests in `NormalModeTests` (`test_diw_whitespace`, `test_dit_containsOpening`).
     - **Failing/Erroring suites (17 failures in `RemappingTests`, 258 errors across 13 suites):** All caused in `@Before` (`VimTestCase.initMocks()`) by `ByteBuddy 1.14.11` rejecting Java 25 class file version 69 (filed as [Bug 13](013.md)).

## Post-fix run — Bug 13 & build qualifier (`2026-11-16`, JDK `25.0.5.0.3`, Maven `3.10.1`, Tycho `5.0.4`)

1. **`mvn -B clean verify --file pom.xml`:**
   - **Result:** `BUILD SUCCESS` in `14.896 s` across all 41 reactor modules.
   - **OSGi qualifiers:** `<project.build.outputTimestamp>-</project.build.outputTimestamp>` in [`pom.xml`](../../pom.xml#L8-L13) overrode Maven `3.10.1`'s fixed `1980-02-01` Super POM timestamp; bundles/features/update-site built with `0.75.0.202611160940`.
   - **Test summary (`net.sourceforge.vrapper.core.tests`):** `Tests run: 309, Failures: 0, Errors: 0, Skipped: 2` (only the 2 pre-existing `@Ignore` tests skipped).
   - **Test summary (`net.sourceforge.vrapper.plugin.surround.tests`):** `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`.
