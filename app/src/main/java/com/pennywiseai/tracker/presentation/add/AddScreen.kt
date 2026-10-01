package com.pennywiseai.tracker.presentation.add

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
import com.pennywiseai.tracker.ui.components.TonalNavigationButton
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScreen(
    viewModel: AddViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollBehaviorLarge = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val hazeState = remember { HazeState() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehaviorLarge.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            CustomTitleTopAppBar(
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorLarge,
                title = stringResource(R.string.add_title),
                hasBackButton = true,
                navigationContent = {
                    TonalNavigationButton(
                        onClick = onNavigateBack,
                        contentDescription = stringResource(R.string.add_back)
                    )
                },
                hazeState = hazeState
            )
        }
    ) { paddingValues ->
        // Only the top inset is applied here: each tab runs edge to edge so its
        // sticky Save bar can sit behind the navigation bar and pad itself.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .background(MaterialTheme.colorScheme.background)
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            AddModeSwitcher(
                selectedTabIndex = pagerState.currentPage,
                onSelectedTabChange = { index ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                },
                modifier = Modifier.padding(
                    horizontal = Dimensions.Padding.content,
                    vertical = Spacing.sm
                )
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                when (page) {
                    0 -> TransactionTabContent(
                        viewModel = viewModel,
                        onSave = onNavigateBack
                    )
                    1 -> SubscriptionTabContent(
                        viewModel = viewModel,
                        onSave = onNavigateBack
                    )
                }
            }
        }
    }
}

/**
 * The Transaction / Subscription switcher at the top of the screen — Cashiro's
 * sliding pill. The options are `selectable` tabs, so each reports its selected
 * state to accessibility services.
 */
@Composable
internal fun AddModeSwitcher(
    selectedTabIndex: Int,
    onSelectedTabChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        stringResource(R.string.add_tab_transaction),
        stringResource(R.string.add_tab_subscription)
    )

    AddPillSwitcher(
        options = tabs,
        selectedIndex = selectedTabIndex,
        onIndexChange = onSelectedTabChange,
        modifier = modifier
    )
}
