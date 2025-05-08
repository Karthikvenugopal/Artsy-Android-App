package com.example.artsyandroid.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
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

    @GET("api/artists/artwork/{artist_id}")
    suspend fun getArtworks(
        @Path("artist_id") artistId: String
    ): Response<ArtworkSearchResponse>

    @GET("api/artists/similar/{artist_id}")
    suspend fun getSimilar(
        @Path("artist_id") artistId: String
    ): Response<SimilarArtistsResponse>

    @GET("api/artists/genes/{artwork_id}")
    suspend fun getGenes(
        @Path("artwork_id") artworkId: String
    ): Response<GeneSearchResponse>

    @POST("api/auth/login")
    suspend fun login(@Body req: LoginRequest): Response<AuthResponse>

    @POST("api/auth/register")
    suspend fun register(@Body req: RegisterRequest): Response<AuthResponse>

    @DELETE("api/auth/delete-account")
    suspend fun deleteAccount(): Response<Unit>

    @GET("api/artists/favorites/saved")
    suspend fun getFavorites(): Response<FavoritesResponse>

    @POST("api/artists/favorites")
    suspend fun toggleFavorite(@Body req: FavoriteRequest): Response<FavoritesResponse>

}
