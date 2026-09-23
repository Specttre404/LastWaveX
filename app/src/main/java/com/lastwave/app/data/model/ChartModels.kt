package com.lastwave.app.data.model

enum class ChartScope(val id: String, val formValue: String, val glValue: String, val label: String) {
    GLOBAL("global", "ZZ", "US", "Global"),
    INDIA("IN", "IN", "IN", "India");

    companion object {
        fun fromId(id: String?): ChartScope =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) || it.formValue.equals(id, ignoreCase = true) } ?: GLOBAL
    }
}

enum class ChartCategory(val id: String, val label: String) {
    ARTISTS("artists", "Top Artists");

    companion object {
        fun fromId(id: String?): ChartCategory = ARTISTS
    }
}

data class ChartEntry(
    val rank: Int,
    val previousRank: Int? = null,
    val movement: Int? = null,
    val title: String,
    val subtitle: String,
    val artworkUrl: String? = null,
    val videoId: String? = null,
    val artistBrowseId: String? = null,
    val scope: ChartScope = ChartScope.GLOBAL,
    val category: ChartCategory = ChartCategory.ARTISTS,
)

sealed interface ChartLoadResult {
    data class Success(val entries: List<ChartEntry>) : ChartLoadResult
    data object Empty : ChartLoadResult
    data object Unsupported : ChartLoadResult
    data class Error(val message: String) : ChartLoadResult
}
