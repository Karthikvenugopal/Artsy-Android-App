@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.artsyandroid

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import com.example.artsyandroid.network.Artist
import com.example.artsyandroid.network.ArtistDetailResponse
import com.example.artsyandroid.network.RetrofitInstance

@Composable
fun MyApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") { SplashScreen(navController) }
        composable("home") { HomeScreen(navController) }
        composable("search") { SearchScreen(navController) }
        composable(
            "artistDetail/{artistId}",
            arguments = listOf(navArgument("artistId") { type = NavType.StringType })
        ) { backStackEntry ->
            val artistId = backStackEntry.arguments?.getString("artistId") ?: ""
            ArtistDetailScreen(artistId, navController)
        }
    }
}

@Composable
fun SplashScreen(navController: NavController) {
    LaunchedEffect(key1 = true) {
        delay(2000)
        navController.navigate("home") {
            popUpTo("splash") { inclusive = true }
        }
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Splash Screen",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}

@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Home Screen",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { navController.navigate("search") }) {
                Text(text = "Go to Search")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Powered by Artsy",
                modifier = Modifier.clickable {
                    val intent = Intent(Intent.ACTION_VIEW, "https://www.artsy.net/".toUri())
                    context.startActivity(intent)
                },
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
fun SearchScreen(navController: NavController) {
    var searchText by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Artist>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = searchText,
            onValueChange = { text ->
                searchText = text
            },
            singleLine = true,
            label = { Text("Search for an artist") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    if (searchText.trim().length >= 3) {
                        isLoading = true
                        coroutineScope.launch {
                            Log.d("SearchScreen", "API call triggered for query: ${searchText.trim()}")
                            try {
                                val response = RetrofitInstance.api.searchArtists(searchText.trim())
                                if (response.isSuccessful) {
                                    // Access the results from embedded
                                    searchResults = response.body()?.embedded?.results ?: emptyList()
                                    Log.d("SearchScreen", "API call successful, results size: ${searchResults.size}")
                                } else {
                                    Log.e("SearchScreen", "API call failed with status: ${response.code()}")
                                    searchResults = emptyList()
                                }
                            } catch (e: Exception) {
                                Log.e("SearchScreen", "Exception during API call: ${e.localizedMessage}")
                                searchResults = emptyList()
                            } finally {
                                isLoading = false
                            }
                        }
                    } else {
                        searchResults = emptyList()
                    }
                }
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(searchResults) { artist ->
                    Text(
                        text = artist.title.orEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable {
                                // Extract ID from self link
                                val artistId = artist.links.self.href.split("/").last()
                                navController.navigate("artistDetail/$artistId")
                            }
                    )
                }
            }
        }
    }
}

@Composable
fun ArtistDetailScreen(artistId: String, navController: NavController) {
    Log.d("ArtistDetail", "ArtistDetailScreen launched with artistId: $artistId")
    var artistDetail by remember { mutableStateOf<ArtistDetailResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    // Fetch the artist details once when the screen appears.
    LaunchedEffect(artistId) {
        try {
            val response = RetrofitInstance.api.getArtistDetail(artistId)
            Log.d("ArtistDetail", "Response: ${response.body()}")
            if (response.isSuccessful) {
                artistDetail = response.body()
            } else {
                errorMessage = "Error fetching artist details (Status: ${response.code()})"
            }
        } catch (e: Exception) {
            errorMessage = e.message ?: "Unknown error"
            Log.e("ArtistDetail", "Exception: ${e.localizedMessage}")
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = artistDetail?.name.orEmpty().ifEmpty { "Loading..." },
                        maxLines = 1,
                        textAlign = TextAlign.Start
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> CircularProgressIndicator()
                errorMessage.isNotEmpty() -> Text(text = errorMessage)
                artistDetail != null -> Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Name: ${artistDetail?.name.orEmpty()}",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Nationality: ${artistDetail?.nationality.orEmpty()}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Birthday: ${artistDetail?.birthday.orEmpty()}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (!artistDetail?.deathday.isNullOrEmpty()) {
                        Text(
                            text = "Deathday: ${artistDetail?.deathday}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Text(
                        text = "Biography: ${artistDetail?.biography.orEmpty()}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                else -> Text(text = "No data available")
            }
        }
    }
}
