package com.m3u.subscription

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.m3u.ui.model.Icon
import com.m3u.core.util.context.toast
import com.m3u.data.entity.Live
import com.m3u.subscription.components.LiveItem
import com.m3u.ui.components.M3UDialog
import com.m3u.ui.model.LocalSpacing
import com.m3u.ui.model.LocalTheme
import com.m3u.ui.model.AppAction
import com.m3u.ui.model.SetActions
import com.m3u.ui.util.EventHandler
import com.m3u.ui.util.LifecycleEffect

@Composable
internal fun SubscriptionRoute(
    url: String,
    navigateToLive: (Int) -> Unit,
    setAppActions: SetActions,
    modifier: Modifier = Modifier,
    viewModel: SubscriptionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    val currentSetAppActions by rememberUpdatedState(setAppActions)
    LifecycleEffect { event ->
        when (event) {
            Lifecycle.Event.ON_START -> {
                val actions = listOf(
                    AppAction(
                        icon = Icon.ImageVectorIcon(Icons.Rounded.Refresh),
                        contentDescription = "refresh",
                        onClick = {
                            viewModel.onEvent(SubscriptionEvent.Sync)
                        }
                    )
                )
                currentSetAppActions(actions)
            }

            Lifecycle.Event.ON_PAUSE -> {
                currentSetAppActions(emptyList())
            }

            else -> {}
        }
    }

    val lives = state.lives
    val refreshing = state.syncing

    var dialogState: DialogState by remember {
        mutableStateOf(DialogState.Idle)
    }

    EventHandler(state.message) {
        context.toast(it)
    }

    LaunchedEffect(url) {
        viewModel.onEvent(SubscriptionEvent.GetDetails(url))
    }

    SubscriptionScreen(
        lives = lives,
        refreshing = refreshing,
        onSyncingLatest = { viewModel.onEvent(SubscriptionEvent.Sync) },
        navigateToLive = navigateToLive,
        onLiveAction = { dialogState = DialogState.Ready(it) },
        modifier = modifier
    )

    if (dialogState is DialogState.Ready) {
        M3UDialog(
            title = stringResource(R.string.dialog_favourite_title),
            text = stringResource(R.string.dialog_favourite_content),
            confirm = stringResource(R.string.dialog_favourite_confirm),
            dismiss = stringResource(R.string.dialog_favourite_dismiss),
            onDismissRequest = { dialogState = DialogState.Idle },
            onConfirm = {
                val current = dialogState
                if (current is DialogState.Ready) {
                    viewModel.onEvent(SubscriptionEvent.AddToFavourite(current.id))
                }
                dialogState = DialogState.Idle
            },
            onDismiss = { dialogState = DialogState.Idle }
        )
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
private fun SubscriptionScreen(
    lives: List<Live>,
    refreshing: Boolean,
    onSyncingLatest: () -> Unit,
    navigateToLive: (Int) -> Unit,
    onLiveAction: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val state = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = onSyncingLatest
    )
    Box(
        modifier = Modifier.pullRefresh(state)
    ) {
        when (configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(lives) { live ->
                        LiveItem(
                            live = live,
                            onClick = { navigateToLive(live.id) },
                            onLongClick = { onLiveAction(live.id) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Configuration.ORIENTATION_PORTRAIT -> {
                val groups = remember(lives) {
                    lives.groupBy { it.group }
                }
                LazyColumn(
                    modifier = modifier.fillMaxSize()
                ) {
                    groups.forEach { (group, lives) ->
                        stickyHeader {
                            Box(
                                modifier = Modifier
                                    .fillParentMaxWidth()
                                    .background(
                                        color = LocalTheme.current.surface
                                    )
                                    .padding(
                                        horizontal = LocalSpacing.current.medium,
                                        vertical = LocalSpacing.current.extraSmall
                                    )
                            ) {
                                Text(
                                    text = group,
                                    color = LocalTheme.current.onSurface,
                                    style = MaterialTheme.typography.subtitle2
                                )
                            }
                        }
                        itemsIndexed(lives) { index, live ->
                            LiveItem(
                                live = live,
                                onClick = { navigateToLive(live.id) },
                                onLongClick = { onLiveAction(live.id) },
                                modifier = Modifier.fillParentMaxWidth()
                            )
                            if (index == lives.lastIndex) {
                                Divider(
                                    modifier = Modifier.height(LocalSpacing.current.extraSmall)
                                )
                            }
                        }
                    }
                }
            }

            else -> {}
        }

        PullRefreshIndicator(
            refreshing = refreshing,
            state = state,
            modifier = Modifier.align(Alignment.TopCenter),
            scale = true,
            contentColor = LocalTheme.current.onTint,
            backgroundColor = LocalTheme.current.tint
        )
    }
}

private sealed class DialogState {
    object Idle : DialogState()
    data class Ready(val id: Int) : DialogState()
}