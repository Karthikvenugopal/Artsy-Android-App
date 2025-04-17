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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import com.example.artsyandroid.network.Artist
import com.example.artsyandroid.network.ArtistDetailResponse
import com.example.artsyandroid.network.RetrofitInstance
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.draw.clip
import com.example.artsyandroid.network.Artwork
import androidx.compose.ui.layout.ContentScale
import coil.request.ImageRequest
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.text.font.FontStyle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.graphics.Color



@Composable
fun MyApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") { SplashScreen(navController) }
        composable("home") { HomeScreen(navController) }
        composable("search") { SearchScreen(navController) }
        composable(
            "artistDetail/{artistId}",
            arguments = listOf(navArgument("artistId") { type = NavType.StringType; defaultValue = "" })
        ) { backStackEntry ->
            val artistId = backStackEntry.arguments?.getString("artistId") ?: ""
            ArtistDetailScreen(artistId, navController)
        }
    }
}

@Composable
fun SplashScreen(navController: NavController) {
    LaunchedEffect(Unit) {
        delay(2000)
        navController.navigate("home") {
            popUpTo("splash") { inclusive = true }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.artsy_logo),
            contentDescription = "Artsy",
            modifier = Modifier
                .size(200.dp)      // adjust as needed
        )
    }
}


@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current

    // 1) Date formatting
    val todayString = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
    }

    // 2) Search state
    var isSearching by remember { mutableStateOf(false) }
    var query       by remember { mutableStateOf("") }
    var results     by remember { mutableStateOf<List<Artist>>(emptyList()) }
    var isLoading   by remember { mutableStateOf(false) }
    val scope       = rememberCoroutineScope()
    var searchJob   by remember { mutableStateOf<Job?>(null) }
    val debounceMs  = 300L

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ),
                title = {
                    if (isSearching) {
                        TextField(
                            value = query,
                            onValueChange = { txt ->
                                query = txt
                                // Debounce + fire the search once >=3 chars
                                searchJob?.cancel()
                                if (txt.length >= 3) {
                                    searchJob = scope.launch {
                                        delay(debounceMs)
                                        isLoading = true
                                        try {
                                            val resp = RetrofitInstance.api.searchArtists(txt.trim())
                                            results = resp.body()?.embedded?.results.orEmpty()
                                        } catch (_: Exception) {
                                            results = emptyList()
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                } else {
                                    results = emptyList()
                                }
                            },
                            placeholder = { Text("Search artists…") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null)
                            },
                            trailingIcon = {
                                    IconButton(onClick = { query = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear")

                                }
                            },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor   = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                focusedIndicatorColor   = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text("Artist Search", modifier = Modifier.fillMaxWidth().padding(start = 4.dp), textAlign = TextAlign.Start)
                    }
                },
                actions = {
                    // only show the toggle‐search and profile icons when NOT searching
                    if (!isSearching) {
                        IconButton(onClick = {
                            isSearching = true
                            // reset any previous query/results:
                            query = ""
                            results = emptyList()
                            isLoading = false
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = { /* TODO: profile */ }) {
                            Icon(Icons.Outlined.Person, contentDescription = "User")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        // Body
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isSearching) {
                // --- SEARCH RESULTS UI ---
                if (isLoading) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                } else {
                    LazyColumn {
                        items(results) { artist ->
                            ArtistRow(artist = artist) {
                                navController.navigate("artistDetail/${it.links.self.href.substringAfterLast("/")}")
                            }
                        }
                    }
                }
            } else {
                // --- NORMAL HOME UI ---
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Date
                    Text(
                        text = todayString,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.Start).padding(start = 16.dp)
                    )
                    Spacer(Modifier.height(4.dp))

                    // Banner
                    Surface( // added style
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Favorites",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(vertical = 2.dp).fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(40.dp))
//Added style
                    // Login button
                    Button(onClick = { /* TODO */ }) {
                        Text("Log in to see favorites")
                    }

                    Spacer(Modifier.height(40.dp))

                    // Powered by Artsy
                    Text(
                        "Powered by Artsy",
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier
                            .clickable {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, "https://www.artsy.net/".toUri())
                                )
                            }
                            .padding(bottom = 16.dp)
                    )
                }
            }
        }
    }
}

// helper row for each artist result
@Composable
private fun ArtistRow(
    artist: Artist,
    onClick: (Artist) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick(artist) },
        elevation = CardDefaults.cardElevation(4.dp),
        shape     = RoundedCornerShape(8.dp)
    ) {
        Column {
            AsyncImage(
                model             = artist.links.thumbnail?.href,
                contentDescription = artist.title,                             // ← must come second
                placeholder       = painterResource(R.drawable.artsy_logo),
                error             = painterResource(R.drawable.artsy_logo),
                fallback          = painterResource(R.drawable.artsy_logo),
                modifier          = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentScale      = ContentScale.Crop
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(artist.title.orEmpty(), style = MaterialTheme.typography.bodyLarge)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
        }
    }
}



@Composable
fun SearchScreen(navController: NavController) {
    var searchText by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Artist>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var currentJob by remember { mutableStateOf<Job?>(null) }
    val debounceDelay = 300L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = searchText,
            onValueChange = { text ->
                searchText = text
                if (text.length >= 3) {
                    currentJob?.cancel()
                    currentJob = coroutineScope.launch {
                        delay(debounceDelay)
                        if (searchText.length >= 3) {
                            isLoading = true
                            val response = RetrofitInstance.api.searchArtists(searchText.trim())
                            searchResults = try {
                                if (response.isSuccessful) {
                                    response.body()?.embedded?.results ?: emptyList()
                                } else {
                                    emptyList()
                                }
                            } catch (_: Exception) {
                                emptyList()
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                } else {
                    searchResults = emptyList()
                }
            },
            singleLine = true,
            label = { Text("Search for an artist") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = { /* no-op */ }
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(searchResults) { artist ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .clickable { navController.navigate("artistDetail/${artist.id}") }
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(artist.links.thumbnail?.href)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = artist.title,
                                placeholder = painterResource(R.drawable.artsy_logo),
                                error       = painterResource(R.drawable.artsy_logo),
                                fallback    = painterResource(R.drawable.artsy_logo),
                                modifier    = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp),
                                contentScale = ContentScale.Crop
                            )
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = artist.title.orEmpty(),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Go to details"
                                )
                            }
                        }
                    }
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
    var tabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Details", "Artworks", "Similar")

    Column {
        TabRow(selectedTabIndex = tabIndex) {
            tabs.forEachIndexed { i, title ->
                Tab(
                    selected = i == tabIndex,
                    onClick = { tabIndex = i },
                    text = { Text(title) }
                )
            }
        }
        when (tabIndex) {
            0 -> DetailsTab(artistDetail)
            1 -> ArtworksTab(artistId)
            2 -> SimilarTab(artistId, navController)
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

@Composable
fun DetailsTab(detail: ArtistDetailResponse?) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        item {
            detail?.let { d ->
                Text("Name: ${d.name.orEmpty()}", style = MaterialTheme.typography.headlineSmall)
                Text("Nationality: ${d.nationality.orEmpty()}", style = MaterialTheme.typography.bodyMedium)
                Text("Birthday: ${d.birthday.orEmpty()}", style = MaterialTheme.typography.bodyMedium)
                d.deathday?.takeIf { it.isNotBlank() }?.let {
                    Text("Deathday: $it", style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(8.dp))
                Text(d.biography.orEmpty(), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}




@Composable
fun ArtworksTab(artistId: String) {
    var artworks by remember { mutableStateOf<List<Artwork>>(emptyList()) }
    var loading  by remember { mutableStateOf(true) }

    LaunchedEffect(artistId) {
        try {
            val resp = RetrofitInstance.api.getArtworks(artistId)
            artworks = resp.body()?.embedded?.artworks ?: emptyList()
        } catch (_: Exception) {
        } finally {
            loading = false
        }
    }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(Modifier.fillMaxSize().padding(8.dp)) {
            items(artworks) { art ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(Modifier.padding(8.dp)) {
                        AsyncImage(
                            model = art.links.image?.href
                                ?.replace("{image_version}", "medium"), // pick a version
                            contentDescription = art.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(art.title, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}


@Composable
fun SimilarTab(
    artistId:    String,
    navController: NavController
) {
    var similars by remember { mutableStateOf<List<Artist>>(emptyList()) }
    var loading  by remember { mutableStateOf(true) }

    LaunchedEffect(artistId) {
        try {
            val resp = RetrofitInstance.api.getSimilar(artistId)
            similars = resp.body()?.embedded?.results ?: emptyList()
        } catch (_: Exception) {
        } finally {
            loading = false
        }
    }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(Modifier.fillMaxSize().padding(8.dp)) {
            items(similars) { artist ->
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                        .clickable { navController.navigate("artistDetail/${artist.id}") },
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = artist.links.thumbnail?.href
                                ?: "", // fallback or missing‐image placeholder
                            contentDescription = artist.title,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(artist.title.orEmpty(), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}
