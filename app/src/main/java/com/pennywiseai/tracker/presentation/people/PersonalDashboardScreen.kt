package com.pennywiseai.tracker.presentation.people

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.preferences.CoverStyle
import com.pennywiseai.tracker.data.repository.PersonLoanSummary
import com.pennywiseai.tracker.data.repository.PersonWithSummary
import com.pennywiseai.tracker.ui.components.AvatarHelper
import com.pennywiseai.tracker.ui.components.CoverGradientBanner
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
import com.pennywiseai.tracker.ui.components.HomeBannerImage
import com.pennywiseai.tracker.ui.components.TonalNavigationButton
import com.pennywiseai.tracker.ui.components.cards.GroupedList
import com.pennywiseai.tracker.ui.components.cards.GroupedRow
import com.pennywiseai.tracker.ui.components.cards.IconTile
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.PennyWiseCardV2
import com.pennywiseai.tracker.ui.components.cards.RowLabels
import com.pennywiseai.tracker.ui.components.cards.SectionHeaderV2
import com.pennywiseai.tracker.ui.components.legibleOn
import com.pennywiseai.tracker.ui.components.parseProfileColor
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.ui.effects.rememberOverscrollFlingBehavior
import com.pennywiseai.tracker.ui.icons.iconax.DirectboxReceive
import com.pennywiseai.tracker.ui.icons.iconax.DirectboxSend
import com.pennywiseai.tracker.ui.icons.iconax.DollarCircle
import com.pennywiseai.tracker.ui.icons.iconax.Edit2
import com.pennywiseai.tracker.ui.icons.iconax.Iconax
import com.pennywiseai.tracker.ui.screens.profile.EditProfileSheet
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.PennyWiseText
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.ui.theme.expense
import com.pennywiseai.tracker.ui.theme.income
import com.pennywiseai.tracker.utils.CurrencyFormatter
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import java.math.BigDecimal
import java.text.NumberFormat

/*
 * The profile page, laid out like Cashiro's: the user's own banner behind a
 * pinned top bar, a centred avatar with the name beneath it, a card of stat
 * tiles, then the lend & borrow figures, shortcuts and a carousel of contacts.
 * Every figure is either a count or a per-currency amount; nothing here ever
 * adds money across currencies.
 */

/** The hero's avatar: a step above a row avatar so it can anchor the page. */
private val HeroAvatarSize = Dimensions.Icon.extraLarge

/**
 * The hero block is at least this tall (the avatar plus room above it), so the
 * avatar sits in the banner's fade rather than hard against the top bar.
 */
private val HeroMinHeight = HeroAvatarSize + Spacing.xxxl + Spacing.lg

/** Contact tile size as multiples of a two-line row (about Cashiro's 120 x 160 card). */
private const val CONTACT_TILE_WIDTH_MULTIPLIER = 1.75f
private const val CONTACT_TILE_HEIGHT_MULTIPLIER = 2.5f

/** How strongly the decorative initials show through a contact tile. */
private const val TILE_WATERMARK_ALPHA = 0.25f

/** The carousel shows this many contacts; "View all" opens the rest. */
private const val DASHBOARD_PEOPLE_LIMIT = 10

@Composable
fun PersonalDashboardScreen(
    onNavigateBack: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToLoans: () -> Unit,
    onNavigateToPerson: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PersonalDashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showEditProfile by rememberSaveable { mutableStateOf(false) }
    PersonalDashboardContent(
        state = state,
        onNavigateBack = onNavigateBack,
        onNavigateToContacts = onNavigateToContacts,
        onNavigateToLoans = onNavigateToLoans,
        onNavigateToPerson = onNavigateToPerson,
        onEditProfile = { showEditProfile = true },
        modifier = modifier,
    )
    if (showEditProfile) {
        EditProfileSheet(onDismiss = { showEditProfile = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PersonalDashboardContent(
    state: PersonalDashboardUiState,
    onNavigateBack: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToLoans: () -> Unit,
    onNavigateToPerson: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onEditProfile: () -> Unit = {},
) {
    // A pinned compact bar, as on Cashiro's profile: the page is a banner with
    // content over it, so there is no large title to collapse.
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }
    val listState = rememberLazyListState()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CustomTitleTopAppBar(
                scrollBehaviorSmall = scrollBehavior,
                scrollBehaviorLarge = scrollBehavior,
                title = stringResource(R.string.personal_dashboard_title),
                hasBackButton = true,
                navigationContent = {
                    TonalNavigationButton(
                        onClick = onNavigateBack,
                        contentDescription = stringResource(R.string.people_back),
                    )
                },
                // Edit identity: name, avatar, avatar colour and the Home banner.
                actionContent = {
                    PeopleTonalActionButton(
                        onClick = onEditProfile,
                        icon = Iconax.Edit2,
                        contentDescription = stringResource(R.string.edit_profile_title),
                    )
                },
                hazeState = hazeState,
            )
        },
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
        } else {
            DashboardBody(
                state = state,
                listState = listState,
                hazeState = hazeState,
                paddingValues = paddingValues,
                onNavigateToContacts = onNavigateToContacts,
                onNavigateToLoans = onNavigateToLoans,
                onNavigateToPerson = onNavigateToPerson,
            )
        }
    }
}

@Composable
private fun DashboardBody(
    state: PersonalDashboardUiState,
    listState: LazyListState,
    hazeState: HazeState,
    paddingValues: PaddingValues,
    onNavigateToContacts: () -> Unit,
    onNavigateToLoans: () -> Unit,
    onNavigateToPerson: (Long) -> Unit,
) {
    val gutter = Dimensions.Padding.content
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // The banner paints behind the transparent top bar, exactly as on Home: a
        // user-chosen image wins over the cover style.
        val bannerUri = state.homeBannerUri
        if (bannerUri != null) {
            HomeBannerImage(
                imageUri = bannerUri,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        } else if (state.coverStyle != CoverStyle.NONE) {
            CoverGradientBanner(
                coverStyle = state.coverStyle,
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .overScrollVertical(),
            // The horizontal inset is applied per item so the contact carousel can
            // scroll all the way to the screen edge.
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding() + Spacing.md,
                bottom = paddingValues.calculateBottomPadding() + Spacing.Layout.scrollBottomPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.Layout.headerToContent),
            flingBehavior = rememberOverscrollFlingBehavior { listState },
        ) {
            item(key = "hero") {
                DashboardHero(state, Modifier.padding(horizontal = gutter))
            }

            item(key = "glance") {
                DashboardGlanceCard(state, Modifier.padding(horizontal = gutter))
            }

            item(key = "balances") {
                Column(
                    modifier = Modifier.padding(horizontal = gutter),
                    verticalArrangement = Arrangement.spacedBy(Spacing.Layout.headerToContent),
                ) {
                    SectionHeaderV2(title = stringResource(R.string.personal_dashboard_overview))
                    DashboardBalanceRows(state.loanSummary)
                }
            }

            item(key = "shortcuts") {
                Column(
                    modifier = Modifier.padding(horizontal = gutter),
                    verticalArrangement = Arrangement.spacedBy(Spacing.Layout.headerToContent),
                ) {
                    SectionHeaderV2(title = stringResource(R.string.personal_dashboard_shortcuts))
                    GroupedList {
                        GroupedRow(position = ListItemPosition.Top, onClick = onNavigateToContacts) {
                            IconTile(
                                Icons.Default.People,
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            RowLabels(
                                title = stringResource(R.string.people_contacts_title),
                                subtitle = pluralStringResource(R.plurals.personal_dashboard_contacts_count, state.people.size, state.people.size),
                            )
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                modifier = Modifier.size(Dimensions.Icon.inline),
                            )
                        }
                        GroupedRow(position = ListItemPosition.Bottom, onClick = onNavigateToLoans) {
                            IconTile(
                                Icons.Default.SwapHoriz,
                                MaterialTheme.colorScheme.secondaryContainer,
                                MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            RowLabels(
                                title = stringResource(R.string.lend_borrow_title),
                                subtitle = pluralStringResource(R.plurals.personal_dashboard_records_count, state.loanSummary.activeLoanCount, state.loanSummary.activeLoanCount),
                            )
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                modifier = Modifier.size(Dimensions.Icon.inline),
                            )
                        }
                    }
                }
            }

            if (state.people.isNotEmpty()) {
                item(key = "people") {
                    DashboardPeopleSection(
                        people = state.people,
                        onViewAll = onNavigateToContacts,
                        onPersonClick = onNavigateToPerson,
                    )
                }
            }
        }
    }
}

/**
 * The centred identity block over the banner: avatar, name, one line of context.
 * The edit button lives in the top bar, so this block is purely display.
 */
@Composable
private fun DashboardHero(state: PersonalDashboardUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = HeroMinHeight)
            .padding(bottom = Spacing.sm),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DashboardAvatar(
            userName = state.userName,
            imageUri = state.profileImageUri,
            backgroundColor = state.profileBackgroundColor,
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            text = state.userName.ifBlank { stringResource(R.string.greeting_default_user_name) },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(R.string.personal_dashboard_hero_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * The user's avatar, ringed in the page background so it reads as cut out of the
 * banner behind it: a preset drawable, their own photo, or their initials.
 */
@Composable
private fun DashboardAvatar(
    userName: String,
    imageUri: String?,
    backgroundColor: Int,
    modifier: Modifier = Modifier,
) {
    val fill = if (backgroundColor != 0) Color(backgroundColor) else MaterialTheme.colorScheme.primaryContainer
    // A user-picked swatch can be any colour, so pick the initials' colour by luminance.
    val onFill = if (backgroundColor != 0) contentColorOn(fill) else MaterialTheme.colorScheme.onPrimaryContainer
    val ring = Dimensions.Component.selectionStroke
    val photoDescription = stringResource(R.string.edit_profile_photo_description)
    Box(
        modifier = modifier
            .size(HeroAvatarSize)
            .border(ring, MaterialTheme.colorScheme.background, CircleShape)
            .padding(ring)
            .clip(CircleShape)
            .background(fill),
        contentAlignment = Alignment.Center,
    ) {
        val presetRes = imageUri?.let(AvatarHelper::resolveAvatarDrawable)
        when {
            presetRes != null -> Image(
                painter = painterResource(presetRes),
                contentDescription = photoDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            imageUri != null -> AsyncImage(
                model = imageUri,
                contentDescription = photoDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            else -> Text(
                text = userName.dashboardInitials(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = onFill,
            )
        }
    }
}

/**
 * A card of four stat tiles (Cashiro's "Financial overview" card). They are all
 * counts: what is owed in money is shown per currency just below, so no figure
 * is repeated and no amount is ever added across currencies.
 */
@Composable
private fun DashboardGlanceCard(state: PersonalDashboardUiState, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val tileSurface = scheme.surfaceContainerHigh
    val summary = state.loanSummary
    val currencyCount = (summary.lentByCurrency.keys + summary.borrowedByCurrency.keys).size

    // Semantic colours wash into the tile's glyph container; the glyph itself is
    // nudged until it reads against that wash.
    val incomeWash = scheme.income.copy(alpha = Dimensions.Alpha.tonalIconContainer)
    val incomeGlyph = scheme.income.legibleOn(
        background = incomeWash.compositeOver(tileSurface),
        towards = scheme.onSurface,
    )
    val expenseWash = scheme.expense.copy(alpha = Dimensions.Alpha.tonalIconContainer)
    val expenseGlyph = scheme.expense.legibleOn(
        background = expenseWash.compositeOver(tileSurface),
        towards = scheme.onSurface,
    )

    PennyWiseCardV2(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Text(
            text = stringResource(R.string.profile_page_glance_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = scheme.onSurface,
        )
        Column(
            modifier = Modifier.padding(top = Spacing.smd),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                GlanceTile(
                    label = stringResource(R.string.profile_page_stat_lent),
                    value = state.lentRecordCount.formatted(),
                    icon = Iconax.DirectboxSend,
                    containerColor = incomeWash,
                    contentColor = incomeGlyph,
                    modifier = Modifier.weight(1f),
                )
                GlanceTile(
                    label = stringResource(R.string.profile_page_stat_borrowed),
                    value = state.borrowedRecordCount.formatted(),
                    icon = Iconax.DirectboxReceive,
                    containerColor = expenseWash,
                    contentColor = expenseGlyph,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                GlanceTile(
                    label = stringResource(R.string.profile_page_stat_settled),
                    value = state.settledRecordCount.formatted(),
                    icon = Icons.Default.CheckCircle,
                    containerColor = scheme.primaryContainer,
                    contentColor = scheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f),
                )
                GlanceTile(
                    label = stringResource(R.string.profile_page_stat_currencies),
                    value = currencyCount.formatted(),
                    icon = Iconax.DollarCircle,
                    containerColor = scheme.tertiaryContainer,
                    contentColor = scheme.onTertiaryContainer,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** One stat: a tonal glyph beside a short label and its figure. Read as one unit by a screen reader. */
@Composable
private fun GlanceTile(
    label: String,
    value: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier
                .semantics(mergeDescendants = true) {}
                .padding(Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(Dimensions.Icon.avatar)
                    .clip(MaterialTheme.shapes.medium)
                    .background(containerColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(Dimensions.Icon.inline),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = value,
                    style = PennyWiseText.amountMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
            }
        }
    }
}

/** A count in the reader's own numerals and grouping. */
private fun Int.formatted(): String = NumberFormat.getIntegerInstance().format(this)

@Composable
private fun DashboardBalanceRows(summary: PersonLoanSummary) {
    val currencies = (summary.lentByCurrency.keys + summary.borrowedByCurrency.keys).sorted()
    if (currencies.isEmpty()) {
        PennyWiseCardV2(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.personal_dashboard_clear),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    GroupedList {
        currencies.forEachIndexed { index, currency ->
            GroupedRow(
                position = ListItemPosition.from(index, currencies.size),
                minHeight = Dimensions.Component.listItemMinHeightTwoLine,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(currency, style = MaterialTheme.typography.titleSmall)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        DashboardBalanceFigure(
                            stringResource(R.string.home_loans_label_owed_to_you),
                            summary.lentByCurrency[currency] ?: BigDecimal.ZERO,
                            currency,
                            MaterialTheme.colorScheme.income,
                        )
                        DashboardBalanceFigure(
                            stringResource(R.string.home_loans_label_you_owe),
                            summary.borrowedByCurrency[currency] ?: BigDecimal.ZERO,
                            currency,
                            MaterialTheme.colorScheme.expense,
                            true,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardBalanceFigure(
    label: String,
    amount: BigDecimal,
    currency: String,
    color: Color,
    alignEnd: Boolean = false,
) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(label, style = PennyWiseText.metadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(CurrencyFormatter.formatCurrency(amount, currency), style = PennyWiseText.amountMedium, color = color)
    }
}

/**
 * The contacts as a horizontally scrolling carousel of tiles in each contact's
 * own colour, the way Cashiro's profile lists them. "View all" opens the full list.
 */
@Composable
private fun DashboardPeopleSection(
    people: List<PersonWithSummary>,
    onViewAll: () -> Unit,
    onPersonClick: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Layout.headerToContent)) {
        SectionHeaderV2(
            title = stringResource(R.string.personal_dashboard_people),
            modifier = Modifier.padding(horizontal = Dimensions.Padding.content),
            action = {
                TextButton(onClick = onViewAll) {
                    Text(stringResource(R.string.personal_dashboard_view_all))
                }
            },
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            contentPadding = PaddingValues(horizontal = Dimensions.Padding.content),
        ) {
            items(people.take(DASHBOARD_PEOPLE_LIMIT), key = { it.person.id }) { row ->
                DashboardContactTile(
                    row = row,
                    onClick = { onPersonClick(row.person.id) },
                )
            }
        }
    }
}

/**
 * One contact: initials as a large watermark, the name and relationship at the
 * bottom and the open balance as a pill that stays legible on any colour. Only
 * the first currency is spelled out; the rest are counted, never dropped or added.
 */
@Composable
private fun DashboardContactTile(
    row: PersonWithSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val person = row.person
    val color = parseProfileColor(person.color, MaterialTheme.colorScheme.primary)
    val onColor = contentColorOn(color)

    PennyWiseCardV2(
        modifier = modifier.width(Dimensions.Component.listItemMinHeightTwoLine * CONTACT_TILE_WIDTH_MULTIPLIER),
        containerColor = color,
        // The fill is the container; an outline would only add a seam.
        border = BorderStroke(Dimensions.Component.hairline, Color.Transparent),
        onClick = onClick,
        contentPadding = Dimensions.Padding.none,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimensions.Component.listItemMinHeightTwoLine * CONTACT_TILE_HEIGHT_MULTIPLIER)
                .padding(Dimensions.Padding.cardCompact),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = person.initials(),
                modifier = Modifier.padding(top = Spacing.sm),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = onColor.copy(alpha = TILE_WATERMARK_ALPHA),
                maxLines = 1,
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
            ) {
                Text(
                    text = person.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = onColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = person.category ?: stringResource(R.string.people_contact),
                    style = MaterialTheme.typography.labelMedium,
                    color = onColor.copy(alpha = Dimensions.Alpha.subtitle),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Column(
                    modifier = Modifier.padding(top = Spacing.xs),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    ContactTileBalance(row)
                }
            }
        }
    }
}

@Composable
private fun ContactTileBalance(row: PersonWithSummary) {
    val entries = row.summary.netByCurrency.entries.sortedBy { it.key }
    val first = entries.firstOrNull()
    if (first == null) {
        TileBalancePill(
            text = stringResource(R.string.people_settled_up),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val positive = first.value.signum() > 0
    TileBalancePill(
        text = stringResource(
            if (positive) R.string.people_owed_to_you_amount else R.string.people_you_owe_amount,
            CurrencyFormatter.formatCurrency(first.value.abs(), first.key),
        ),
        // Owed to you is the income colour, what you owe the expense colour.
        color = if (positive) MaterialTheme.colorScheme.income else MaterialTheme.colorScheme.expense,
    )
    // A second open currency is never silently dropped.
    val hidden = entries.size - 1
    if (hidden > 0) {
        TileBalancePill(
            text = stringResource(R.string.people_more_balances, hidden),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A balance on a coloured tile: a near-opaque surface pill, so it reads on any contact colour. */
@Composable
private fun TileBalancePill(text: String, color: Color) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = Dimensions.Alpha.high),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
            style = PennyWiseText.amountSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

private fun String.dashboardInitials(): String {
    val words = trim().split(Regex("\\s+")).filter(String::isNotBlank)
    return when {
        words.isEmpty() -> "PW"
        words.size == 1 -> words.first().take(2).uppercase()
        else -> "${words.first().first()}${words.last().first()}".uppercase()
    }
}
