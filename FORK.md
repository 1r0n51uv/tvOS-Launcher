# Fork notes

This is a fork of [techgenius1/tvOS-Launcher](https://github.com/techgenius1/tvOS-Launcher). The dojo's
`keeper` agent (see `1r0n51uv/ron1n-dojo`, FLOW-013) reads this file every time upstream publishes a
release, merges it here and opens a PR from `sync/vX`. Keep it current: it is what the agent knows
about what we changed.

## What we changed

The list for people is the "Questo fork" section of `README.md`; for the agent, in short:

- **Home**: Sources card (`Sources.DEVICES`, images in `assets/sources/`), seasonal theme (`Themes.java`),
  `HomeDashboard.java` heavily reworked (about 1500 lines): expect conflicts there on almost every sync.
- **Removed on purpose**: Prayer Times and Cricket widgets, non-Western leagues, news countries and UI
  languages (Turkish, Azeri, Hindi, Arabic, Japanese, Chinese, Korean, Hebrew). If upstream changes
  those, keep them removed.
- **Football**: European competitions only, ESPN read one day at a time for 7 days, Napoli as default.
- **Italian**: `I18nIt.java` translates the strings the shared dictionary in `I18n.java` leaves in English
  (`it("English text", "Testo italiano");`, one per line). Composed texts use `%s` formats.
- **Updates point at the fork**: `AppUpdater.GITHUB_API_LATEST` and `GITHUB_FALLBACK_APK` use
  `1r0n51uv/tvOS-Launcher`. Never take upstream's value here.
- **Fork automation**: `.github/workflows/` (`ci.yml`, `release.yml`, `fork-sync.yml`),
  `.github/scripts/fork-sync.sh`, `CHANGELOG.md` (ours, Italian), this file.

## Rules for a sync

1. Branch `sync/v<upstream version>` from `main`, then merge the upstream **tag** (`git merge v<X>`),
   never a squash: the history must keep upstream's commits.
2. On a conflict, keep upstream's code and re-apply our changes above on top; the README's "Questo fork"
   section stays at the top.
3. Every string upstream added or changed in the UI that has no Italian yet gets an `it(...)` line in
   `I18nIt.java`, translated (the app falls back to English for the rest, but we want none left).
4. **Version**: the higher of upstream's and ours; if ours is higher or equal, ours +1 patch. Set it as the
   default in `build.sh`: `--version-code "${VERSION_CODE:-N}" --version-name "${VERSION_NAME:-X.Y.Z}"`,
   with N = X*10000 + Y*100 + Z (2.2.6 -> 20206). The release workflow passes the same values.
5. **Changelog**: a `## [<version>]` section in `CHANGELOG.md`, in Italian: what upstream brought, short.
   The release fails without it.
6. Anything beyond `I18nIt.java` lines, the `build.sh` version line and `CHANGELOG.md` makes the PR
   "not clean": it waits for the owner (`needs-review`). That is the normal case here.
7. Never push to `main`, never merge: `fork-sync.yml` decides, with fixed rules, after the CI is green.
8. Signing: releases are signed with the key in the `TVOS_KEYSTORE_B64` secret (the `debug.keystore` the
   installed app was built with, alias `debug`). Never change it: Android refuses updates signed with
   another key.
