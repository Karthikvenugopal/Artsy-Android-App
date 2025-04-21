package com.example.artsyandroid.network

import com.google.gson.annotations.SerializedName

data class SimilarArtistsResponse(
    @SerializedName("_embedded") val embedded: SimilarEmbedded
)

data class SimilarEmbedded(
    @SerializedName("artists") val artists: List<Artist>
)
