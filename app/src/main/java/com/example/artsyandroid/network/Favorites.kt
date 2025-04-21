package com.example.artsyandroid.network

import com.google.gson.annotations.SerializedName


data class FavoriteRequest(val artistId: String)
data class FavoriteItem(
    val artistId: String,
    @SerializedName("name") val title: String,
    val birthday: String,
    val deathday: String,
    val nationality: String,
    val thumbnail: String,
    val addedAt: String
)
data class FavoritesResponse(val favorites: List<FavoriteItem>)