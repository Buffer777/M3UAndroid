package com.m3u.smartphone.ui.business.channel.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import com.m3u.smartphone.ui.material.components.mask.Mask
import com.m3u.smartphone.ui.material.components.mask.MaskState
import com.m3u.smartphone.ui.material.model.LocalSpacing

@Composable
internal fun PlayerMask(
    state: MaskState,
    brush: Brush,
    header: @Composable RowScope.() -> Unit,
    body: @Composable RowScope.() -> Unit,
    footer: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    control: @Composable RowScope.() -> Unit = {},
    slider: (@Composable () -> Unit)? = null
) {
    val configuration = LocalConfiguration.current
    val spacing = LocalSpacing.current

    Mask(
        state = state,
        brush = brush,
        contentColor = Color.White,
        modifier = modifier.windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.medium)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.spacedBy(
                spacing.none,
                Alignment.End
            ),
            verticalAlignment = Alignment.Top,
            content = header
        )
        val centerSpacing = remember(configuration.screenWidthDp, spacing) {
            (configuration.screenWidthDp / 6).dp.coerceAtLeast(spacing.medium)
        }
        Row(
            modifier = Modifier
                .padding(horizontal = spacing.medium)
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(
                centerSpacing,
                Alignment.CenterHorizontally
            ),
            verticalAlignment = Alignment.CenterVertically,
            content = body
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    start = spacing.medium,
                    end = spacing.medium,
                    bottom = spacing.small
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom,
                content = control
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.medium),
                verticalAlignment = Alignment.Bottom,
                content = footer
            )
            AnimatedVisibility(
                visible = slider != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                slider?.invoke()
            }
        }
    }
}
