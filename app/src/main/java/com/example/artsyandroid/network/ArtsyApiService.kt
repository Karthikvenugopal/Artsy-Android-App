package com.example.artsyandroid.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface ArtsyApiService {
    @GET("api/artists/search/{query}")
    suspend fun searchArtists(
        @Path("query") query: String
    ): Response<ArtistSearchResponse>

    @GET("api/artists/{id}")
    suspend fun getArtistDetail(
        @Path("id") artistId: String
    ): Response<ArtistDetailResponse>
}
