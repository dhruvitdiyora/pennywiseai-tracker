package com.pennywiseai.tracker.presentation.add

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.ui.components.PennyWiseScaffold
import com.pennywiseai.tracker.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScreen(
    viewModel: AddViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    PennyWiseScaffold(
        title = stringResource(R.string.add_title),
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.add_back)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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

    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        tabs.forEachIndexed { index, title ->
            SegmentedButton(
                selected = selectedTabIndex == index,
                onClick = { onSelectedTabChange(index) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size),
                modifier = Modifier.heightIn(min = Dimensions.Component.minTouchTarget),
                label = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            )
        }
    }
}
