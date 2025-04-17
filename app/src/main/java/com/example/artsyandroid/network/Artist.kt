package com.example.artsyandroid.network

import com.google.gson.annotations.SerializedName

data class Artist(
    @SerializedName("title") val title: String?,
    @SerializedName("_links") val links: ArtistLinks
)

data class ArtistLinks(
    @SerializedName("self") val self: Link,
    @SerializedName("thumbnail") val thumbnail: Link?
)

data class Link(
    @SerializedName("href") val href: String
)