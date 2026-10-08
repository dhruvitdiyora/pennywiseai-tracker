# Cashiro Port Decisions

This file records reviewed decisions for selectively bringing Cashiro ideas into
PennyWise while keeping PennyWise as the foundation. The primary goal is a
cohesive PennyWise UI; Cashiro commits are references, not cherry-pick targets.

**Decision authority (2026-09-12):** The product owner delegated routine
Cashiro-parity and PennyWise-native UI decisions while the work stays within the
reviewed direction. Separate confirmation is reserved for changes to financial
meaning, record/profile ownership, privacy or security, destructive behavior,
external data transfer, data formats, or similarly unusual product behavior.

## 2026-09-07 — Custom subscription cycles

**Decision:** Explicitly approved by the product owner on 2026-09-07.

**Status:** Implemented; automated verification passed on 2026-09-07. On-device
verification is pending because no ADB device was connected at the end of the
implementation pass.

### Approved contract

- Add interval-only custom cycles with a positive count and one of: day, week,
  month, or year.
- Store the value in the existing `billingCycle` field using a strict,
  versioned encoding, with parsing and date advancement centralized in one
  domain utility.
- Support the cycle in both add and edit subscription flows.
- Show a readable cycle label anywhere the billing cycle is displayed.
- Calculate subscription totals using monthly-equivalent amounts for both
  existing fixed cycles and custom cycles.
- Preserve custom cycle values through backup export/import and cover the
  round trip with tests.

### Explicitly excluded

- End dates. Cashiro records an end date but does not consistently enforce it.
- Adding billing-cycle fields to transactions.
- Automatically creating a transaction when a subscription is created.
- Accepting malformed, zero, or negative interval values.

### Why this shape

It adds the useful scheduling behavior without a Room schema change and avoids
silently changing PennyWise's current subscription-creation semantics.

### Implementation review points

- Wire format: `PENNYWISE_CYCLE_V1|<count>|<unit>`.
- The shared codec/date/math implementation lives in
  `domain/model/SubscriptionBillingCycle.kt`.
- Add and edit surfaces use the same custom interval editor.
- Light/dark visual baselines cover the valid and invalid editor states.

## 2026-09-07 — Safe category deletion and reassignment

**Decision:** Explicitly approved by the product owner on 2026-09-07.

**Status:** Implemented; automated verification passed on 2026-09-07. On-device
verification is pending because no ADB device was connected at the end of the
implementation pass.

### Approved contract

- Only custom categories can be deleted.
- Show impact counts before deletion.
- If a category has live references, require the user to explicitly choose a
  replacement category of the same income/expense type. There is no automatic
  fallback category.
- Reassign all live references and delete the category in one Room transaction:
  transactions (including trash), transaction splits, subscriptions, recurring
  templates, merchant mappings, structured rule category references, and
  active budget category rows.
- If the replacement already exists in the same budget, block the operation and
  ask the user to edit that budget first. Do not merge, add, overwrite, or discard
  budget limits implicitly.
- Keep historical budget-month snapshots unchanged.
- Keep `PersonEntity.category` unchanged because it is a contact label, not a
  financial category reference.
- Reuse the app's searchable category picker and current design-system
  components for the reassignment UI.

### Explicitly excluded

- Cashiro's automatic move to `Miscellaneous`.
- Cross-type reassignment between expense and income categories.
- Subcategory migration; PennyWise has no subcategory model.
- Rewriting historical budget snapshots.
- Non-atomic, row-by-row migration.

### Why this shape

Cashiro updates transactions and then deletes the category in separate calls,
and it misses several category-name references. A fresh database avoids legacy
migration concerns, but it does not remove the need for atomic deletion once a
user has real data.

### Implementation review points

- The final impact re-read, rule rewrite, budget-conflict check, reassignment,
  and deletion all run inside one Room transaction.
- Swipe-to-delete keeps the row visible until the user reviews the impact and
  the confirmed operation succeeds.
- The replacement picker receives only same-type categories and excludes the
  category being deleted.
- Light/dark visual baselines cover referenced and unreferenced confirmation
  states.
- Repository tests cover trash, splits, subscriptions, recurring templates,
  mappings, rule JSON, budget conflicts, historical snapshots, cross-type
  rejection, and system-category protection.

## 2026-09-08 — Category list search

**Decision:** Explicitly approved by the product owner on 2026-09-08.

**Status:** Implemented; app compilation and all 409 unit tests passed on
2026-09-08. Light and dark Roborazzi baselines were recorded and visually
reviewed. A focused Compose interaction test passed on a Pixel 9 running
Android 17.

### Reviewed contract

- Add search to the Categories management screen as an ephemeral UI filter.
- Match trimmed category names case-insensitively.
- Keep the existing expense and income sections, showing a section only when it
  contains matches.
- Provide a standard search leading icon and an explicit clear action using the
  current PennyWise design system.
- Show a localized no-results state when the query has no matches.
- Keep edit, delete-impact review, and reassignment behavior unchanged.

### Explicitly excluded

- Cashiro's subcategory-name matching because PennyWise has no subcategory
  model.
- Cashiro's rotating search-placeholder animation.
- Cashiro's Iconsax dependency and bulk icon/category assets.
- The additional All/Expense/Income overflow filter; the existing two-section
  layout already makes the category type visible. This can be reviewed later
  if search alone is insufficient.
- Any database, repository, backup-format, or migration change.

### Expected implementation surface

- `CategoriesViewModel.kt`: query state and deterministic filtering.
- `CategoriesScreen.kt`: token-based search field, clear action, conditional
  sections, and no-results state.
- Localized strings plus focused filter and light/dark UI tests.

### Why this shape

Cashiro's useful behavior is the fast name search. Its implementation is mixed
with subcategories, animated placeholder copy, an additional type filter, and
Iconsax styling that do not belong in this small PennyWise-native UI slice.

### Implementation review points

- The query is ViewModel-owned ephemeral state and survives configuration
  changes without being persisted as a preference.
- Filtering trims the query, ignores case, and preserves repository ordering.
- The management screen filters first, then retains its existing expense and
  income grouping.
- The clear action is accessible by description, and the search IME action
  releases focus.
- No category mutation, picker behavior, schema, backup, or migration path was
  changed by this slice.

## 2026-09-09 — Account selection bottom sheet

**Decision:** Explicitly approved by the product owner on 2026-09-09.

**Status:** Implemented; app compilation and all 414 unit tests passed on
2026-09-09. Three Roborazzi baselines were recorded and visually reviewed after
correcting selected-row contrast. Three focused interaction tests passed on a
Pixel 9 running Android 17.

### Reviewed contract

- Replace the anchored account dropdowns in transaction and subscription entry
  with one shared modal bottom-sheet picker.
- Keep the current PennyWise account-selector cards as the launch affordances.
- Show each account's alias-aware display label, masked account tail where
  applicable, account type, own-currency balance, and selected state.
- Keep accounts grouped in their existing repository order by account type.
- Preserve PennyWise's optional-account behavior: manual/no-account remains
  available for expense, income, and subscription entries, but not for either
  transfer leg.
- Preserve separate `From account` and `To account` targets and the existing
  distinct-account/same-currency transfer validation.
- Keep the sheet open on outside/back dismissal only until Compose completes the
  normal dismissal; selecting an option updates the existing ViewModel state and
  then closes the sheet. No database write occurs in the picker.
- Show a localized empty state while retaining the manual-entry option wherever
  that option is valid.

### Explicitly excluded

- Cashiro's full `AccountCard`, account-merge/edit flows, Iconsax assets, and
  hard-coded dimensions.
- Any account entity, repository, database, backup-format, or migration change.
- Automatic clearing or replacement of the opposite transfer leg.
- Hiding cross-currency accounts or silently changing currencies. The existing
  validation remains authoritative unless a separate behavior change is
  explicitly approved.
- Search, account creation, or account editing inside the picker.

### Expected implementation surface

- New shared `ui/components/AccountSelectionSheet.kt` using Material icons,
  `Dimensions`, `Spacing`, and Material theme shapes.
- `TransactionTabContent.kt`: replace only the shared `From`/`To`/single-account
  dropdown host with the sheet.
- `SubscriptionTabContent.kt`: replace only its funding-account dropdown host
  with the same sheet while preserving the approved custom-cycle work.
- Localized strings plus light/dark UI tests for selected, manual-entry, empty,
  and transfer configurations, and interaction tests for selection and dismissal.

### Why this shape

Cashiro's bottom sheet provides larger touch targets and clearer visual account
context than the current popup menus. Its exact implementation removes optional
account selection from the add flows, uses the large Cashiro account card, and
filters transfer targets differently. The proposed port keeps the useful
interaction and presentation while retaining PennyWise's existing data and
validation semantics.

### Implementation review points

- The shared picker owns no business or persistence state; transaction and
  subscription ViewModels remain the source of truth.
- Selected rows use radio-button selection semantics and the theme's
  `onPrimaryContainer` role so icons, labels, balances, and checks remain
  readable in light and dark themes.
- Account identity is matched by the stable bank-name/account-tail pair rather
  than a balance-row database id that can change when a new balance is recorded.
- Manual entry remains selected and available when no account is linked, while
  both transfer pickers omit that option.
- Device tests cover account selection, manual-entry selection, and system-back
  dismissal.

## 2026-09-09 — Home account-card visual refresh

**Decision:** Explicitly approved by the product owner on 2026-09-09.

**Status:** Implemented. App compilation and the full unit-test gate passed on
2026-09-09. Four Roborazzi baselines (savings light, credit dark, low-balance
light, and multi-account pager) were recorded, visually reviewed, corrected for
dark/warning contrast, and passed fresh-image verification. Two focused device
tests compile and cover hidden-balance/reveal behavior plus nested reveal versus
card navigation; execution is pending because the Pixel disconnected before the
instrumentation run and ADB currently reports no connected device.

### Already present in PennyWise

- Single-account and multi-account pager behavior.
- Alias-aware labels, masked account tails, localized account-type labels, and
  account-detail navigation.
- Balance hidden by default with an accessible reveal control.
- Correct credit-card outstanding semantics, per-account currency formatting,
  low-balance warning treatment, unified-mode handling, and optional blur.
- Existing light savings-card and dark credit-card visual baselines.

### Genuinely missing Cashiro presentation

- A taller identity-card composition with a more prominent balance hierarchy.
- A quiet tiled bank-icon watermark and bottom gradient for foreground contrast.
- A visibly labelled `View details` affordance in addition to whole-card tapping.
- The extra-large corner treatment used by Cashiro's account cards.

### Proposed PennyWise-native contract

- Restyle only `ui/components/cards/AccountCarousel.kt`; keep its public data and
  navigation callbacks unchanged.
- Reuse the existing `TiledIconBackground`, `BrandIcon`, `Dimensions`, `Spacing`,
  Material shapes, and semantic color roles. Add a named height token if a fixed
  card height is approved.
- Recommended layout: a 200dp token-backed identity card; brand icon and account
  type at the top; alias/name and masked account tail in the lower content area;
  balance label, masked/revealed amount, and privacy eye at the bottom-left; and a
  non-competing `View details` label at the bottom-right. The whole card remains
  the navigation target, so the label adds discoverability without a duplicate
  nested navigation button.
- Keep balances hidden by default even though Cashiro reveals them immediately.
- Keep alias, account type, masked number, low-balance, credit-card outstanding,
  wallet, currency, unified-mode, and blur behavior intact.
- Keep the watermark decorative and absent from accessibility semantics; retain
  readable contrast in light, dark, blur, and error-container states.
- Extend visual coverage to low-balance and multi-account pager states and verify
  that the reveal control does not trigger account navigation.

### Explicitly excluded

- Cashiro's shared-element navigation architecture, custom account colors/icons,
  Iconsax assets, account editing/merging, and any entity or database change.
- Cashiro's always-visible balance.
- Reworking Home widget order or any other Home section.

### Approved visual direction

Cashiro's card is a fixed 200dp hero, materially taller than PennyWise's previous
compact card. The product owner explicitly approved that height and the labelled
`View details` affordance before implementation.

## Approved and implemented — Onboarding visual refresh

**Decision:** Approved by the product owner on 2026-09-09 and explicitly
reconfirmed on 2026-09-10.

**Status:** Implemented as a PennyWise-native UI layer over the existing five-step
state machine. No ViewModel, repository, persistence, worker, permission-policy,
or navigation behavior was replaced with Cashiro behavior.

The implementation adds a neutral phone-preview welcome hero, live profile
preview, permission and scan illustrations, account selection/empty states,
localized copy, full-width actions, and reduced-motion support. It deliberately
keeps the exclusions and action matrix documented below.

Verification completed on 2026-09-09: 15 focused Robolectric/Compose tests pass,
including callbacks, validation, account selection, reduced motion, and large
font coverage. Seven visual baselines cover the five stages plus large-font and
empty-account states across light/dark themes. Physical-device keyboard, inset,
and Android permission/back checks remain pending because the configured Pixel
was disconnected during this pass.

### Existing PennyWise flow to preserve

`Welcome → Profile → SMS permissions → SMS scan → Main account`

- Keep the current step order, optional SMS-permission path, WorkManager scan,
  account discovery and selection, profile persistence, main-account currency,
  back/skip behavior, and completion rules.
- Keep the existing privacy wording corrections already present in the dirty
  worktree.

### Cashiro presentation worth adapting

- A device-frame welcome hero instead of a standalone launcher icon.
- Animated bank-message illustrations for the SMS-permission stage.
- A richer scan/sync animation and account-discovery presentation.
- A live profile/avatar preview while choosing name, image, and background.
- Stronger transitions, progress hierarchy, and full-width bottom actions.

### Proposed PennyWise-native contract

- Introduce a neutral `PhonePreviewFrame` rather than Cashiro's iPhone-named
  component and build it only from Compose primitives and existing assets.
- Restyle all five existing stages with `PennyWiseScaffold`, design tokens,
  Material icons, the current avatar assets, and existing account components.
- Keep animations decorative, finite or state-driven, and absent from
  accessibility semantics. Respect the system animator setting so reduced-motion
  users receive a stable presentation.
- Localize the current hard-coded onboarding copy while keeping its meaning and
  permission disclosures intact.
- Split the current large screen into small stage components only where doing so
  leaves ViewModel ownership and navigation behavior unchanged.
- Keep the current action matrix exactly: Get Started; Save & Continue; optional
  permission Skip/Continue; scan Skip/Start Scanning/Continue; and account
  Skip/Finish according to the existing state.
- Keep generic message artwork free of real sender identifiers, personal data,
  or realistic account details.
- Treat `OnBoardingViewModel.kt`, repositories, persistence, the scan worker,
  permission policy, and navigation routes as out of scope for this UI slice.

### Explicitly excluded

- Cashiro's reordered `Welcome → SMS → Notifications → Sync/Account → Profile`
  flow.
- A separate notification-permission onboarding step, manual account creation,
  account merging, shared-element navigation architecture, Iconsax, and bulk
  emoji/icon assets.
- Any database, repository, preference-key, backup-format, scan-worker, or
  permission-policy change.
- Any change to account filtering (non-credit and non-zero balances), automatic
  main-account selection, currency/base-wallet side effects, or the point at
  which profile/account data is persisted.
- Infinite or accessibility-visible decorative motion, banner-image editing,
  realistic message/account examples, or external transmission of onboarding
  data.

### Verification contract

- Light and dark visual baselines for Welcome, Profile, Permissions, scanning,
  scan-complete/account selection, and empty-account completion.
- Semantics and callback tests for step progress, Back, Skip, Continue, scan
  start, profile validation, account selection, and Finish.
- A focused device flow that verifies keyboard/inset behavior and that Android
  permission and back handling still reach the existing callbacks.
- Verify reduced-motion behavior and one large-font configuration in addition
  to the standard light/dark passes.

### Why this is not a direct cherry-pick

Cashiro commit `8e6d0be6` is a roughly 1,300-line UI and behavior rewrite with
hard-coded dimensions and new dependencies. PennyWise already has the intended
five-stage state machine. Porting the visual language stage by stage protects
that foundation while still delivering the first-run experience that makes the
Cashiro version feel more polished.

## Approved and implemented — Home portfolio balance trend

**Decision:** Approved by the product owner on 2026-09-09 and explicitly
reconfirmed on 2026-09-10.

**Status:** Implemented as an expanded-card-only addition while preserving the
collapsed spending-first Home hero and privacy defaults.

The implementation uses a bounded Room query for the 180-day window plus the
latest pre-window seed per account. It aggregates the latest row per account/day,
carries values forward, respects the selected profile and hidden accounts,
excludes credit cards, never sums native mixed currencies, and marks unified
results approximate when rates are unavailable. Insufficient history produces a
truthful empty state. No schema, entity, migration, or backup-format change was
required.

Verification completed on 2026-09-09: 10 focused tests pass (six aggregation,
one bounded-query integration, and three UI/visual tests). Light and dark visual
baselines cover populated and unified-approximate states; the latest dark pass
also verifies readable chart and approximation contrast. A physical-device pass
remains pending while the configured Pixel is disconnected.

### Verified pre-implementation gap

- `HomeViewModel` assigned the same cumulative current-cycle spending
  series to both `spendingHistory` and `balanceHistory`.
- `BalanceCard` rendered only `spendingHistory`; its `balanceHistory` input was
  unused, so Home could not show how the user's account portfolio changed over
  time.
- Room already stores historical account-balance rows and exposes
  `AccountBalanceDao.getAllBalances()`. No schema or migration is required.

### Proposed PennyWise-native contract

- Keep the collapsed Home hero spending-first and keep all balances hidden by
  default.
- Expose existing balance history through `AccountBalanceRepository` and derive
  a Home-only daily portfolio series from the latest row for each account/day.
- Add a `Balance trend` section only to the expanded `BalanceCard`, reusing the
  existing chart primitives and current design tokens.
- Retain the selected-profile filter, stable account identity, wallet handling,
  credit-card semantics, and strict currency tagging. Never sum mixed currencies;
  in unified mode convert only when a rate exists and surface incomplete totals
  as approximate rather than relabelling native values.
- Use a bounded 180-day window and retain a truthful empty state when historical
  rows are insufficient.

### Explicitly excluded

- Replacing the collapsed spending hero with Cashiro's `Net worth` headline.
- Redefining the existing spending comparison or `monthlyChange` as portfolio
  growth.
- Silently subtracting credit-card outstanding amounts from cash-account totals,
  introducing asset/liability models, or revealing balances by default.
- Cashiro's Haze/shared-transition architecture, hard-coded dimensions, Iconsax,
  custom account colors, account editing, or any entity/database change.

### Approval-sensitive choices

- The recommended first slice excludes credit cards from the portfolio line and
  labels it `Account balance trend`, not `Net worth`; liabilities can be reviewed
  separately rather than implied incorrectly.
- The expanded card gains a second chart below the existing spending chart. This
  increases expanded height but leaves collapsed Home density unchanged.

### Verification contract

- Unit-test latest-row-per-account/day aggregation, the 180-day boundary,
  selected-profile filtering, wallet identity, credit-card exclusion, and mixed
  or unconvertible currency behavior.
- Light/dark visual baselines for empty, hidden, revealed, populated, and unified
  approximate states.
- Interaction coverage for expand/collapse and privacy toggling, ensuring nested
  controls do not accidentally collapse the card.

## Implemented — Analytics category and chart-label polish

**Decision:** Proceeded on 2026-09-09 after a read-only Cashiro/current-worktree
comparison because the slice is presentation-only and does not introduce an
unusual behavior or data change.

**Status:** Implemented and visually reviewed.

- Category list/progress rows now include the existing PennyWise category icon
  in a color-matched tonal container. Amount, percentage, progress, and click
  behavior are unchanged.
- The shared `BalanceChart` now accepts an optional localized series label.
  Account Detail retains the localized `Balance trend` default, while Analytics
  truthfully labels the same chart `Spending trend`.
- No ViewModel, filter, aggregation, navigation, schema, dependency, or account
  behavior changed. Cashiro's chart implementation and shared-transition/Haze
  architecture were not ported.

Verification completed on 2026-09-09: focused Roborazzi verification passes.
The category presentation has light and dark/1.3x-font baselines with icon
semantics assertions; the Analytics line-chart baseline verifies the corrected
legend.

## Remaining reviewed candidates — approval gates

The 2026-09-09 comparison found that PennyWise already covers most Cashiro
transaction and Analytics functionality. These remaining changes stay
unimplemented until their behavior is explicitly approved:

- **Unified transaction filter sheet:** visually consolidate the existing
  period, type, category, profile, account, tag, amount, and currency controls.
  This replaces a familiar interaction pattern, so the sheet layout and whether
  filters apply immediately or through an Apply action need product approval.
- **Multi-field batch editing:** edit amount/date/category/note across selected
  transactions. This can affect balances, transfer pairs, loans, and linked
  records and needs a transactional contract before any UI is added.
- **Personal profile presentation:** a richer identity/contacts overview may
  reuse the existing name, avatar, background, and people data. Persistent banner
  media or a second profile concept is explicitly excluded without approval.
- **Notification reminders and language selection:** both look like Settings UI
  but require new background scheduling or locale/resource behavior.
- **Cloud backup, webhooks, and Cashew import:** deferred because they transmit
  data externally or add substantial import/credential/encryption contracts and
  do not fit the current fresh-database, UI-first phase.

## Implemented — Settings hierarchy and selector-label correction

**Decision:** Proceeded on 2026-09-10 after a read-only Settings/design audit.
Both changes are presentation-only and preserve all preferences, callbacks,
rows, routes, and inline data-management behavior.

- The conditional first Settings header now opts out of extra top spacing for
  both Play (`PennyWise Pro`) and F-Droid (`Support development`) builds, matching
  the first-header rule in `docs/design.md`.
- `SettingsDropdownItem` now receives an explicit localized field label. Default
  Currency remains `Currency`; Main Account now correctly says `Account` instead
  of the previous misleading `Currency` label.
- No section reordering, route extraction, preference change, or Cashiro Settings
  split was introduced.

Verification completed on 2026-09-10: focused light/dark semantics and Roborazzi
baselines pass for the Currency and Main Account fields.

## Future reviewed candidate — Personal dashboard identity editor

**Decision:** Awaiting product-owner approval, including the entitlement choice
below. Audited against Cashiro commit `e114502d` on 2026-09-09.

### Correct PennyWise mapping

Cashiro adds a destination named `Profile`, but PennyWise already uses `Profiles`
for Personal/Business/shared transaction and account scopes. The Cashiro personal
page must therefore enhance PennyWise's existing typed `PersonalDashboard`
destination; it must not add a second profile route or repurpose the scope
manager.

### PennyWise-native first slice

- Keep the current Personal dashboard hero, per-currency lend/borrow summary,
  Contacts and Lend & Borrow shortcuts, and active-people list.
- Add a clear Edit identity affordance and a bottom sheet for the existing
  `userName`, `profileImageUri`, and `profileBackgroundColor` preferences only.
- Reuse PennyWise's current preset avatars, `avatar://index` representation,
  background palette or existing color picker, and design tokens.
- Trim names and reject blank values. Copy gallery images into app-private
  storage before saving; never persist a transient picker URI.
- Keep Appearance as the separate owner of theme, accent, AMOLED, blur,
  navigation, cover style, and fonts. Keep the existing Personal dashboard,
  Appearance, and Profiles Settings rows distinct.

### Explicitly excluded

- Cashiro's banner image preference, 4.5 MB default banner, ten additional
  raster avatars, new media/dependency stack, shared transitions, Haze, Iconsax,
  animated verification badge, and hard-coded dimensions.
- Cashiro's `Net Worth`/income/expense overview because its implementation sums
  balances and transactions across currencies. A future financial overview must
  use currency-safe per-currency values or explicit conversion and needs its own
  review.
- Any Room/entity/schema change, second personal-profile concept, or change to
  the Personal/Business scope manager.

### Entitlement approval required

The Home greeting currently sends a free user's avatar tap to the Pro upgrade
and a Pro user's tap to Personal dashboard, while Settings exposes Personal
dashboard to everyone. Onboarding already allows all users to set name, avatar,
and background.

Recommended contract: identity editing remains baseline personalization and is
available to all users from Personal dashboard; the existing Home membership
ring/tap behavior remains unchanged. If identity editing is intended to be Pro
only, that gate must be enforced consistently inside the dashboard as well as on
Home. Do not create an accidental Settings bypass.

### Verification contract

- Light, dark, and large-font baselines for preset-avatar, gallery-image,
  initials/fallback, and editor states.
- Callback tests for open, cancel, save, blank-name validation, image clear,
  avatar selection, gallery persistence, and background color.
- Preference round-trip tests and ViewModel coverage preserving active-person
  sorting, archived exclusion, and per-currency lend/borrow totals.
- Navigation/entitlement coverage for Settings to Personal dashboard and Home's
  existing free-versus-Pro avatar behavior.

## Reviewed candidate — Appearance screen polish

**Review status:** Implemented on 2026-09-13 under delegated routine UI
authority. Automated verification is recorded below; on-device verification is
pending until an ADB device reconnects.

PennyWise already carries the useful Cashiro appearance controls for
system/light/dark mode, dynamic versus branded colour, Rose Pine accents,
AMOLED, blur, normal/floating navigation, and system/SN Pro fonts. PennyWise
also has its own Home cover-style control, which Cashiro does not have and must
be preserved.

### Recommended UI-only slice

- Keep every existing route, preference, callback, default, Android-version
  gate, navigation behaviour, and cover style unchanged.
- Consolidate the repeated theme, navigation, and font option tiles into one
  PennyWise-native choice-tile pattern.
- Replace screen-local dimensions with the existing design tokens, adding only
  narrowly named component tokens where the current system has no equivalent.
- Add radio-button/selected semantics to every mutually exclusive choice.
- Move the visible Appearance labels and accessibility descriptions into string
  resources without changing their English wording.
- Add light, dark, branded/dynamic, large-font, visibility, and interaction
  coverage for the Appearance controls.

### Approval-gated exclusions

- Do not port Cashiro's launcher-logo switcher. It requires new launcher
  aliases, image assets, preference persistence, and backup import/export work;
  it is not a UI-only change.
- Do not add Catppuccin or another accent family without a separate branding
  decision and persisted-enum review.
- Do not expose Cashiro's Hide navigation labels or Hide pill indicator toggles
  without separate approval. Although PennyWise's normal bottom bar can render
  those states, persisting them changes navigation behaviour and backup scope.
- Do not change the current effective default navigation style, theme defaults,
  or Android-version gating.

### Implemented adaptation

- Theme mode, theme style, navigation style, and font choices now share one
  tokenized PennyWise choice-tile component with a consistent selected state.
- Existing preferences, callbacks, defaults, Android-version gates, accent
  palette, AMOLED/blur switches, and PennyWise's cover-style selector were
  preserved without behavioral or persistence changes.
- Choice tiles, accent previews, and cover previews expose radio-button and
  selected-state semantics for accessibility.
- All visible labels and accessibility descriptions touched by the screen use
  string resources, and screen-local measurements were replaced with narrowly
  scoped design tokens.
- Choice subtitles may wrap to two lines so 200% font scale does not clip the
  description.
- Focused Compose/Roborazzi coverage verifies light and dark presentation,
  selected/unselected semantics, 200% font scale, click behavior, and Dynamic
  theme visibility before and after Android 12.
- `./init.sh app` passed on 2026-09-13: 460 tests, zero failures, errors,
  or skips. No ADB device was connected, so the physical runtime pass remains
  pending.

Cashiro's launcher-logo switcher, Catppuccin palette, navigation label/pill
toggles, and changes to defaults or gates remain excluded.

## Reviewed candidate — Unified transaction filter sheet

**Review status:** Implemented on 2026-09-20 under delegated routine UI
authority. Automated verification is recorded below; on-device verification is
pending until an ADB device reconnects.

PennyWise already filters by search, period/custom dates, transaction type,
category, profile, account, tag, amount, and currency. The current presentation
spreads those controls across chips, dropdown menus, the totals card, and a
separate Amount & currency sheet. Cashiro collects several controls into one
sheet, but its controls mutate live ViewModel state and its Apply button merely
dismisses the sheet; dismissing or swiping therefore does not behave like a
true Cancel action.

### Recommended interaction contract

- Keep search and sort outside the sheet and immediate.
- Put period/custom range, type, category, profile, account, tag, amount, and
  currency refinement in one full-height modal sheet.
- Copy committed filters to a separate draft when the sheet opens. Controls
  change only the draft; Apply validates and commits the complete draft in one
  operation; Cancel, back, and swipe discard it; Reset restores draft defaults
  while keeping the sheet open.
- Preserve PennyWise's current single-select type, category, account, and tag
  behavior. Preserve the existing global profile scope and label it clearly as
  applying across the app.
- Keep the existing native-currency control as the canonical display/native
  currency selector. In unified mode, show original-currency refinement chips;
  in native mode, explain the current behavior rather than duplicating the
  selector.
- Keep ordinary transaction filters in ViewModel state. Do not add DataStore
  persistence; the existing profile and display-currency preferences remain
  global.
- Define navigation initialization explicitly so deep-link and budget filters
  cannot inherit stale account, tag, amount, or profile state accidentally.
- Applying the draft may prune bulk-selected transactions that no longer match,
  but the sheet must make that outcome deterministic and covered by tests.

### Explicitly excluded without separate approval

- Cashiro's multi-select transaction types, categories, accounts, or tags.
- Cashiro subcategories and LENT/BORROWED transaction types, which do not map to
  PennyWise's current transaction model.
- A RangeSlider derived from the largest transaction; precise text inputs are
  more accessible and do not make the useful range hostage to an outlier.
- Cashiro's live-mutation/fake-Apply behavior.
- Persisting the full filter bundle across ordinary app launches.

### Verification contract

- Draft Apply, Cancel/back/swipe, Reset, and invalid-amount behavior.
- Atomic AND semantics across all supported filters, conversion-failure
  handling, native/unified currency behavior, and totals.
- Deep-link and budget initialization, mode toggles, profile scope, and
  selection-mode pruning.
- Light, dark, AMOLED, and 200% font baselines; scrolling, nested custom-date
  picker behavior, selected semantics, fixed bottom actions, and 48dp targets.

### Implemented adaptation

- Replaced the scattered period/type/category/profile/account/tag dropdowns and
  the separate amount sheet with one full-height, scrollable PennyWise filter
  sheet. Search and sort remain immediate controls outside the sheet.
- The sheet edits a complete `TransactionFilterDraft`. Apply validates and
  commits the bundle, Cancel/back/swipe discard it through the sheet dismiss
  path, and Reset restores draft defaults without touching the visible list
  until Apply.
- Preserved single-select type, category, profile, account, and tag behavior.
  The profile section explicitly explains that its scope is shared with Home
  and applies across the app.
- Preserved the existing display/native-currency selector. Original-currency
  refinement appears only in unified mode; native mode explains which selector
  remains authoritative.
- Preserved budget drill-down category bundles until the user clears them or
  chooses a single category. New navigation and budget drill-downs explicitly
  clear stale screen-scoped category, account, tag, amount, and date filters,
  while intentionally retaining the app-wide profile scope.
- Removed the obsolete More-filters amount sheet and old dropdown header code.
  No multi-select filters, subcategories, LENT/BORROWED types, range slider, or
  filter persistence were introduced.
- Focused tests cover complete-draft Apply, Reset-without-live-mutation,
  Cancel-without-apply, invalid amount validation, unified/native currency
  behavior, every filter family, selected semantics, scrolling, light, dark,
  AMOLED, and 200% font rendering.
- Verification: `./init.sh app` passed on 2026-09-20 with 470 tests and zero
  failures, errors, or skips. On-device verification remains pending while the
  configured ADB device is disconnected.

## Reviewed candidate — Notification reminders

**Review status:** Product behavior must be defined before implementation.

PennyWise already creates transaction notifications from SMS, ingests supported
bank-app notifications, requests notification permission during onboarding, and
shows upcoming/overdue subscription data. It does not schedule daily reminders
or expose reminder preferences.

Cashiro's Settings screen suggests a daily new-transaction reminder, upcoming
payment reminders, and per-subscription opt-outs. The underlying receiver sends
only one generic notification, never reads subscription dates or opt-out IDs,
and does not re-arm its one-shot alarm after delivery. Copying that UI would
promise behavior the implementation does not provide.

### Product decisions required

- Remind for detected subscriptions, scheduled recurring transactions, or both.
- Daily summary versus one notification per due item; due window, overdue repeat
  policy, time of day, timezone/DST behavior, and mark-paid behavior.
- Disabled or enabled by default; free or Pro; permission-denied behavior.
- Inexact WorkManager delivery versus exact alarms. Prefer WorkManager unless
  minute-level precision is a genuine product requirement; a finance reminder
  normally does not justify exact-alarm permission.
- Notification privacy/copy and whether settings participate in backup/restore.
- If item-level opt-outs are required, use stable domain identity rather than a
  fragile DataStore set of database row IDs.

Keep bank-notification access separate from app-generated reminders so users do
not confuse Android notification-listener access with permission to display
PennyWise reminders. Before changing its manifest, independently verify the
current bank listener on a physical device because PennyWise declares the
service non-exported while Cashiro declares it exported.

### Verification contract

- Scheduling, cancellation/re-arm, reboot/process death, timezone/DST, and
  permission/channel behavior.
- Due, no-date, overdue, deletion/recreation, opt-out, mark-paid, and mixed-
  currency cases.
- Light, dark, large-font, and RTL Settings/reminder UI; localized notification
  text where supported.

## Reviewed candidate — In-app language selection

**Review status:** Defer until a deliberately scoped translation plan is
approved. A picker alone is not useful.

Cashiro declares 57 locales, checks in translated resource directories, and
uses AppCompat application locales. PennyWise currently has only the base and
night resource sets, while significant Settings and subscription copy remains
hardcoded English.

A correct first release must choose a smaller supported-locale set based on
reviewed translation quality, migrate all user-visible strings in the supported
flows, declare those locales, and then add AppCompat locale switching. Do not
copy Cashiro's long hardcoded locale list or claim support for incomplete or
unreviewed machine translations.

Required coverage includes locale persistence across recreation/process death,
resource fallback and completeness, RTL layout, long labels, date/number/money
formatting, and localized notification/background-worker text.

## Reviewed candidate — Multi-field transaction batch editing

**Review status:** Do not implement until the prerequisite transaction-integrity
work below is explicitly approved and completed.

Cashiro commit `61afaea6` adds a large batch-edit sheet for date, time,
category/subcategory, amount, and note. Its ViewModel loops through selected
rows and writes each separately, with no atomic rollback or Undo. Its ad-hoc
balance correction covers only some amount edits and ignores date moves,
transfers, loans, splits, tags, currencies, and related records.

PennyWise already has ID-based selection, currency-safe selection totals, bulk
category/group actions, manual transfer marking, transactional bulk soft-delete
and restore, and snackbar Undo. The Cashiro editor must therefore be adapted to
PennyWise's stronger domain model rather than layered over it.

### Existing integrity issues found during review

- Manual two-row transfer marking directly updates both rows without routing
  through the account-balance shift owner. It accepts unequal/mixed-currency
  pairs and missing or ambiguous accounts.
- Both rows are stored as complete TRANSFER rows with the same from/to legs but
  no structural pair identity. Manual-account recomputation can count both rows
  and debit/credit the transfer twice.
- Transfer legs store only last-four digits; balance lookup can target the wrong
  bank when two accounts share the same suffix.
- Bulk category and group updates—and their Undo paths—write row by row, so a
  failure can leave a partially applied operation.
- Bulk category can change the parent category of a split transaction while its
  split categories remain unchanged. The single-row editor correctly prevents
  that mismatch.
- Bulk delete/restore shifts balances transactionally, but loan-linked deletes
  can leave persisted loan remaining/status values stale.

These are correctness fixes with data-behaviour consequences and require
explicit approval before changing the current worktree.

### Required PennyWise-native contract

- On Apply, re-read selected IDs from Room and run the entire operation inside
  one database transaction. Never trust stale UI entity snapshots.
- Model every field as unchanged, set, or clear. Cancel/back/swipe writes
  nothing. Apply reports updated, skipped, and blocked counts.
- Route all balance effects through the original-to-updated balance-shift owner,
  then recompute each affected manual account once.
- Date/time, note, and parent category are valid only where their invariants are
  preserved; block parent-category changes on split rows unless the split model
  is edited deliberately.
- Tags need explicit Add, Remove, or Replace semantics and exact previous-set
  snapshots for Undo.
- A shared amount is valid only for one native currency per operation. Block
  amount edits for split rows until split redistribution is designed.
- Do not bulk-edit currency or account without a separate contract. Account
  moves require stable bank-plus-last-four identity, old/new balance effects,
  and currency compatibility.
- Treat transfers as a separate operation until the paired-transfer model and
  balance counting are corrected. Do not apply generic amount/account/type edits
  to them.
- Preserve loan linkage. Amount/type/currency/account edits or deletion of
  loan-linked rows must be blocked or update contribution, remaining amount,
  and settlement status transactionally.
- Editing a materialized transaction must never silently change its originating
  subscription or recurring template.
- Undo is one conflict-aware transaction: restore a row only if it still equals
  this operation's post-state, never overwrite a later edit. Keep Undo ephemeral
  to avoid a new schema/backup contract.

### Verification contract

- Atomic rollback, fresh ID re-read, concurrent edits, filter pruning, empty and
  partially invalid selections.
- Set/clear semantics, split restrictions, tag modes, exact/conflict-aware Undo,
  and mixed-currency amount rejection.
- Manual, SMS-tracked, credit-card, transfer, loan-linked, subscription-linked,
  recurring-materialized, and no-account cases.
- Balance/loan consistency after edit/delete/restore and backup consistency
  during multi-table changes.
- Selection and select-all UI, draft sheet Apply/Cancel/back/swipe, validation,
  accessibility, light/dark/AMOLED, RTL, and 200% font coverage.

## Reviewed candidate — Cloud backup and restore

**Review status:** Defer the remote-provider implementation. A local Backup &
Restore screen that presents PennyWise's existing behavior is the only safe
UI-primary slice ready for separate approval.

PennyWise already owns the important local foundation: its versioned full JSON
backup, merge/replace importer, manual save/share flow, CSV import/export, and
daily user-selected-folder backup with last-run status. Cashiro adds a much
larger cloud system: Google Drive `appDataFolder`, WebDAV, client-side
passphrase encryption, schedules, retention, snapshot browsing, deletion, and
restore. Its combined screen is roughly 1,700 lines and should not be copied as
a visual component.

The current PennyWise format also needs hardening before becoming a cloud
payload. Backups and automatic folder copies are plaintext JSON without an
integrity checksum. `MASKED` and `ANONYMOUS` do not currently redact every
entity: account/card, subscription, and merchant-mapping data can remain, and
CSV export includes the raw SMS body. Treat every current export as sensitive;
do not describe those modes as fully anonymized until entity-by-entity tests
prove it.

### Security and behavior findings

- Cashiro stores WebDAV credentials, Google access tokens, and the encryption
  passphrase through `EncryptedSharedPreferences`, but silently falls back to
  ordinary `MODE_PRIVATE` preferences if secure storage initialization fails.
  Remote credentials and passphrases must fail closed instead of becoming
  plaintext application preferences.
- The passphrase is retained on the device. That can protect the remote copy
  from the storage provider, but it is not protection against compromise of an
  unlocked device; the UI must describe that boundary accurately.
- Cashiro's encrypted format uses AES-256-GCM with PBKDF2-HMAC-SHA256, a random
  salt and IV, and 100,000 PBKDF2 iterations. Any PennyWise format needs its own
  versioned cryptographic envelope, authenticated metadata, compatibility
  tests, and a reviewed modern KDF cost rather than inheriting constants
  blindly.
- Restoring a Cashiro cloud snapshot always uses `REPLACE_ALL`. PennyWise must
  show the chosen Merge or Replace effect before download/restore and require a
  destructive confirmation for Replace.
- Cashiro writes a SHA-256 checksum but its importer does not verify it. Its ZIP
  extraction also has no archive or per-file size budget. Neither behavior is a
  safe integrity/import contract to inherit.
- Cashiro permits unencrypted cloud uploads when its optional E2E switch is
  off, and WebDAV may send both a plaintext backup and Basic credentials to a
  user-entered HTTP endpoint.
- Cashiro's checked-in privacy policy still claims that the app has no uploads
  or cloud services. UI, implementation, and published privacy wording must be
  updated together before any PennyWise remote feature ships.
- Google-specific dependencies must not leak into the F-Droid build. Provider
  interfaces and flavor boundaries have to be decided before navigation or UI
  promises are added.

### Required PennyWise-native contract

- Keep `BackupExporter`/`BackupImporter` as the single source of backup bytes
  and compatibility rules; providers only store and retrieve an opaque file.
- Require authenticated client-side encryption for every remote provider.
  Never upload a plaintext full backup, silently downgrade secure storage, or
  log credentials, tokens, passphrases, remote paths, or backup contents.
- Use least-privilege OAuth, explicit sign-out/revocation, cancellable uploads,
  network constraints, bounded retries, visible last-success/error state, and
  retention/delete confirmations.
- Define provider availability for Standard and F-Droid, credential lifecycle,
  recovery when a passphrase is lost, snapshot naming/versioning, concurrent
  devices, and whether remote automation is a Pro feature before implementation.
- A smaller UI-only pass may move the current local controls to a dedicated
  PennyWise-styled Backup & Restore destination, without changing their
  behavior or adding remote-provider placeholders.

### Verification contract

- Cryptographic known-answer, wrong-passphrase, corruption/truncation, format-
  version, large-file streaming, and secure-storage-unavailable cases.
- Provider auth expiry/revocation, offline/timeout/retry, partial upload,
  concurrent devices, retention, cancellation, delete, and restore recovery.
- Merge/Replace confirmation and rollback, worker constraints/restart, Standard
  versus F-Droid dependency isolation, plus light/dark/large-font/RTL UI.

## Reviewed candidate — BYO webhook synchronization

**Review status:** Defer until an explicit data-sharing and secret-storage
contract is approved. This is a security-sensitive integration, not UI polish.

Cashiro adds profiles, schedules, cursors, delivery logs, an editor, synthetic
test payloads, retries, redirect handling, and batches of up to 250
transactions. Payloads can include merchants, descriptions, categories,
transaction dates and amounts, bank names and account suffixes, account
balances and credit limits, budgets, and subscriptions. It does correctly use
synthetic values for its test payload and avoids forwarding custom headers to a
different redirect origin.

### Security and correctness findings

- URL validation permits plain `http://`, allowing financial data and custom
  authorization headers to cross the network unencrypted.
- There is no payload signature, replay nonce, or receiver-authentication
  contract. TLS protects transport but does not by itself prove that a receiver
  can authenticate a PennyWise delivery or reject a replay.
- Custom header values are stored as plaintext JSON in Room. Backup export
  blanks their values, which prevents one leak path but does not secure them at
  rest or define what happens after restore.
- Automatic retries intentionally resend a failed cursor window. Stable IDs
  help, but receiver idempotency is an external requirement and must be stated
  clearly; a generic third-party endpoint may create duplicates.
- Logs surface delivery messages and latest errors. They must be bounded and
  scrubbed so URLs, query secrets, response bodies, headers, and payload data
  cannot become persistent diagnostics.
- Enabling schedules creates continuing off-device disclosure. The UI needs a
  field-level preview and a persistent active-sharing indicator, not only a
  generic warning.

### Required PennyWise-native contract

- Default to HTTPS only. If local-network HTTP is ever supported, gate it behind
  a prominent per-endpoint warning and never send reusable secrets over it.
- Store secrets with fail-closed platform-backed protection, separate secret
  material from profile metadata, redact it from backups/logs/UI, and support
  explicit removal and credential rotation.
- Before first enable, show the exact selected data types and representative
  fields, destination host, cadence/range, currency scope, retry/duplicate
  behavior, and how to stop sharing.
- Apply strict URL/redirect/header validation, forbid dangerous or ambiguous
  headers, cap payload and response sizes, use bounded backoff, and never log
  payload bodies or secret-bearing URLs.
- Version the outbound schema and define deletion/tombstone, profile filtering,
  multi-currency, cursor reset, device-clock, and receiver-idempotency behavior.
  Do not add webhook tables to PennyWise's database or backup schema until this
  contract is approved.

### Verification contract

- HTTPS/HTTP, redirect-origin and downgrade cases, malformed URLs, DNS/network
  failures, timeouts, cancellation, retries, batch boundaries, and duplicate
  delivery.
- Cursor advancement only after complete success; clock/timezone changes,
  deleted/updated records, mixed currencies, profile filters, and process death.
- Secret storage failure, backup redaction/restore, log redaction and retention,
  test-payload privacy, plus consent, active-state, accessibility, and theme UI.

## Reviewed candidate — Import from Cashew

**Review status:** Useful as a later, isolated importer, but do not port
Cashiro's current implementation. A staged preview and stricter validation are
prerequisites even for a fresh PennyWise database.

Cashiro accepts either a Cashew SQLite file or a CSV, then imports wallets,
categories/subcategories, transactions, transfer pairs, budgets, limits, and
Google Drive receipt links. The UI immediately starts the import from a generic
`*/*` picker and reports only a final status; there is no preflight summary,
mapping review, conflict choice, or confirmation.

### Correctness and safety findings

- Input is copied without a size limit to a fixed cache filename. Any non-SQLite
  file is treated as CSV, with no strong source/schema/version validation.
- The CSV parser is handwritten and does not correctly support escaped quotes
  or multiline fields. Invalid money becomes zero and invalid/missing dates
  silently become the current time, which can create plausible-looking corrupt
  records.
- Money is read through `Double`, made absolute, and rounded to two decimals,
  losing exact decimal data and mishandling zero- or three-decimal currencies.
- SQLite dedup relies on the source primary key. CSV dedup uses only a 16-bit
  four-character digest plus row number; reordering rows produces duplicates
  and collisions can skip unrelated data. The in-memory dedup map is not
  advanced as rows are accepted.
- Wallet account suffixes are derived from only four hex characters of a name,
  so collisions can merge distinct wallets. Budgets are hardcoded to INR even
  when imported wallets and transactions use another currency. Budget dates
  are always interpreted as epoch milliseconds even though transaction dates
  accept both seconds and milliseconds.
- Receipt downloads happen before the SQLite database transaction, leaving
  orphan files if the database write fails. The CSV path performs network
  downloads inside the Room transaction. Downloads read entire bodies into
  memory without an explicit byte cap, and an undownloaded remote Drive URL is
  stored as if it were a saved attachment.
- Although the main SQLite writes are wrapped in one Room transaction, imported
  account balances are reconstructed only from newly imported rows and the
  mapping does not use PennyWise's authoritative balance/transfer invariants.

### Required PennyWise-native contract

- Add a Cashew-specific document type/recognizer, bounded temporary storage,
  input and row limits, supported schema fingerprints, and strict parsing that
  reports invalid rows instead of substituting zero or `now`.
- Parse monetary text directly to `BigDecimal` with the currency's scale. Use a
  durable source namespace plus full source ID or a strong canonical-content
  hash for repeatable deduplication.
- Stage the complete mapping before writes and show counts, currencies,
  accounts, category conflicts, transfers, budgets, receipts, duplicates, and
  invalid/skipped rows. Let the user confirm mappings and import scope.
- Perform database writes atomically through PennyWise repositories/invariants.
  Use stable bank-plus-account identity, preserve transfer and budget currency,
  and recompute affected account balances once after validation.
- Download optional receipts only after database commit to bounded private
  storage, with MIME/content validation, byte/count limits, cancellation, clear
  partial-result reporting, and orphan cleanup. Google authorization must be a
  separate explicit consent flow; importing core records must not require it.

### Verification contract

- Real supported Cashew schema fixtures plus missing/newer/corrupt/mislabeled
  files, very large inputs, malformed CSV quoting/newlines, invalid dates and
  amounts, and every supported currency scale.
- Repeat import, reordered CSV, hash collision, duplicate source IDs, account-
  suffix collision, category conflicts, transfer pairs/orphans, and multi-
  currency budgets.
- Atomic rollback, cancellation/process death, preflight-to-apply changes,
  receipt auth/network/content/size failures, orphan cleanup, and clear import
  summary UI across themes, RTL, and large fonts.

## Reviewed candidate — Chat screen visual and message UX

**Review status:** The visual-only first slice was implemented on 2026-09-20
under delegated routine UI authority. Message mutation and multi-session
behavior remain explicitly outside this slice.

Cashiro first restyled Chat in commit `362617fb`, then substantially rewrote the
AI/session stack inside broad commit `6a20b3c8`. Its current screen adds an
asymmetric user bubble, full-width Markdown assistant responses, copy/edit/
delete/regenerate actions, stop generation, chat history/search, a new-chat
action, and a compact composer. The PennyWise comparison branch explored a
smaller subset in `d2d736c4` and clarified the loading state in `034034b0`; only
the loading-state clarification is present in the current dirty worktree.

### Recommended UI-only slice

- Preserve PennyWise's current single conversation, model-download states,
  financial context, streaming, token warning, prompt suggestions, send, and
  clear-chat behavior.
- Present user prompts as a compact right-aligned asymmetric bubble and
  assistant responses as a calmer full-width reading surface, using only
  PennyWise tokens and semantic colors.
- Add Copy as the only per-message action. It is non-destructive and requires no
  DAO, repository, ViewModel, schema, or backup change. Give every action a 48dp
  semantic touch target and accessible confirmation.
- Move the existing Clear chat command into the top-app-bar overflow, but keep
  its current behavior and add an explicit confirmation before destructive
  clearing.
- Restyle the composer as one integrated field/action surface; preserve maximum
  lines, keyboard focus, enabled state, and the existing send callback.
- Localize all visible Chat/model-state/action copy touched by the slice and
  preserve the existing accessible indeterminate waiting indicator.

### Explicitly excluded

- Cashiro's chat sessions/history/search, new-chat persistence, message editing,
  individual deletion, response regeneration, and their DAO/entity/backup
  changes.
- Stop-generation behavior and partial-response persistence. Cancellation of a
  native model stream is a lifecycle/data decision, not an icon swap.
- A new Markdown dependency. Cashiro adds the Mike Penz renderer; the PennyWise
  comparison branch instead attempted a partial handwritten parser. Neither
  should be introduced without separately choosing supported Markdown syntax,
  sanitization/link behavior, dependency size, and F-Droid compatibility.
- Haze/blur duplication, Iconsax, hardcoded dimensions, 32dp action targets,
  Toast-only feedback, or the broad AI-model/service rewrite from `6a20b3c8`.

### Verification contract

- Existing model-not-downloaded/downloading/loading/ready/error, empty,
  populated, streaming, and token-warning states keep their callbacks and copy.
- Copy and confirmed Clear semantics; no clear on dismiss/back; keyboard IME,
  focus, long message, long unbroken text, list auto-scroll, and process
  recreation behavior.
- Light/dark/dynamic/branded/AMOLED, RTL, TalkBack order, reduced motion, and
  200% font visual coverage.

### Implemented adaptation

- Preserved PennyWise's single conversation, model download/loading/error
  states, financial context, streaming response, token warning, prompt
  suggestions, send behavior, and existing message storage.
- Restyled user prompts as compact right-aligned asymmetric bubbles and
  assistant replies as calm full-width reading surfaces. Both use PennyWise
  shape, spacing, color, and minimum-touch-target tokens.
- Added Copy as the only per-message action, with system clipboard integration
  and accessible Snackbar confirmation. Edit, delete, regenerate, Markdown,
  history/session, and stop-generation behavior were not introduced.
- Moved Clear chat to the app-bar overflow and routed both that action and the
  token warning through the same destructive confirmation dialog. Dismiss,
  back, and Cancel cannot clear messages.
- Replaced the detached field/button row with one integrated three-line
  composer that preserves focus, enabled/loading state, the send callback, and
  keyboard Send behavior.
- Localized every visible Chat/model/action string touched by the slice and
  retained the accessible indeterminate waiting state.
- Focused tests cover light, dark, AMOLED, 200% font rendering, user/assistant
  hierarchy, dynamic color, RTL mirroring, the single Copy action, keyboard
  Send, and confirmed-versus-cancelled Clear behavior. The existing
  waiting-state accessibility and visual tests remain green.
- Verification: `./init.sh app` passed on 2026-09-20 with 479 tests and zero
  failures, errors, or skips. On-device verification remains pending while the
  configured ADB device is disconnected.

## Reviewed candidate — App Lock

**Review status:** Do not port Cashiro's screen. App Lock already came from the
shared PennyWise history, and correctness fixes require explicit product
approval before a visual refresh claims stronger protection.

Cashiro did not independently add this capability: both repositories contain
the original `ffebb07f` App Lock feature with the same repository, biometric
manager, ViewModel, lifecycle, navigation, preference, and lock-screen model.
Cashiro's current differences are mostly package restructuring, localized copy,
a `CustomTitleTopAppBar`, and an Iconsax padlock. It still uses hardcoded screen
dimensions and retains the underlying behavior below. PennyWise already has the
`USE_BIOMETRIC` permission, device-credential fallback, enable switch, timeout
selector, foreground check, and blocking unlock destination.

### Existing behavior requiring approval before correction

- `PennyWiseApp` deliberately skips lock navigation while Settings is visible.
  If the timeout expires while the app is backgrounded on Settings, the lock
  state becomes true but the one-shot navigation effect can remain skipped
  after the user leaves Settings. This is a lock bypass.
- On successful unlock, navigation always goes to Home instead of restoring the
  previously visible destination and back stack.
- Timeout is measured from the last successful authentication, not from when
  the app entered the background. A long foreground session can therefore lock
  after even a brief app switch, while wall-clock changes can distort the
  timeout.
- App-lock preferences initially load asynchronously while the start
  destination can render Home. A cold launch needs a verified gated state to
  prevent sensitive content flashing before lock navigation.
- Settings describes the feature as biometric protection even though the
  allowed authenticators include device PIN/pattern/password. Enabling,
  disabling, and changing timeout do not require a fresh credential check.
- App Lock does not define recent-app thumbnail/screenshot protection or
  notification-content privacy. Adding `FLAG_SECURE` or suppressing notification
  content would be separate product behavior, not an automatic UI change.

### Recommended order if approved

1. Define timeout semantics as time spent away from the foreground, gate cold
   start before sensitive composition, remove the Settings bypass, and restore
   the pre-lock destination after successful authentication.
2. Decide whether changing protection settings requires re-authentication and
   whether screenshots/recent-app thumbnails and notification contents are in
   scope.
3. Then apply a PennyWise-native lock screen using `PennyWiseScaffold`, design
   tokens, localized credential-accurate copy, a semantic Material icon, clear
   unavailable/retry states, and no Cashiro dependency or route changes.

### Verification contract

- Cold/warm start, immediate and delayed timeout, short/long backgrounding,
  Settings background/return, process death, activity recreation, device-time
  change, and multi-window/configuration changes.
- Success, rejection, cancellation, lockout, hardware unavailable, no enrolled
  credential, enrollment change, device credential fallback, and repeated
  prompt behavior.
- Destination/back-stack restoration with deep links and Add shortcuts, plus
  no pre-auth content flash.
- Settings enable/disable/timeout authorization choices and light/dark/AMOLED,
  large-font, RTL, and TalkBack lock-screen coverage.

## Reviewed comparison — Account management screens

**Review status:** No additional Cashiro screen should be ported. The current
PennyWise worktree already contains the useful presentation improvements, while
PennyWise's account navigation and data behavior are newer and more complete.
The remaining localization and visual-verification pass was implemented on
2026-09-21 under delegated routine UI authority.

Cashiro's account-related history spans the large original `688e280f` manage-
screen change, account merging in `7c1a64d1`, wallet/main-account additions,
the Account Detail redesign, and later comparison-branch work for account
sheets, the number pad, account preview, expression input, custom icon/color,
and shared card composition (`b65e4fd7`, `b83eb6e5`, `b066fdca`, `d9f61219`,
`6dd4bbd3`, and `cb403a70`).

### Already present or stronger in PennyWise

- Manage Accounts separates bank/wallet accounts, credit cards, hidden items,
  linked/orphaned cards, low-balance warnings, statement dates, aliases, and
  actions without replacing repository ownership.
- Add Account and Account Detail are real destinations. Balance History is a
  full page backed by navigation arguments and account-native currency, rather
  than Cashiro's nested history sheet.
- Merge Accounts already exists with transaction retargeting, and has since
  received scrolling and spacing fixes in PennyWise.
- The current UI worktree already uses `PennyWiseCardV2`, alias-aware identity,
  brand/tiled presentation, semantic balance hierarchy, and correct success/
  warning/error colors for credit utilization.
- Balance and credit-limit editing already use the shared token-backed number
  pad with exact `BigDecimal` expression evaluation, a two-step credit-card
  flow, currency-aware previews, validation, Cancel/Back, and dedicated visual
  tests. This is a corrected PennyWise-native version of Cashiro's number-pad
  idea, not a direct copy.

### Deliberately not ported

- Custom account icon and color persistence. Cashiro adds fields to the account
  entity and propagates them through edit sheets, backup, cards, and navigation.
  This is a schema/identity feature—not merely theming—and remains excluded by
  the approved Home account-card contract unless separately approved.
- Cashiro's Iconsax dependency, bulk icon assets, hardcoded card/sheet
  dimensions, in-place add-account sheet, and duplicate HistorySheet.
- Replacing PennyWise's account routes, merge use case, currency resolution,
  balance-history queries, low-balance behavior, statement-date behavior, or
  linked-card handling with the older fork implementation.

### Remaining review points

- Continue localizing the untouched hardcoded Manage Accounts copy as part of a
  deliberate screen-wide localization pass, not one label at a time.
- Preserve exact monetary parsing and account-native currencies in every edit
  path; never reintroduce Cashiro's `Double`-based amount handling.
- Keep visual/semantics coverage for a savings account with alias/linked card,
  a credit card with limit, balance update, and the second credit-limit step;
  add large-font/RTL/device coverage when this screen is next exercised.

### Implemented finishing pass

- Moved the complete Manage Accounts presentation copy—including app-bar and
  section labels, menus, dialogs, input labels, dynamic counts/dates, and
  meaningful content descriptions—into string/plural resources. The visible
  English meaning and every callback remain unchanged.
- Removed the final screen-local measurement from the touched presentation and
  uses the existing spacing/elevation/stroke tokens. The credit-limit amount
  and utilization now stack in one trailing column so the label and values do
  not compete for a single line.
- Preserved account grouping, hidden state, aliases, linked and orphaned cards,
  low-balance warnings, statement dates, account-native currencies, exact
  BigDecimal editing, profile reassignment, merge gating, delete confirmation,
  history navigation, and every existing route.
- Extended focused component tests for mixed savings/credit presentation,
  hidden-account Show behavior, 200% font, RTL, AMOLED, and the two-step credit
  update Back flow. Recorded light, dark, mixed, and AMOLED/large-font/RTL
  baselines were visually inspected.
- Screen-level empty/unlinked-card composition remains covered by the existing
  ViewModel-owned screen rather than a duplicate test harness; this pass did
  not recreate Hilt/Room state merely to obtain another screenshot.
- Verification: the required ./init.sh app gate passed on 2026-09-21 with 496
  tests and zero failures, errors, or skips. A physical-device pass remains
  pending because no ADB device is connected.

## Reviewed comparison — Budget screens

**Review status:** Keep PennyWise's budget model and calculations. The Budget
History refresh, Budget overview-card cleanup, and PennyWise-native Budget
Detail destination are now implemented. Cashiro's remaining budget work is
limited to the approval-sensitive behavior changes below.

Cashiro's distinctive budget work consists of a large entity/repository/editor
rewrite, a gradient summary card, a dedicated Budget Detail destination with a
category chart and transaction list, and a History destination with a spending
line and percentage rings. PennyWise has since developed a different and more
complete budget-group foundation, so the screens cannot safely be cherry-picked
as one feature.

### Already present or stronger in PennyWise

- Weekly, monthly, and custom budgets use resolved cycle windows, including
  weekday/month-day anchors, historical snapshots, navigation across empty past
  months, and correct per-window history.
- A budget can track categories, all expenses, or a transaction-type bucket.
  LIMIT, TARGET, and EXPECTED groups preserve PennyWise's spending, investment,
  and expected-cost semantics rather than collapsing everything into Cashiro's
  EXPENSE/SAVINGS switch.
- Refund and budget-impact handling, split transactions, unified-currency
  conversion, and missing-rate behavior all have fixes that post-date the fork.
  Replacing this with Cashiro's repository would regress monetary correctness.
- The overview already expands in place to show a spending-pace chart and
  category rows; category rows link to filtered Transactions. History already
  supports category breakdowns for each resolved window.

### Existing worktree adaptation — Budget History

- Adds a token-backed `Spending trend` card only when at least two comparable,
  non-future windows exist.
- Makes each history row a full-card target and shows spent/limit hierarchy,
  a bounded percentage ring, semantic success/warning/error color, and existing
  current/frozen status without changing the breakdown callback.
- Uses the effective category-funded amount for every history window. Weekly
  summary limits multiply that effective amount by the actual number of windows;
  this corrects a direct Cashiro-style implementation that would use only the
  base entity amount.
- The focused suite covers category-funded weekly math, monthly math, excluding
  future trend windows, click behavior, extreme overspend labels, and light/dark
  visual baselines. It was included in the last recorded full app gate (445
  tests, zero failures); a physical-device pass remains pending because the
  configured Pixel is disconnected.

### Implemented adaptation — Budget overview card

- Replaces the cramped 32dp edit/delete/menu cluster with one 48dp overflow
  action containing Edit, period History, Move up/down, and Delete. Delete still
  opens the existing confirmation dialog; no destructive callback or repository
  behavior changed.
- Keeps full-card expansion while adding a visible Show/Hide breakdown cue and
  expansion-state semantics. Cards with no chart/category breakdown no longer
  advertise a no-op expansion.
- Uses token-backed screen dimensions and touch targets, localized user-facing
  copy (including plural cycle timing), locale-aware dates/weekdays, and an
  accessible dark-theme expansion label. The over-budget subtitle now shows
  the useful renewal timing instead of repeating the overage already in the
  headline.
- Preserves all totals, progress/status thresholds, resolved budget windows,
  category navigation, edit/history/reorder callbacks, and currency formatting.
- Adds three focused Compose tests for light/dark rendering, expand/collapse,
  the 48dp overflow target, disabled reorder state, and every overflow callback,
  plus two recorded visual baselines.
- Verification: `./init.sh app` passed with 448 tests and zero failures,
  errors, or skips. The standard debug APK was then installed on the connected
  Pixel 9; the empty state, smart-default overview, expanded breakdown, chart,
  category rows, and overflow menu were visually checked in dark mode.

### Implemented adaptation — Budget Detail

- Adds a typed Budget Detail destination reached through a visible `View
  details` action on each overview card. Back, Edit, and transaction-row taps
  reuse the existing destinations and callbacks.
- Reuses the Budget Groups back-stack ViewModel and its exact selected-window
  summary rather than calculating a second total. The detail combines the
  budget type, period and live/completed state, progress and remaining amount,
  category breakdown, and matching transaction rows.
- Projects matching rows with the existing native and unified aggregation
  semantics, including split categories, `Others`, refund/extra-budget impacts,
  transaction-type buckets, and tracking-all behavior. Unified rows with a
  missing conversion rate remain omitted instead of receiving a face-value
  fallback.
- Shows whole-transaction values in the shared transaction row and explicitly
  explains that only the matching split portion contributes to the budget.
- Preserves the current repository scope exactly. Budget aggregation does not
  presently apply the app-wide selected-profile or hidden-account filters, so
  this detail screen does not add an independent filter that would make its
  transaction list disagree with the displayed total. Changing that scope is
  a separate financial-behavior decision.
- Uses the current design tokens and shared cards, 48dp app-bar targets, and
  high-contrast amount hierarchy. Recorded light, dark, and AMOLED/200%-font
  baselines cover the summary, breakdown, transaction list, split disclosure,
  and large-text scrolling.
- Focused tests cover split/category/type-bucket matching, `Others`, refunds,
  transfers, native versus unified tracking-all edges, missing rates, empty
  windows, and the transaction callback. No entity, schema, or database
  migration was added.
- Verification: the required `./init.sh app` gate passed on 2026-09-21 with
  492 tests and zero failures, errors, or skips. Recorded light, dark, and
  AMOLED/200%-font baselines were visually inspected; a physical-device pass
  remains pending when the configured device is available.

### Approval-sensitive behavior — do not add implicitly

- Account-scoped budgets. Cashiro persists a list of composite account strings
  and matches them with substring checks; that identity can collide. Supporting
  this in PennyWise requires stable account IDs plus entity, migration, backup,
  repository, edit, and history propagation.
- `Added only` versus `All transactions`. Cashiro's source filter changes which
  records count toward a budget and needs a precise definition for manual,
  imported, restored, recurring, and SMS-created transactions.
- Daily/yearly recurrence and a distinct Savings budget type. These alter the
  period and goal model; they are not necessary to reproduce the reviewed UI.
- Cashiro's broad edit bottom sheet and entity replacement, float-based money
  ratios, string removal of `.00`, fixed 24-period history cap, custom icon/color
  dependency, shared-transition architecture, and older transaction aggregation.

### Verification contract for either candidate

- Limit, target, expected, all-expense, category-funded, and transaction-type
  budgets; weekly/monthly/custom windows; current, past, empty, under, warning,
  and over-budget states.
- Split/refund/budget-impact transactions, selected profile, hidden accounts,
  native and unified currency, and missing conversion rates.
- Edit/history/delete/reorder/expand/detail/transaction callbacks, back stack,
  destructive confirmation, 48dp targets, TalkBack order, reduced motion, RTL,
  200% font, and light/dark/dynamic/branded/AMOLED visuals.

## Reviewed candidate — Add Transaction UI

**Review status:** The first PennyWise-native visual slice is implemented and
focused verification passes. The already approved shared Account Selection
sheet remains in place. Currency/profile ownership questions below are
correctness changes and remain deliberately outside this visual work.

Cashiro makes the Add flow feel more deliberate with a large amount hero,
calculator sheet, segmented Transaction/Subscription switcher, type-first form
order, richer account/category cards, bottom-sheet pickers, and a persistent
bottom Save action. It also mixes in subcategories, multi-attachments, inline
lend/borrow creation, account identity fields, subscription editing, Haze,
shared transitions, Iconsax, and hardcoded measurements. Those additions are
not one safe UI patch.

### PennyWise behavior to preserve

- Transaction/Subscription tabs, optional no-account/manual entry, editable
  currency, expense/income/credit/investment/transfer types, date and time,
  merchant, notes, tags, receipt camera/file selection, and the sticky Save
  action.
- The approved account bottom sheet, including distinct From/To transfer legs,
  no manual option for transfers, alias-aware labels, account-native balances,
  and dismissal semantics.
- Atomic two-leg same-currency transfers, account balance updates, duplicate-as-
  new prefill, merchant-to-category suggestions, split/refund-aware budget
  behavior, income budget impacts, widget refresh, and exact `BigDecimal`
  persistence.
- The approved custom subscription-cycle editor and existing subscription
  creation semantics. The Transaction form must not silently create or update a
  subscription except through the existing `isRecurring` contract.

### Recommended visual slice

- Keep `PennyWiseScaffold` and the two-page pager, but localize the app-bar and
  tab copy and use one token-backed segmented/tab treatment with a clear active
  state. Do not add Haze or shared-transition ownership to this form.
- Reorder the form so amount and transaction type come first, followed by date/
  time, account/category, merchant/notes, tags, income budget impact, and
  receipt. Conditional transfer fields should appear without leaving stale
  merchant/category requirements behind.
- Present the amount as the main visual anchor. If calculator entry is approved,
  reuse PennyWise's tested `BigDecimal` `NumberPad`; Cashiro's animated counter
  converts money through `Float`, rounds large/precise values, hardcodes font
  sizes, and animates every edit from zero, so it must not be ported.
- Replace the anchored category dropdown with the existing searchable
  PennyWise category picker, using the selected category icon and Material
  assets only. PennyWise has no subcategory model, so the field remains one
  level.
- Keep the sticky Save action, but surface `uiState.error` in an accessible
  inline/banner state. Today persistence and transfer errors are stored but not
  rendered, leaving failures silent.
- Remove screen-local dp/sp literals touched by the refresh, use theme tokens,
  maintain 48dp targets, localize touched copy, and respect reduced motion.

### Implemented adaptation — Add shell and transaction form

- Replaces the plain tab row with a token-backed segmented
  Transaction/Subscription control while preserving the existing pager and
  both creation flows.
- Reorders the transaction form to amount, type, date/time, account/category,
  merchant/notes, tags, income budget impact, and receipt. Conditional transfer
  fields and every existing update/save callback are unchanged.
- Replaces the anchored category dropdown with the approved searchable quick
  category sheet and shows the selected category icon in a connected grouped
  row. No subcategory model or category persistence was added.
- Adds the shared exact `BigDecimal` calculator behind an optional trailing
  action on the amount field. Keyboard, paste, and direct editing remain the
  primary path; applying a valid expression writes its exact plain-decimal
  result back through the existing amount callback.
- Renders repository/save failures in an inline error container instead of
  silently retaining `uiState.error`.
- Localizes all user-facing copy in the touched transaction form, uses
  locale-aware date/AM-PM formatting, removes screen-local dp literals, derives
  grouped corners from the shared list-position shapes, and restores 48dp clear
  and receipt-removal targets.
- Adds focused light/dark visual and interaction coverage for the mode switcher,
  category selector, and calculator, including exact expression application and
  the disabled empty state. Recorded baselines were visually reviewed in both
  themes. The final `./init.sh app` gate passes with 454 tests and zero failures,
  errors, or skips. Packaging succeeds, but the physical-device install/pass is pending:
  the configured Pixel disconnected before `installStandardDebug`, and
  `adb devices` currently reports no attached device.
- Does not change linked-account currency, selected-profile ownership,
  monetary parsing, transfer persistence, subscription behavior, or receipt
  storage; calculator use remains optional alongside keyboard/paste entry.

### Implemented adaptation — Subscription form

- Preserves PennyWise's stronger Expense/Income direction behavior, strict
  custom-cycle encoding, scheduling math, optional funding account, account
  balance behavior, and existing save/error callbacks.
- Reuses the optional exact amount calculator and searchable quick category
  sheet from the transaction form. Keyboard/paste amount entry remains
  available, and the category remains a single existing PennyWise category.
- Reorders the form to direction/info, amount, billing/date, custom interval,
  funding account/category, then service/notes. Account and category now read as
  one connected group; service and notes form a separate details group.
- Displays the localized resolved custom interval (for example, `Every 3
  weeks`) instead of the generic `Custom` label, and gives its existing editor
  a tonal PennyWise card without changing its callbacks or stored format.
- Removes screen-local dp literals, uses shared list-position shapes and 48dp
  targets, localizes touched copy, and uses locale-aware schedule dates.
- Adds stateful light/dark visual and interaction coverage for both recurring
  directions; the income baseline verifies its automatic-schedule explanation.
  The final `./init.sh app` gate passes with 456 tests and zero failures,
  errors, or skips.
- Excludes Cashiro's custom-cycle end date, automatic first transaction,
  subcategories, multiple attachments, automatic account selection, and
  account-owned currency changes because those alter recurrence, persistence,
  ownership, or financial behavior rather than presentation.

### Approval-sensitive correctness decisions

1. **Linked-account currency.** For non-transfer entries, selecting an account
   currently leaves the independently selected transaction currency unchanged.
   A USD account can therefore receive a numerically applied INR transaction
   while its balance remains labelled USD. Recommended rule: a linked account
   owns the transaction currency and the selector becomes account-locked;
   manual/no-account entries retain free currency selection.
2. **Selected-profile ownership.** Add currently lists accounts from every
   profile. Account-linked transactions later inherit that account's profile,
   while a no-account transaction has no explicit profile and falls back to the
   built-in Personal profile. From another selected profile it can disappear
   immediately after Save. Recommended rule: filter the picker to the selected
   profile and stamp that profile on manual/no-account transactions; `All`
   remains unfiltered and needs an explicit default ownership rule.
3. **Calculator-only amount entry.** Cashiro replaces keyboard/paste entry with
   a modal keypad. This changes hardware-keyboard, clipboard, accessibility,
   dismissal, and unsaved-expression behavior. The implemented calculator is
   only a secondary action; replacing the editable field still requires an
   explicit behavior decision.

### Explicitly excluded

- Cashiro's LENT/BORROWED transaction types, due-date/person carousel, and
  inline person creation. PennyWise already has the approved People foundation
  and dedicated lend/borrow entry flow; duplicating it in Add would create two
  ownership paths.
- Subcategories and Cashiro's subcategory entity/repository/category-asset
  changes, consistent with the approved category scope.
- Generic multi-attachment storage. PennyWise currently has a single managed
  receipt path with capture/share/delete handling; multiple heterogeneous files
  require a separate entity, backup, cleanup, permission, and privacy contract.
- Cashiro's automatic main-account preselection. It changes a formerly manual
  entry into a balance-affecting transaction without an explicit selection.
- Cashiro's subscription edit route, automatic first transaction on subscription
  creation, custom-cycle end date, custom account icon/color fields, Haze,
  Iconsax, rotating placeholder copy, infinite decorative animation, or raw
  `Float`/`Double` money formatting.
- Exposing the dormant one-click `isRecurring` switch as a fixed monthly
  subscription. The Subscription tab now has an explicit cycle model and should
  remain the clear recurring-entry path unless product behavior is redesigned.

### Verification contract

- Fresh expense/income/credit/investment and transfer; duplicate prefill;
  optional account; camera/file receipt; tags; merchant suggestion; income
  budget impact; save error and retry; loading and back/dismiss behavior.
- Exact decimal and large values, calculator precedence/invalid/divide-by-zero,
  negative/zero rejection, locale decimal expectations, paste/hardware keyboard
  if retained, rotation/process recreation, and double-submit prevention.
- Same/different/cross-currency transfer accounts and the approved linked-account
  currency rule; Personal/custom/All profile cases after the profile rule is
  approved.
- Keyboard/insets, 48dp targets, TalkBack traversal, reduced motion, RTL, 200%
  font, and light/dark/dynamic/branded/AMOLED visual baselines.

## Reviewed comparison — Scan spotlight tutorial

**Review status:** No Cashiro port is needed. The Spotlight composable,
ViewModel state machine, first-show preference, Home FAB position capture, and
Main-screen overlay are already the same implementation in both codebases apart
from package names. This is shared PennyWise ancestry, not a Cashiro addition.

The current implementation is also not a model for further UI work: it uses
hardcoded white/black colors and dp values, assumes the message fits in a fixed
200dp box positioned 220dp left of the FAB, does not adapt to narrow/RTL/large-
font layouts, and animates without consulting reduced-motion settings. The copy
says tapping anywhere dismisses the tutorial, while tapping inside the cutout
also starts an SMS scan. It has no focused UI or semantics coverage.

Do not spend port effort here. A future accessibility/localization cleanup may
replace the fixed tooltip with a window-aware anchored coach mark, tokenized
surface roles, explicit `Scan messages` and `Dismiss` semantics, localized copy,
and reduced-motion handling. That is ordinary PennyWise design-system debt and
is not required for Cashiro parity.
