package com.example.artsyandroid.network

import com.google.gson.annotations.SerializedName

data class ArtistDetailResponse(
    @SerializedName("name") val name: String?,
    @SerializedName("nationality") val nationality: String?,
    @SerializedName("birthday") val birthday: String?,
    @SerializedName("deathday") val deathday: String?,
    @SerializedName("biography") val biography: String?
)
