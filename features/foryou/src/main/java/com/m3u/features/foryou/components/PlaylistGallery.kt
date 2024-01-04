package com.m3u.features.foryou.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import com.m3u.data.database.entity.Playlist
import com.m3u.features.foryou.model.PlaylistDetail
import com.m3u.i18n.R.string
import com.m3u.material.ktx.plus
import com.m3u.material.model.LocalSpacing
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun PlaylistGallery(
    rowCount: Int,
    details: ImmutableList<PlaylistDetail>,
    navigateToPlaylist: (Playlist) -> Unit,
    onMenu: (Playlist) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    LazyVerticalGrid(
        columns = GridCells.Fixed(rowCount),
        contentPadding = PaddingValues(LocalSpacing.current.medium) + contentPadding,
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(spacing.medium),
        modifier = modifier.fillMaxSize()
    ) {
        items(
            items = details,
            key = { it.playlist.url },
            contentType = {}
        ) { detail ->
            PlaylistItem(
                label = detail.playlist.calculateUiTitle(),
                number = detail.count,
                local = detail.playlist.local,
                modifier = Modifier.fillMaxWidth(),
                onClick = { navigateToPlaylist(detail.playlist) },
                onLongClick = { onMenu(detail.playlist) }
            )
        }
    }
}

@Composable
private fun Playlist.calculateUiTitle(): AnnotatedString {
    val actual = title.ifEmpty {
        if (local) stringResource(string.feat_foryou_imported_playlist_title)
        else ""
    }
    return AnnotatedString(actual.uppercase())
}