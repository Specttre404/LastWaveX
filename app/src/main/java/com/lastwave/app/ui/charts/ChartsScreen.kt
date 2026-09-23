package com.lastwave.app.ui.charts

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lastwave.app.data.model.ChartCategory
import com.lastwave.app.data.model.ChartEntry
import com.lastwave.app.data.model.ChartScope
import com.lastwave.app.playback.PlayableTrack
import com.lastwave.app.ui.common.ArtworkImage
import com.lastwave.app.ui.common.ExpressiveHeader
import com.lastwave.app.ui.common.ExpressiveLoadingIndicator
import com.lastwave.app.ui.common.adaptiveContentWidth
import com.lastwave.app.ui.common.safeHorizontalContentPadding
import com.lastwave.app.ui.player.LocalMiniPlayerScrollClearance
import com.lastwave.app.ui.player.LocalMusicPlayer
import androidx.compose.material.icons.filled.Person

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartsScreen(
    onBack: () -> Unit,
    onOpenArtist: (name: String, browseId: String?) -> Unit = { _, _ -> },
    viewModel: ChartsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val musicPlayer = LocalMusicPlayer.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .adaptiveContentWidth(maxWidth = 860.dp),
    ) {
        ExpressiveHeader(title = "Charts & Trending", onBack = onBack)

        // Scope Selector (Global / Country)
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(ChartScope.entries) { scope ->
                val selected = scope == uiState.scope
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.setScope(scope) },
                    label = { Text(scope.label) },
                    leadingIcon = if (scope == ChartScope.GLOBAL) {
                        { Icon(Icons.Filled.Public, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                )
            }
        }



        var top10Only by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            FilterChip(
                selected = !top10Only,
                onClick = { top10Only = false },
                label = { Text("Top 100") },
            )
            Spacer(Modifier.width(8.dp))
            FilterChip(
                selected = top10Only,
                onClick = { top10Only = true },
                label = { Text("Top 10") },
            )
        }

        val displayEntries = remember(uiState.entries, top10Only) {
            if (top10Only) uiState.entries.take(10) else uiState.entries
        }

        PullToRefreshBox(
            isRefreshing = uiState.status == ChartStatus.LOADING,
            onRefresh = viewModel::loadCharts,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            when (uiState.status) {
                ChartStatus.LOADING -> {
                    if (displayEntries.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            ExpressiveLoadingIndicator(message = "Loading charts...")
                        }
                    }
                }
                ChartStatus.UNSUPPORTED -> {
                    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "${uiState.category.label} unavailable for ${uiState.scope.label}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                ChartStatus.EMPTY -> {
                    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "No entries in ${uiState.category.label}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                ChartStatus.ERROR -> {
                    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            uiState.error ?: "Unable to load charts",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                ChartStatus.SUCCESS -> {
                    val playbackQueue = remember(displayEntries) {
                        displayEntries.mapNotNull { entry ->
                            if (entry.category == ChartCategory.ARTISTS) null
                            else PlayableTrack(
                                title = entry.title,
                                artist = entry.subtitle,
                                artworkUrl = entry.artworkUrl,
                            )
                        }
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 32.dp + LocalMiniPlayerScrollClearance.current,
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize().safeHorizontalContentPadding(),
                    ) {
                        items(displayEntries, key = { "chart_${it.rank}_${it.title}" }) { entry ->
                            ChartEntryRow(
                                entry = entry,
                                onClick = {
                                    if (entry.category == ChartCategory.ARTISTS) {
                                        onOpenArtist(entry.title, entry.artistBrowseId)
                                    } else {
                                        musicPlayer.playQueue(
                                            tracks = playbackQueue,
                                            startIndex = (entry.rank - 1).coerceIn(0, playbackQueue.lastIndex),
                                            sourceLabel = "${entry.scope.label} ${entry.category.label}",
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartEntryRow(
    entry: ChartEntry,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = if (entry.rank <= 3) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(36.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "#${entry.rank}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (entry.rank <= 3) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(if (entry.category == ChartCategory.ARTISTS) CircleShape else RoundedCornerShape(10.dp)),
            ) {
                ArtworkImage(
                    name = entry.title,
                    artist = entry.subtitle,
                    embeddedUrl = entry.artworkUrl,
                    fallbackIcon = if (entry.category == ChartCategory.ARTISTS) Icons.Filled.Person else Icons.AutoMirrored.Filled.QueueMusic,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        entry.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(6.dp))
                    com.lastwave.app.ui.common.ChartRankingBadge(entry)
                }
            }
            IconButton(onClick = onClick) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "Play",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}