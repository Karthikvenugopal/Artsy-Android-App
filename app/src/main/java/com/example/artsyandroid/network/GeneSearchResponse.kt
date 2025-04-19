package com.example.artsyandroid.network

import com.google.gson.annotations.SerializedName

data class GeneSearchResponse(
    @SerializedName("_embedded") val embedded: GeneEmbedded
)

data class GeneEmbedded(
    @SerializedName("genes") val genes: List<Gene>
)

data class Gene(
    @SerializedName("id")          val id: String,
    @SerializedName("name")        val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("_links")      val links: GeneLinks
)

data class GeneLinks(
    @SerializedName("thumbnail") val thumbnail: Link?
)
