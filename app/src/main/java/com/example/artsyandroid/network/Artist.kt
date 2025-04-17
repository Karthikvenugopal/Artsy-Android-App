package com.example.artsyandroid.network

import com.google.gson.annotations.SerializedName

// Represents an artist search result, extracting 'title' and links to derive the ID
data class Artist(
    @SerializedName("title") val title: String?,
    @SerializedName("_links") val links: ArtistLinks
) {
    // Compute the artist ID from the self link URL
    val id: String
        get() = links.self.href.substringAfterLast("/")
}

data class ArtistLinks(
    @SerializedName("self") val self: Link,
    @SerializedName("thumbnail") val thumbnail: Link?
)

data class Link(
    @SerializedName("href") val href: String
)
