@file:Suppress("UsingMaterialAndMaterial3Libraries")

package com.m3u.features.playlist

import android.Manifest
import android.content.res.Configuration.UI_MODE_TYPE_APPLIANCE
import android.content.res.Configuration.UI_MODE_TYPE_CAR
import android.content.res.Configuration.UI_MODE_TYPE_DESK
import android.content.res.Configuration.UI_MODE_TYPE_NORMAL
import android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
import android.content.res.Configuration.UI_MODE_TYPE_VR_HEADSET
import android.content.res.Configuration.UI_MODE_TYPE_WATCH
import android.os.Build
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowUp
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.m3u.core.architecture.pref.LocalPref
import com.m3u.core.util.basic.title
import com.m3u.core.wrapper.Event
import com.m3u.core.wrapper.Message
import com.m3u.data.database.model.Stream
import com.m3u.features.playlist.internal.PlaylistScreenImpl
import com.m3u.features.playlist.internal.TvPlaylistScreenImpl
import com.m3u.i18n.R.string
import com.m3u.material.components.Background
import com.m3u.material.ktx.interceptVolumeEvent
import com.m3u.material.ktx.isTelevision
import com.m3u.material.ktx.thenIf
import com.m3u.material.model.LocalSpacing
import com.m3u.ui.Destination
import com.m3u.ui.helper.Fob
import com.m3u.ui.helper.LocalHelper
import com.m3u.ui.Sort
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Composable
internal fun PlaylistRoute(
    navigateToStream: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlaylistViewModel = hiltViewModel(),
    contentPadding: PaddingValues = PaddingValues()
) {
    val context = LocalContext.current
    val pref = LocalPref.current
    val helper = LocalHelper.current

    val tv = isTelevision()

    val state by viewModel.state.collectAsStateWithLifecycle()
    val zapping by viewModel.zapping.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val playlistUrl by viewModel.playlistUrl.collectAsStateWithLifecycle()
    val playlist by viewModel.playlist.collectAsStateWithLifecycle()
    val channels by viewModel.channels.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()

    val sorts = viewModel.sorts
    val sort by viewModel.sort.collectAsStateWithLifecycle()

    val message by viewModel.message.collectAsStateWithLifecycle()

    // If you try to check or request the WRITE_EXTERNAL_STORAGE on Android 13+,
    // it will always return false.
    // So you'll have to skip the permission check/request completely on Android 13+.
    val writeExternalPermissionRequired = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU

    val writeExternalPermissionState = rememberPermissionState(
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    LifecycleStartEffect(playlist) {
        helper.title = playlist?.title?.title().orEmpty()
        helper.deep += 1
        onStopOrDispose {
            helper.deep -= 1
        }
    }

    LaunchedEffect(pref.autoRefresh, playlistUrl) {
        if (playlistUrl.isNotEmpty() && pref.autoRefresh) {
            viewModel.onEvent(PlaylistEvent.Refresh)
        }
    }

    BackHandler(query.isNotEmpty()) {
        viewModel.onEvent(PlaylistEvent.Query(""))
    }

    BackHandler(refreshing) {}

    Background {
        PlaylistScreen(
            title = playlist?.title.orEmpty(),
            message = message,
            query = query,
            onQuery = { viewModel.onEvent(PlaylistEvent.Query(it)) },
            rowCount = pref.rowCount,
            zapping = zapping,
            channels = channels,
            scrollUp = state.scrollUp,
            sorts = sorts,
            sort = sort,
            onSort = { viewModel.sort(it) },
            navigateToStream = navigateToStream,
            onScrollUp = { viewModel.onEvent(PlaylistEvent.ScrollUp) },
            onRefresh = { viewModel.onEvent(PlaylistEvent.Refresh) },
            contentPadding = contentPadding,
            onFavorite = { id, target -> viewModel.onEvent(PlaylistEvent.Favourite(id, target)) },
            ban = { id -> viewModel.onEvent(PlaylistEvent.Ban(id)) },
            savePicture = {
                if (writeExternalPermissionRequired && writeExternalPermissionState.status is PermissionStatus.Denied) {
                    writeExternalPermissionState.launchPermissionRequest()
                    return@PlaylistScreen
                }
                viewModel.onEvent(PlaylistEvent.SavePicture(it))
            },
            createShortcut = { viewModel.onEvent(PlaylistEvent.CreateShortcut(context, it)) },
            modifier = Modifier
                .fillMaxSize()
                .thenIf(!tv && pref.godMode) {
                    Modifier.interceptVolumeEvent { event ->
                        pref.rowCount = when (event) {
                            KeyEvent.KEYCODE_VOLUME_UP -> (pref.rowCount - 1).coerceAtLeast(1)
                            KeyEvent.KEYCODE_VOLUME_DOWN -> (pref.rowCount + 1).coerceAtMost(2)
                            else -> return@interceptVolumeEvent
                        }
                    }
                }
                .then(modifier)
        )
    }
}

@Composable
private fun PlaylistScreen(
    title: String,
    query: String,
    message: Message,
    onQuery: (String) -> Unit,
    rowCount: Int,
    zapping: Stream?,
    channels: ImmutableList<Channel>,
    sorts: ImmutableList<Sort>,
    sort: Sort,
    onSort: (Sort) -> Unit,
    scrollUp: Event<Unit>,
    onRefresh: () -> Unit,
    navigateToStream: () -> Unit,
    onScrollUp: () -> Unit,
    onFavorite: (streamId: Int, target: Boolean) -> Unit,
    ban: (streamId: Int) -> Unit,
    savePicture: (streamId: Int) -> Unit,
    createShortcut: (streamId: Int) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val helper = LocalHelper.current
    val currentOnScrollUp by rememberUpdatedState(onScrollUp)

    val isAtTopState = remember {
        mutableStateOf(true)
    }

    LaunchedEffect(Unit) {
        snapshotFlow { isAtTopState.value }
            .distinctUntilChanged()
            .onEach { newValue ->
                helper.fob = if (newValue) null
                else {
                    Fob(
                        icon = Icons.Rounded.KeyboardDoubleArrowUp,
                        rootDestination = Destination.Root.Foryou,
                        iconTextId = string.feat_playlist_scroll_up,
                        onClick = currentOnScrollUp
                    )
                }
            }
            .launchIn(this)
    }

    DisposableEffect(Unit) {
        onDispose { helper.fob = null }
    }

    val tv = isTelevision()
    if (!tv) {
        PlaylistScreenImpl(
            channels = channels,
            zapping = zapping,
            query = query,
            onQuery = onQuery,
            rowCount = rowCount,
            scrollUp = scrollUp,
            contentPadding = contentPadding,
            navigateToStream = navigateToStream,
            isAtTopState = isAtTopState,
            onRefresh = onRefresh,
            sorts = sorts,
            sort = sort,
            onSort = onSort,
            onFavorite = onFavorite,
            ban = ban,
            onSavePicture = savePicture,
            createShortcut = createShortcut,
            modifier = modifier
        )
    } else {
        TvPlaylistScreenImpl(
            title = title,
            message = message,
            channels = channels,
            query = query,
            onQuery = onQuery,
            navigateToStream = navigateToStream,
            onRefresh = onRefresh,
            sorts = sorts,
            sort = sort,
            onFavorite = onFavorite,
            ban = ban,
            savePicture = savePicture,
            createShortcut = createShortcut,
            modifier = modifier
        )
    }
}

@Composable
@Suppress("UNUSED")
private fun UnsupportedUIModeContent(
    type: Int,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    val spacing = LocalSpacing.current

    val device = remember(type) {
        when (type) {
            UI_MODE_TYPE_NORMAL -> "Normal"
            UI_MODE_TYPE_DESK -> "Desktop"
            UI_MODE_TYPE_CAR -> "Car"
            UI_MODE_TYPE_TELEVISION -> "Television"
            UI_MODE_TYPE_APPLIANCE -> "Appliance"
            UI_MODE_TYPE_WATCH -> "Watch"
            UI_MODE_TYPE_VR_HEADSET -> "VR-Headset"
            else -> "Device Type: $type"
        }
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            spacing.medium,
            Alignment.CenterVertically
        ),
        modifier = modifier.fillMaxSize()
    ) {
        Text("Unsupported UI Mode: $device")
        if (description != null) {
            Text(description)
        }
    }
}
