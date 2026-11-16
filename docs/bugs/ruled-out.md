---
type: lessons_learned
title: "Investigated and ruled out (not Vrapper code bugs)"
description: "Upstream issues and behaviors checked during the audit that are intentional design choices or external Eclipse/OS behaviors."
status: stable
sources:
  - plugins/net.sourceforge.vrapper.eclipse/src/net/sourceforge/vrapper/eclipse/interceptor/SelectionVisualHandler.java
  - plugins/net.sourceforge.vrapper.eclipse/src/net/sourceforge/vrapper/eclipse/interceptor/VimInputInterceptorFactory.java
  - plugins/net.sourceforge.vrapper.eclipse/src/net/sourceforge/vrapper/eclipse/platform/EclipseHistoryService.java
  - plugins/net.sourceforge.vrapper.eclipse/src/net/sourceforge/vrapper/eclipse/ui/CLabel.java
  - plugins/net.sourceforge.vrapper.eclipse/src/net/sourceforge/vrapper/eclipse/ui/ModeContributionItem.java
verified: unverified
tags: [bugs, audit, ruled-out, upstream]
generated:
  by: antigravity
  at: 2026-11-16T09:20:00Z
---

# Investigated and ruled out (not Vrapper code bugs)

Checked during the codebase and upstream issue audit and excluded from the numbered [bug tracker](index.md).

| Concern | Result |
|---|---|
| Upstream [#864](https://github.com/vrapper/vrapper/issues/864) / [#889](https://github.com/vrapper/vrapper/issues/889) startup error: `NoSuchMethodException: ...CLabel.setCSSForeground(Color)` | **External theme plugin conflict.** Thrown by Genuitec Darkest Dark / DevStyle (`com.genuitec.eclipse.theming.ui.css.handler.ExpandableContribution`), which reflectively expects `setCSSForeground` on widgets matching simple class name `CLabel`. Vrapper's [`CLabel`](../../plugins/net.sourceforge.vrapper.eclipse/src/net/sourceforge/vrapper/eclipse/ui/CLabel.java) subclasses `org.eclipse.swt.custom.CLabel` solely to match Eclipse 4 CSS selectors without an `e4` bundle dependency (see [`ModeContributionItem`](../../plugins/net.sourceforge.vrapper.eclipse/src/net/sourceforge/vrapper/eclipse/ui/ModeContributionItem.java#L76-L79)). |
| Upstream [#881](https://github.com/vrapper/vrapper/issues/881): selection from "next problem marker" not entering Visual mode | **Intentional behavior.** [`SelectionVisualHandler`](../../plugins/net.sourceforge.vrapper.eclipse/src/net/sourceforge/vrapper/eclipse/interceptor/SelectionVisualHandler.java#L74-L80) deliberately ignores non-mouse selections when `NormalMode` is active to prevent Eclipse navigation/refactoring actions from unexpectedly forcing Visual mode (upstream tracks an opt-in Eclipse-command listener experiment in [PR #883](https://github.com/vrapper/vrapper/pull/883)). |
| Upstream [#847](https://github.com/vrapper/vrapper/issues/847): macro recording `{` outputs `<A-C-{>` on non-US keyboards | **OS/SWT modifier reporting limitation.** Eclipse reports `AltGr` as `Ctrl+Alt`, and [`VimInputInterceptorFactory`](../../plugins/net.sourceforge.vrapper.eclipse/src/net/sourceforge/vrapper/eclipse/interceptor/VimInputInterceptorFactory.java#L335) explicitly does not synthesize or strip `AltGr` modifiers. |
| Blockwise `lock("block-action")` overwritten or prematurely unlocked by `InsertMode.lock("insertmode")` | **Safe under normal execution.** [`EclipseHistoryService`](../../plugins/net.sourceforge.vrapper.eclipse/src/net/sourceforge/vrapper/eclipse/platform/EclipseHistoryService.java#L31-L42) records `lockName` only when `!locked` and ignores `unlock("insertmode")` while locked under `"block-action"`. (It only breaks when `InsertMode` loses `mOnLeaveHint` via [Bug 2](002.md).) |
