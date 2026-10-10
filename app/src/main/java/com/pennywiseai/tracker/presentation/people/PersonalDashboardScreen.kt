package com.pennywiseai.tracker.presentation.people

import com.pennywiseai.tracker.ui.screens.settings.glassPanel
import com.pennywiseai.tracker.ui.screens.settings.GlassGroupedRow
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
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
import com.pennywiseai.tracker.ui.components.cards.IconTile
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.GlassCard
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
import com.pennywiseai.tracker.ui.theme.blue_dark
import com.pennywiseai.tracker.ui.theme.blue_light
import com.pennywiseai.tracker.ui.theme.expense
import com.pennywiseai.tracker.ui.theme.green_dark
import com.pennywiseai.tracker.ui.theme.green_light
import com.pennywiseai.tracker.ui.theme.orange_dark
import com.pennywiseai.tracker.ui.theme.orange_light
import com.pennywiseai.tracker.ui.theme.purple_dark
import com.pennywiseai.tracker.ui.theme.purple_light
import com.pennywiseai.tracker.ui.theme.red_dark
import com.pennywiseai.tracker.ui.theme.red_light
import com.pennywiseai.tracker.ui.theme.teal_dark
import com.pennywiseai.tracker.ui.theme.teal_light
import com.pennywiseai.tracker.ui.theme.income
import com.pennywiseai.tracker.utils.CurrencyFormatter
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import java.math.BigDecimal
import java.text.NumberFormat

/*
 * The profile page, laid out like Cashiro's: the user's own banner behind a
 * pinned top bar, a centred avatar with the name beneath it, a card of pastel
 * stat tiles, then the per-currency lend & borrow figures, a carousel of
 * contacts and the shortcuts.
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

/**
 * Contrast the contact tile's initials keep against the tile (WCAG large text):
 * still a quiet tone of the tile colour, but never a faint ghost.
 */
private const val TILE_INITIALS_MIN_CONTRAST = 3f

/** Where (as a fraction of the tile height) the contact tile's bottom fade begins. */
private const val TILE_FADE_START = 0.4f

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
    // The banner is its own haze source, so the glance card can frost it (as Home does).
    val hazeStateBanner = remember { HazeState() }
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // The banner paints behind the transparent top bar, exactly as on Home: a
        // user-chosen image wins over the cover style.
        val bannerUri = state.homeBannerUri
        if (bannerUri != null) {
            HomeBannerImage(
                imageUri = bannerUri,
                hazeStateBanner = hazeStateBanner,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        } else if (state.coverStyle != CoverStyle.NONE) {
            CoverGradientBanner(
                coverStyle = state.coverStyle,
                hazeStateBanner = hazeStateBanner,
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
                DashboardGlanceCard(state, hazeStateBanner, Modifier.padding(horizontal = gutter))
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

            // Contacts sit above the shortcuts, as on Cashiro's profile: the carousel
            // is the page's most personal content; the shortcuts are plain navigation.
            item(key = "people") {
                DashboardPeopleSection(
                    people = state.people,
                    onViewAll = onNavigateToContacts,
                    onPersonClick = onNavigateToPerson,
                )
            }

            item(key = "shortcuts") {
                Column(
                    modifier = Modifier.padding(horizontal = gutter),
                    verticalArrangement = Arrangement.spacedBy(Spacing.Layout.headerToContent),
                ) {
                    SectionHeaderV2(title = stringResource(R.string.personal_dashboard_shortcuts))
                    GroupedList {
                        GlassGroupedRow(position = ListItemPosition.Top, onClick = onNavigateToContacts) {
                            IconTile(Icons.Default.People, purple_light, purple_dark)
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
                        GlassGroupedRow(position = ListItemPosition.Bottom, onClick = onNavigateToLoans) {
                            IconTile(Icons.Default.SwapHoriz, teal_light, teal_dark)
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
        }
    }
}

/**
 * The centred identity block over the banner: avatar, name, and how many
 * contacts the user keeps (Cashiro shows a count in the same place). The edit
 * button lives in the top bar, so this block is purely display.
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
        Spacer(modifier = Modifier.height(Spacing.smd))
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
            text = pluralStringResource(
                R.plurals.profile_page_hero_contacts,
                state.people.size,
                state.people.size,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * The user's avatar: a preset drawable, their own photo, or their initials, on
 * their chosen swatch. Like Cashiro's it sits straight on the faded banner with
 * no ring, so it never reads as a dark halo over a light cover.
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
    val photoDescription = stringResource(R.string.edit_profile_photo_description)
    Box(
        modifier = modifier
            .size(HeroAvatarSize)
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
 * A card of four stat tiles (Cashiro's "Financial overview" card, with its pastel
 * glyph squares). They are all counts: what is owed in money is shown per
 * currency just below, so no figure is repeated and no amount is ever added
 * across currencies.
 */
@Composable
private fun DashboardGlanceCard(
    state: PersonalDashboardUiState,
    hazeStateBanner: HazeState,
    modifier: Modifier = Modifier,
) {
    val summary = state.loanSummary
    val currencyCount = (summary.lentByCurrency.keys + summary.borrowedByCurrency.keys).size

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        hazeState = hazeStateBanner,
        // One step below the tiles, so the tiles read as raised chips on the card.
        tint = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentPadding = Spacing.lg,
    ) {
        Text(
            text = stringResource(R.string.profile_page_glance_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Column(
            modifier = Modifier.padding(top = Spacing.smd),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                GlanceTile(
                    label = stringResource(R.string.profile_page_stat_lent),
                    value = state.lentRecordCount.formatted(),
                    icon = Iconax.DirectboxSend,
                    containerColor = green_light,
                    contentColor = green_dark,
                    modifier = Modifier.weight(1f),
                )
                GlanceTile(
                    label = stringResource(R.string.profile_page_stat_borrowed),
                    value = state.borrowedRecordCount.formatted(),
                    icon = Iconax.DirectboxReceive,
                    containerColor = red_light,
                    contentColor = red_dark,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                GlanceTile(
                    label = stringResource(R.string.profile_page_stat_settled),
                    value = state.settledRecordCount.formatted(),
                    icon = Icons.Default.CheckCircle,
                    containerColor = blue_light,
                    contentColor = blue_dark,
                    modifier = Modifier.weight(1f),
                )
                GlanceTile(
                    label = stringResource(R.string.profile_page_stat_currencies),
                    value = currencyCount.formatted(),
                    icon = Iconax.DollarCircle,
                    containerColor = orange_light,
                    contentColor = orange_dark,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * One stat: a pastel glyph square beside a short label and its figure. The pastel
 * pairs are the same fixed light/dark couples the Settings rows use, so they keep
 * their contrast in both themes. Read as one unit by a screen reader.
 */
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
        modifier = modifier.glassPanel(shape = MaterialTheme.shapes.large),
        shape = MaterialTheme.shapes.large,
        color = Color.Transparent,
    ) {
        Row(
            modifier = Modifier
                .semantics(mergeDescendants = true) {}
                .padding(Spacing.xs),
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
            Column(modifier = Modifier.weight(1f).padding(end = Spacing.xs)) {
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
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
            }
        }
    }
}

/** A count in the reader's own numerals and grouping. */
private fun Int.formatted(): String = NumberFormat.getIntegerInstance().format(this)

/**
 * One row per currency: a symbol badge and the code, then what is owed each way
 * side by side. When both directions are open, the row also shows the net for
 * that one currency (a same-currency difference, never a cross-currency sum).
 */
@Composable
private fun DashboardBalanceRows(summary: PersonLoanSummary) {
    val currencies = (summary.lentByCurrency.keys + summary.borrowedByCurrency.keys).sorted()
    if (currencies.isEmpty()) {
        GlassCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconTile(Icons.Default.CheckCircle, green_light, green_dark)
                Text(
                    text = stringResource(R.string.personal_dashboard_clear),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        return
    }
    GroupedList {
        currencies.forEachIndexed { index, currency ->
            val lent = summary.lentByCurrency[currency] ?: BigDecimal.ZERO
            val borrowed = summary.borrowedByCurrency[currency] ?: BigDecimal.ZERO
            GlassGroupedRow(
                position = ListItemPosition.from(index, currencies.size),
                minHeight = Dimensions.Component.listItemMinHeightTwoLine,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CurrencyBadge(currency)
                        Text(
                            text = currency,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        if (lent.signum() != 0 && borrowed.signum() != 0) {
                            val net = summary.netByCurrency[currency] ?: lent.subtract(borrowed)
                            NetPill(net, currency)
                        }
                    }
                    // Each figure takes half the row, so a long amount wraps in its
                    // own half instead of running into the other one.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        DashboardBalanceFigure(
                            label = stringResource(R.string.home_loans_label_owed_to_you),
                            amount = lent,
                            currency = currency,
                            color = MaterialTheme.colorScheme.income,
                            modifier = Modifier.weight(1f),
                        )
                        DashboardBalanceFigure(
                            label = stringResource(R.string.home_loans_label_you_owe),
                            amount = borrowed,
                            currency = currency,
                            color = MaterialTheme.colorScheme.expense,
                            alignEnd = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/** The currency's symbol in a small tonal circle; decorative, as the code sits beside it. */
@Composable
private fun CurrencyBadge(currency: String) {
    val symbol = CurrencyFormatter.getCurrencySymbol(currency).trim()
    Box(
        modifier = Modifier
            .size(Dimensions.Icon.large)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = symbol.ifEmpty { currency.take(1) },
            style = if (symbol.length <= 2) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 1,
        )
    }
}

/** The same-currency net, washed in the income or expense colour. */
@Composable
private fun NetPill(net: BigDecimal, currency: String) {
    val scheme = MaterialTheme.colorScheme
    val tone = when {
        net.signum() > 0 -> scheme.income
        net.signum() < 0 -> scheme.expense
        else -> scheme.onSurfaceVariant
    }
    val wash = tone.copy(alpha = Dimensions.Alpha.tonalIconContainer)
    val text = when {
        net.signum() > 0 -> stringResource(R.string.people_owed_to_you_amount, CurrencyFormatter.formatCurrency(net, currency))
        net.signum() < 0 -> stringResource(R.string.people_you_owe_amount, CurrencyFormatter.formatCurrency(net.abs(), currency))
        else -> stringResource(R.string.people_settled_up)
    }
    Surface(shape = CircleShape, color = wash) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
            style = PennyWiseText.amountSmall,
            fontWeight = FontWeight.SemiBold,
            color = tone.legibleOn(background = wash.compositeOver(scheme.surfaceContainerLow), towards = scheme.onSurface),
            maxLines = 1,
        )
    }
}

@Composable
private fun DashboardBalanceFigure(
    label: String,
    amount: BigDecimal,
    currency: String,
    color: Color,
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
    ) {
        Text(label, style = PennyWiseText.metadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = CurrencyFormatter.formatCurrency(amount, currency),
            style = PennyWiseText.amountMedium,
            color = color,
            textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
        )
    }
}

/**
 * The contacts as a horizontally scrolling carousel of tiles in each contact's
 * own colour, the way Cashiro's profile lists them. "View all" opens the full
 * list; with no contacts yet the section invites the user to add one instead of
 * disappearing.
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
            action = if (people.isEmpty()) {
                null
            } else {
                {
                    TextButton(onClick = onViewAll) {
                        Text(stringResource(R.string.personal_dashboard_view_all))
                    }
                }
            },
        )
        if (people.isEmpty()) {
            DashboardPeopleEmpty(
                onAddPerson = onViewAll,
                modifier = Modifier.padding(horizontal = Dimensions.Padding.content),
            )
        } else {
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
}

/** No contacts yet: one line of why, and a way to add the first one. */
@Composable
private fun DashboardPeopleEmpty(onAddPerson: () -> Unit, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTile(Icons.Default.People, purple_light, purple_dark)
            RowLabels(
                title = stringResource(R.string.people_empty_title),
                subtitle = stringResource(R.string.people_empty_description),
            )
        }
        FilledTonalButton(
            onClick = onAddPerson,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.md)
                .height(Dimensions.Component.buttonHeight),
        ) {
            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(Dimensions.Icon.inline))
            Spacer(Modifier.width(Spacing.sm))
            Text(stringResource(R.string.people_add_person))
        }
    }
}

/**
 * One contact, Cashiro-style: the contact's preset avatar (or their initials as a
 * large watermark) fills the tile, and a fade in the tile's colour keeps the name,
 * relationship and open balance legible at the bottom. Only the first currency
 * is spelled out; the rest are counted, never dropped or added.
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
    // The initials start from the tile's own hue and move toward its content
    // colour only as far as needed to read; a flat alpha washed them out.
    val initialsColor = remember(color, onColor) {
        color.legibleOn(background = color, towards = onColor, minContrast = TILE_INITIALS_MIN_CONTRAST)
    }
    val avatarRes = person.avatar?.let(AvatarHelper::resolveAvatarDrawable)

    GlassCard(
        modifier = modifier.width(Dimensions.Component.listItemMinHeightTwoLine * CONTACT_TILE_WIDTH_MULTIPLIER),
        shape = MaterialTheme.shapes.large,
        tint = color,
        onClick = onClick,
        contentPadding = Dimensions.Padding.none,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimensions.Component.listItemMinHeightTwoLine * CONTACT_TILE_HEIGHT_MULTIPLIER),
        ) {
            if (avatarRes != null) {
                Image(
                    painter = painterResource(avatarRes),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = person.initials(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = Spacing.lg),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = initialsColor,
                    maxLines = 1,
                )
            }
            // Fade the lower part into the tile colour so the text reads over a photo.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            TILE_FADE_START to Color.Transparent,
                            1f to color.copy(alpha = Dimensions.Alpha.high),
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(Dimensions.Padding.cardCompact),
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
