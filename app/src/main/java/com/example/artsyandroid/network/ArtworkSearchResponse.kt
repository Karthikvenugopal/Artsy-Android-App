package com.example.artsyandroid.network

import com.google.gson.annotations.SerializedName

data class ArtworkSearchResponse(
    @SerializedName("total_count") val totalCount: Int?,
    @SerializedName("_links")     val links: ArtworkSearchLinks,
    @SerializedName("_embedded")  val embedded: ArtworkEmbedded
)

data class ArtworkSearchLinks(
    @SerializedName("self") val self: Link,
    @SerializedName("next") val next: Link?
)

data class ArtworkEmbedded(
    @SerializedName("artworks") val artworks: List<Artwork>
)

data class Artwork(
    @SerializedName("id")          val id: String,
    @SerializedName("slug")        val slug: String,
    @SerializedName("title")       val title: String,
    @SerializedName("category")    val category: String?,
    @SerializedName("medium")      val medium: String?,
    @SerializedName("date")        val date: String?,
    @SerializedName("dimensions")  val dimensions: Dimensions?,
    @SerializedName("_links")      val links: ArtworkLinks
)

data class Dimensions(
    @SerializedName("in") val inches: DimensionMeasurement?,
    @SerializedName("cm") val cm: DimensionMeasurement?
)

data class DimensionMeasurement(
    @SerializedName("text")     val text: String?,
    @SerializedName("height")   val height: Double?,
    @SerializedName("width")    val width: Double?,
    @SerializedName("depth")    val depth: Double?,
    @SerializedName("diameter") val diameter: Double?
)

data class ArtworkLinks(
    @SerializedName("thumbnail") val thumbnail: Link?,
    @SerializedName("image")     val image: HrefTemplate?,
    @SerializedName("self")      val self: Link,
    @SerializedName("permalink") val permalink: Link
)

data class HrefTemplate(
    @SerializedName("href")      val href: String,
    @SerializedName("templated") val templated: Boolean
)
