package com.example.artsyandroid.network

import com.google.gson.annotations.SerializedName

data class Artist(
    @SerializedName("title") private val titleField: String?,
    @SerializedName("name")  private val nameField: String?,
    @SerializedName("_links") val links: ArtistLinks
) {
    val title: String?
        get() = titleField ?: nameField

    val id: String
        get() = links.self.href.substringAfterLast("/")
}

data class ArtistLinks(
    @SerializedName("self")      val self: Link,
    @SerializedName("thumbnail") val thumbnail: Link?
)

data class Link(
    @SerializedName("href") val href: String
)
