package com.example.artsyandroid.network


data class FavoriteRequest(val artistId: String)
data class FavoriteItem(
    val artistId: String,
    val name: String,
    val birthday: String,
    val deathday: String,
    val nationality: String,
    val thumbnail: String,
    val addedAt: String
)
data class FavoritesResponse(val favorites: List<FavoriteItem>)