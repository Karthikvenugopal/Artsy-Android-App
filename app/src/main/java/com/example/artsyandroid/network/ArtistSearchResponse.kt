package com.example.artsyandroid.network

import com.google.gson.annotations.SerializedName

data class ArtistSearchResponse(
    @SerializedName("total_count") val totalCount: Int,
    @SerializedName("offset") val offset: Int,
    @SerializedName("q") val query: String,
    @SerializedName("_links") val links: SearchLinks,
    @SerializedName("_embedded") val embedded: EmbeddedResults
)

data class SearchLinks(
    @SerializedName("self") val self: Link,
    @SerializedName("next") val next: Link?
)

data class EmbeddedResults(
    @SerializedName("results") val results: List<Artist>
)
