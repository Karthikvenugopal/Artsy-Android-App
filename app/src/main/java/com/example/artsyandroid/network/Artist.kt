package com.example.artsyandroid.network

import com.google.gson.annotations.SerializedName

// Represents an artist, from both search (which uses "title") and similar (which uses "name")
data class Artist(
    @SerializedName("title") private val titleField: String?,
    @SerializedName("name")  private val nameField: String?,
    @SerializedName("_links") val links: ArtistLinks
) {
    /** Use the search API’s `title` if present, otherwise fallback to the similar API’s `name`. */
    val title: String?
        get() = titleField ?: nameField

    // Compute the artist ID from the self link URL
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
