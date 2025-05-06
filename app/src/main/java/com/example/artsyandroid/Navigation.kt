@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.artsyandroid

import android.content.Intent
import android.util.Log
import android.util.Patterns
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
import com.example.artsyandroid.network.Gene
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.Info
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.window.DialogProperties
import com.example.artsyandroid.auth.AuthManager
import com.example.artsyandroid.network.LoginRequest
import com.example.artsyandroid.network.RegisterRequest
import com.example.artsyandroid.network.FavoriteItem
import com.example.artsyandroid.network.FavoriteRequest
import java.time.Duration
import java.time.Instant
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.onFocusChanged


@Composable
fun MyApp() {
    val context = LocalContext.current
    var loggedIn       by remember { mutableStateOf(false) }
    var profileImage   by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        loggedIn = AuthManager.getToken(context) != null
        if (loggedIn) {
            profileImage = AuthManager.getProfileImage(context)
        }
    }
    val navController = rememberNavController()
    NavHost(navController, startDestination = "splash") {
        composable("splash")  { SplashScreen(navController) }
        composable("home")    { HomeScreen(navController) }
        composable("search")  { SearchScreen(navController) }
        composable("login")   { LoginScreen(navController) }
        composable("register"){ RegisterScreen(navController) }
        composable(
            "artistDetail/{artistId}",
            arguments = listOf(navArgument("artistId"){ type = NavType.StringType })
        ) { backStackEntry ->
            ArtistDetailScreen(
                backStackEntry.arguments!!.getString("artistId")!!,
                navController
            )
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
    val loggedIn   = AuthManager.isLoggedIn()
    var favorites  by remember { mutableStateOf<List<FavoriteItem>>(emptyList()) }
    var favLoading by remember { mutableStateOf(false) }
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
    val profileUrl = AuthManager.getProfileImage(context)
    var menuOpen  by remember { mutableStateOf(false) }
    var now by remember { mutableStateOf(Instant.now()) }
    val favoriteIds by remember(favorites) {
        derivedStateOf { favorites.map { it.artistId }.toSet() }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            now = Instant.now()
        }
    }

    LaunchedEffect(loggedIn) {
        if (!loggedIn) return@LaunchedEffect

        favLoading = true
        favorites = try {
            RetrofitInstance
                .api
                .getFavorites()
                .body()
                ?.favorites
                .orEmpty()
        } catch (_: Exception) {
            emptyList()
        } finally {
            favLoading = false
        }
    }



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
                        Text("Artist Search", modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp), textAlign = TextAlign.Start)
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
                        if (loggedIn && profileUrl != null) {
                            Box {
                                AsyncImage(
                                    model = profileUrl,
                                    contentDescription = "Profile",
                                    modifier = Modifier.size(36.dp).clip(CircleShape).clickable { menuOpen = true }
                                )
                                DropdownMenu(
                                    expanded = menuOpen,
                                    onDismissRequest = { menuOpen = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Log out") },
                                        onClick = {
                                            AuthManager.clearToken(context)
                                            AuthManager.clearProfileImage(context)
                                            menuOpen = false
                                            navController.navigate("home"){
                                                popUpTo("home"){ inclusive = true }
                                                }
                                            }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete account", color = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            menuOpen = false
                                            scope.launch {
                                                val resp = RetrofitInstance.api.deleteAccount()
                                                if (resp.isSuccessful) {
                                                    AuthManager.clearToken(context)
                                                    AuthManager.clearProfileImage(context)
                                                    navController.navigate("home"){
                                                        popUpTo("home"){ inclusive = true }
                                                    }
                                                    }
                                                }
                                            }
                                    )
                                }
                            }
                            } else {
                                IconButton(onClick = { navController.navigate("login") }) {
                                Icon(Icons.Outlined.Person, contentDescription = "Log in")
                                }
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
                            ArtistRow(
                                artist            = artist,
                                isFav             = favoriteIds.contains( artist.id ),
                                isLoggedIn = loggedIn,
                                onToggleFavorite  = { id ->
                                    // fire your toggle endpoint, then reload `favorites`
                                    scope.launch {
                                        val resp = RetrofitInstance.api.toggleFavorite(FavoriteRequest(id))
                                        if (resp.isSuccessful) {
                                            favorites = resp.body()?.favorites.orEmpty()
                                        }
                                    }
                                },
                                onClick           = {
                                    navController.navigate("artistDetail/${artist.id}")
                                }
                            )
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
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(start = 16.dp)
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
                            modifier = Modifier
                                .padding(vertical = 2.dp)
                                .fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(8.dp))
//Added style
                    // Login button
                    Log.d("Favorites button", "loggedIn: $loggedIn")

                    if (!loggedIn) {
                        Button(onClick = { navController.navigate("login") }) {
                            Text("Log in to see favorites")
                        }
                    } else if (favLoading) {
                        CircularProgressIndicator()
                    } else {
                        FavoritesSection(
                            favorites = favorites,
                            now = now,
                            onArtistClick = { artistId ->
                                navController.navigate("artistDetail/$artistId")
                            }
                        )
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
    isFav: Boolean,
    isLoggedIn: Boolean,
    onToggleFavorite: (String)->Unit,
    onClick: (Artist)->Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick(artist) },
            elevation = CardDefaults.cardElevation(4.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Box {
                AsyncImage(
                    model = artist.links.thumbnail?.href,
                    placeholder = painterResource(R.drawable.artsy_logo),
                    error = painterResource(R.drawable.artsy_logo),
                    fallback = painterResource(R.drawable.artsy_logo),
                    contentDescription = artist.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(188.dp),
                    contentScale = ContentScale.Crop
                )

                // Title overlay
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = artist.title.orEmpty(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // ───── the little round star button ─────
        if (isLoggedIn) {
        IconButton(
            onClick = { onToggleFavorite(artist.id) },
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 12.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    shape = CircleShape
                )
                .padding(6.dp)
        ) {
            Icon(
                painter = painterResource(
                    if (isFav)
                        R.drawable.baseline_star_24                // your filled‑star drawable
                    else
                        R.drawable.outline_star_outline_24          // your outline‑star XML
                ),
                contentDescription = if (isFav) "Unfavorite" else "Favorite",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(32.dp)
            )
        }
    }
    }
}

@Composable
fun SearchScreen(navController: NavController) {
    var searchText by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Artist>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var currentJob by remember { mutableStateOf<Job?>(null) }
    val debounceDelay = 300L


    // load favorites once so we can show snackbars
    var favorites by remember { mutableStateOf<List<FavoriteItem>>(emptyList()) }
    LaunchedEffect(Unit) {
        favorites = RetrofitInstance.api.getFavorites()
            .body()?.favorites.orEmpty()
    }
    val favoriteIds = remember(favorites) { favorites.map { it.artistId }.toSet() }

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { text ->
                    searchText = text
                    currentJob?.cancel()
                    if (text.length >= 3) {
                        currentJob = scope.launch {
                            delay(debounceDelay)
                            isLoading = true
                            searchResults = try {
                                RetrofitInstance.api.searchArtists(text.trim())
                                    .body()?.embedded?.results.orEmpty()
                            } catch (_: Exception) {
                                emptyList()
                            } finally {
                                isLoading = false
                            }
                        }
                    } else {
                        searchResults = emptyList()
                    }
                },
                singleLine = true,
                label = { Text("Search for an artist") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {}),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(searchResults) { artist ->
                        ArtistRow(
                            artist = artist,
                            isFav = artist.id in favoriteIds,
                            isLoggedIn = AuthManager.isLoggedIn(),
                            onToggleFavorite = { id ->
                                scope.launch { // Use the same coroutine scope
                                    val resp = RetrofitInstance
                                        .api
                                        .toggleFavorite(FavoriteRequest(id))
                                    if (resp.isSuccessful) {
                                        favorites = resp.body()?.favorites.orEmpty()
                                        val nowFav = id in favoriteIds
                                        // Show snackbar
                                        snackbarHostState.showSnackbar(
                                            if (nowFav) "Removed from favorites"
                                            else "Added to favorites"
                                        )
                                    } else {
                                        snackbarHostState.showSnackbar("Error toggling favorite")
                                    }
                                }
                            },
                            onClick = {
                                navController.navigate("artistDetail/${artist.id}")
                            }
                        )
                    }
                }
            }
        }
    }
}



@Composable
fun ArtistDetailScreen(
    artistId: String,
    navController: NavController
) {
    val isLoggedIn = AuthManager.isLoggedIn()
    var artistDetail by remember { mutableStateOf<ArtistDetailResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var tabIndex by remember { mutableIntStateOf(0) }
    var isFav     by remember { mutableStateOf(false) }
    val scope     = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }


    // on load, check if this artist is in favorites
    LaunchedEffect(artistId, isLoggedIn) {
        if (isLoggedIn) {
            try {
                val resp = RetrofitInstance.api.getFavorites()
                isFav = resp.body()?.favorites?.any { it.artistId == artistId } == true
            } catch (_: Exception) {
                // Handle error if needed, maybe log or show a message
            }
        }
    }

    var favorites by remember { mutableStateOf<List<FavoriteItem>>(emptyList()) }
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            favorites = try {
                RetrofitInstance.api.getFavorites().body()?.favorites.orEmpty()
            } catch(_: Exception) {
                emptyList()
            }
        }
    }
    val favoriteIds by remember(favorites) {
        derivedStateOf { favorites.map { it.artistId }.toSet() }
    }

    // Define tabs structure with icon painter
    data class TabItem(
        val icon: @Composable () -> Unit,  // Changed to composable icon
        val label: String
    )

    val tabs = listOfNotNull(
        TabItem(
            icon = { Icon(Icons.Outlined.Info, contentDescription = "Details") },
            label = "Details"
        ),
        TabItem(
            icon = { Icon(Icons.Outlined.AccountBox, contentDescription = "Artworks") },
            label = "Artworks"
        ),
        if (isLoggedIn) TabItem(
            icon = {
                Icon(
                    painter = painterResource(R.drawable.outline_person_search_24),
                    contentDescription = "Similar Artists"
                )
            },
            label = "Similar"
        ) else null
    )

    LaunchedEffect(artistId) {
        try {
            val resp = RetrofitInstance.api.getArtistDetail(artistId)
            if (resp.isSuccessful) artistDetail = resp.body()
            else errorMessage = "Error ${resp.code()}"
        } catch (e: Exception) {
            errorMessage = e.message ?: "Unknown error"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isLoggedIn) {
                        IconButton(onClick = {
                            scope.launch {
                                val resp = RetrofitInstance
                                    .api
                                    .toggleFavorite(FavoriteRequest(artistId))

                                if (resp.isSuccessful) {
                                    // 1) refresh your full favorites list:
                                    favorites = resp.body()?.favorites.orEmpty()

                                    // 2) recompute the detail‐screen star state
                                    isFav = artistId in favorites.map { it.artistId }

                                    snackbarHostState.showSnackbar(
                                        if (isFav) "Added to favorites" else "Removed from favorites"
                                    )
                                } else {
                                    snackbarHostState.showSnackbar("Error toggling favorite")
                                }
                            }
                        }) {
                            Icon(
                                painter = painterResource(
                                    if (isFav) R.drawable.baseline_star_24
                                    else         R.drawable.outline_star_outline_24
                                ),
                                contentDescription = null
                            )
                        }
                    }
                }

            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            when {
                isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                errorMessage.isNotEmpty() -> Text(errorMessage, Modifier.align(Alignment.Center))
                artistDetail != null -> Column {
                    TabRow(selectedTabIndex = tabIndex) {
                        tabs.forEachIndexed { index, tab ->
                            Tab(
                                selected = index == tabIndex,
                                onClick = { tabIndex = index }
                            ) {
                                Column(
                                    Modifier.padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    tab.icon()  // Use the composable icon
                                    Spacer(Modifier.height(4.dp))
                                    Text(tab.label, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    when (tabIndex) {
                        0 -> DetailsTab(artistDetail!!)
                        1 -> ArtworksTab(artistId)
                        2 -> SimilarTab(
                            artistId         = artistId,
                            navController    = navController,
                            favoriteIds      = favoriteIds,
                            onToggleFavorite = { id ->
                                scope.launch {
                                    val resp = RetrofitInstance.api.toggleFavorite(FavoriteRequest(id))
                                    if (resp.isSuccessful) {
                                        // refresh our local list
                                        favorites = resp.body()?.favorites.orEmpty()
                                        val justFavTitle = id in favoriteIds
                                        snackbarHostState.showSnackbar(
                                            if (!justFavTitle) "Removed from favorites" else "Added to favorites"
                                        )
                                    } else {
                                        snackbarHostState.showSnackbar("Error toggling favorite")
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun DetailsTab(detail: ArtistDetailResponse?) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            detail?.let { d ->
                Text(
                    text = d.name.orEmpty(),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${d.nationality.orEmpty()}, ${d.birthday.orEmpty()} – ${d.deathday.orEmpty()}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = d.biography.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start  // or Center if you prefer
                )
            }
        }
    }
}




@Composable
fun ArtworksTab(artistId: String) {
    var artworks by remember { mutableStateOf<List<Artwork>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    // state for the categories dialog:
    var showGenesDialog by remember { mutableStateOf(false) }
    var selectedArtworkId by remember { mutableStateOf<String?>(null) }
    var genesLoading by remember { mutableStateOf(false) }
    var genes by remember { mutableStateOf<List<Gene>>(emptyList()) }

//    val scope = rememberCoroutineScope()

    // 1) load artworks on first composition:
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
        return
    }

    if (artworks.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No Artworks",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        return
    }


    LazyColumn(Modifier
        .fillMaxSize()
        .padding(8.dp)) {
        items(artworks) { art ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column {
                    AsyncImage(
                        model = art.links.image
                            ?.href
                            ?.replace("{image_version}", "medium"),
                        contentDescription = art.title,
                        modifier = Modifier
                            .fillMaxWidth(),
                        contentScale = ContentScale.FillWidth
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = art.title,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = {
                            selectedArtworkId = art.id
                            showGenesDialog = true
                        },
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.CenterHorizontally)
                    ) {
                        Text("View categories")
                    }
                }
            }
        }
    }

    // 4) The categories dialog:
    if (showGenesDialog && selectedArtworkId != null) {
        LaunchedEffect(selectedArtworkId) {
            genesLoading = true
            genes = try {
                val resp = RetrofitInstance.api.getGenes(selectedArtworkId!!)
                resp.body()?.embedded?.genes.orEmpty()
            } catch (_: Exception) {
                emptyList()
            } finally {
                genesLoading = false
            }
        }

        // inside ArtworksTab, replace the existing AlertDialog with:

        if (showGenesDialog && selectedArtworkId != null) {
            // load genes as you already do…
            LaunchedEffect(selectedArtworkId) {
                genesLoading = true
                genes = try {
                    RetrofitInstance.api.getGenes(selectedArtworkId!!)
                        .body()
                        ?.embedded
                        ?.genes
                        .orEmpty()
                } catch (_: Exception) {
                    emptyList()
                } finally {
                    genesLoading = false
                }
            }

            // state to track which gene is showing
            var geneIndex by remember { mutableStateOf(0) }

//            val configuration = LocalConfiguration.current

            // In ArtworksTab composable - Updated AlertDialog section
            if (showGenesDialog && selectedArtworkId != null) {

                // at the top of your ArtworksTab, grab screen dims:
                val screenHeight = LocalConfiguration.current.screenHeightDp.dp
                val dialogHeight = screenHeight * 0.75f
                val carouselHeight = dialogHeight * 0.8f

                AlertDialog(
                    onDismissRequest = { showGenesDialog = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(dialogHeight),
                    title = {
                        Text(
                            "Categories",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )
                    },
                    text = {
                        if (genesLoading) {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else if (genes.isEmpty()) {
                            Text("No Categories Found")
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(carouselHeight),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { geneIndex = (geneIndex - 1 + genes.size) % genes.size },
                                    enabled = genes.size > 1
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous")
                                }

                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(horizontal = 4.dp),
                                    elevation = CardDefaults.cardElevation(4.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        AsyncImage(
                                            model = genes[geneIndex].links.thumbnail?.href,
                                            contentDescription = genes[geneIndex].name,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(180.dp),
                                            contentScale = ContentScale.Crop,
                                            placeholder = painterResource(R.drawable.artsy_logo),
                                            error       = painterResource(R.drawable.artsy_logo)
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            text = genes[geneIndex].name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = genes[geneIndex].description.orEmpty(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            textAlign = TextAlign.Justify,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                        Spacer(Modifier.weight(1f)) // empty space if description is short
                                    }
                                }

                                IconButton(
                                    onClick = { geneIndex = (geneIndex + 1) % genes.size },
                                    enabled = genes.size > 1
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = { showGenesDialog = false },
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Text("Close")
                            }
                        }
                    }
                )

            }
        }

    }
}




@Composable
fun SimilarTab(
    artistId:          String,
    navController:     NavController,
    favoriteIds:       Set<String>,
    onToggleFavorite:  (String) -> Unit
) {
    var similars by remember { mutableStateOf<List<Artist>>(emptyList()) }
    var loading  by remember { mutableStateOf(true) }

    // fetch similar artists
    LaunchedEffect(artistId) {
        loading = true
        similars = try {
            RetrofitInstance.api
                .getSimilar(artistId)
                .body()
                ?.embedded
                ?.artists        // <-- now matches your JSON
                .orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
        loading = false
    }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(Modifier.fillMaxSize().padding(8.dp)) {
            items(similars) { artist ->
                Log.d("ArtistRow", "Artist: $artist")
                ArtistRow(
                    artist            = artist,
                    isFav             = artist.id in favoriteIds,
                    isLoggedIn = AuthManager.isLoggedIn(),
                    onToggleFavorite  = onToggleFavorite,
                    onClick           = { navController.navigate("artistDetail/${artist.id}") }
                )
            }
        }
    }
}


@Composable
fun LoginScreen(navController: NavController) {
    var email           by remember { mutableStateOf("") }
    var emailTouched    by remember { mutableStateOf(false) }
    var password        by remember { mutableStateOf("") }
    var showPwdError    by remember { mutableStateOf(false) }
    var submitAttempted by remember { mutableStateOf(false) }
    var errorMessage    by remember { mutableStateOf<String?>(null) }
    val scope           = rememberCoroutineScope()
    val context         = LocalContext.current

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Log In") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )
    }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(padding),
            verticalArrangement = Arrangement.Center
        ) {
            // ─── Email ────────────────────────────────────────────────────────────
            OutlinedTextField(
                value         = email,
                onValueChange = {
                    email = it
                    errorMessage = null
                },
                label     = { Text("Email") },
                isError   = (emailTouched && email.isBlank())
                        || (submitAttempted
                        && email.isNotBlank()
                        && !Patterns.EMAIL_ADDRESS.matcher(email).matches()),
                singleLine = true,
                modifier    = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { fs -> if (fs.isFocused) emailTouched = true }
            )
            if (emailTouched && email.isBlank()) {
                Text(
                    "Email cannot be empty",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            } else if (submitAttempted
                && email.isNotBlank()
                && !Patterns.EMAIL_ADDRESS.matcher(email).matches()
            ) {
                Text(
                    "Invalid email format",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            // ─── Password ─────────────────────────────────────────────────────────
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                label = { Text("Password") },
                isError = (showPwdError && password.isBlank()) || (submitAttempted && password.isBlank()),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { fs ->
                        if (fs.isFocused) {
                            showPwdError = true // Set to true on focus
                        }
                    }
            )
            if ((showPwdError || submitAttempted) && password.isBlank()) {
                Text(
                    "Password cannot be empty",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            // ─── Login Button ─────────────────────────────────────────────────────
            Button(
                onClick = {
                    submitAttempted = true
                    emailTouched    = true
                    showPwdError    = true

                    val emailEmpty   = email.isBlank()
                    val emailInvalid = email.isNotBlank()
                            && !Patterns.EMAIL_ADDRESS.matcher(email).matches()
                    val pwdEmpty     = password.isBlank()

                    if (emailEmpty || emailInvalid || pwdEmpty) {
                        return@Button
                    }
                    showPwdError = false

                    scope.launch {
                        val resp = try {
                            RetrofitInstance.api.login(LoginRequest(email, password))
                        } catch (_: Exception) {
                            null
                        }
                        if (resp?.isSuccessful == true) {
                            val auth = resp.body()!!
                            AuthManager.saveToken(context, auth.token)
                            AuthManager.saveProfileImage(context, auth.user.profileImageUrl)
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = true }
                            }
                        } else {
                            errorMessage = "Username or password is incorrect"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Log In")
            }

            errorMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Don't have an account? ",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text("Register",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .clickable { navController.navigate("register") }
                        .padding(start = 2.dp)
                )
            }
        }
    }
}

@Composable
fun RegisterScreen(navController: NavController) {
    var fullName        by remember { mutableStateOf("") }
    var nameTouched     by remember { mutableStateOf(false) }
    var email           by remember { mutableStateOf("") }
    var emailTouched    by remember { mutableStateOf(false) }
    var password        by remember { mutableStateOf("") }
    var showPwdError    by remember { mutableStateOf(false) }
    var submitAttempted by remember { mutableStateOf(false) }
    var errorMessage    by remember { mutableStateOf<String?>(null) }
    val scope           = rememberCoroutineScope()
    val context         = LocalContext.current

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Register") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )
    }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(padding),
            verticalArrangement = Arrangement.Center
        ) {
            // ─── Full Name ───────────────────────────────────────────────────────
            OutlinedTextField(
                value         = fullName,
                onValueChange = {
                    fullName = it
                    errorMessage = null
                },
                label     = { Text("Full Name") },
                isError   = nameTouched && fullName.isBlank(),
                singleLine = true,
                modifier    = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { fs -> if (fs.isFocused) nameTouched = true }
            )
            if (nameTouched && fullName.isBlank()) {
                Text(
                    "Full name cannot be empty",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            // ─── Email ─────────────────────────────────────────────────────────
            OutlinedTextField(
                value         = email,
                onValueChange = {
                    email = it
                    errorMessage = null
                },
                label     = { Text("Email") },
                isError   = (emailTouched && email.isBlank())
                        || (submitAttempted
                        && email.isNotBlank()
                        && !Patterns.EMAIL_ADDRESS.matcher(email).matches()),
                singleLine = true,
                modifier    = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { fs -> if (fs.isFocused) emailTouched = true }
            )
            if (emailTouched && email.isBlank()) {
                Text(
                    "Email cannot be empty",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            } else if (submitAttempted
                && email.isNotBlank()
                && !Patterns.EMAIL_ADDRESS.matcher(email).matches()
            ) {
                Text(
                    "Invalid email format",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            // ─── Password ───────────────────────────────────────────────────────
            // ─── Password ─────────────────────────────────────────────────────────
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                label = { Text("Password") },
                isError = (showPwdError && password.isBlank()) || (submitAttempted && password.isBlank()),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { fs ->
                        if (fs.isFocused) {
                            showPwdError = true // Set to true on focus
                        }
                    }
            )
            if ((showPwdError || submitAttempted) && password.isBlank()) {
                Text(
                    "Password cannot be empty",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            // ─── Register Button ────────────────────────────────────────────────
            Button(
                onClick = {
                    submitAttempted = true
                    nameTouched     = true
                    emailTouched    = true
                    showPwdError    = true

                    val nameEmpty    = fullName.isBlank()
                    val emailEmpty   = email.isBlank()
                    val emailInvalid = email.isNotBlank()
                            && !Patterns.EMAIL_ADDRESS.matcher(email).matches()
                    val pwdEmpty     = password.isBlank()

                    if (nameEmpty || emailEmpty || emailInvalid || pwdEmpty) {
                        return@Button
                    }
                    showPwdError = false

                    scope.launch {
                        val resp = try {
                            RetrofitInstance.api.register(RegisterRequest(fullName, email, password))
                        } catch (_: Exception) {
                            null
                        }
                        if (resp?.isSuccessful == true) {
                            val auth = resp.body()!!
                            AuthManager.saveToken(context, auth.token)
                            AuthManager.saveProfileImage(context, auth.user.profileImageUrl)
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = true }
                            }
                        } else {
                            errorMessage = "Username or password is incorrect"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Register")
            }

            errorMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Already have an account? ",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text("Login",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .clickable { navController.navigate("login") }
                        .padding(start = 2.dp)
                )
            }
        }
    }
}


@Composable
fun FavoritesSection(
    favorites: List<FavoriteItem>,
    now: Instant,
    onArtistClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Section header
//        Text(
//            text = "Favorites",
//            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(vertical = 8.dp),
//            textAlign = TextAlign.Center
//        )

        if (favorites.isEmpty()) {
            Text(
                "No favorites yet",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                textAlign = TextAlign.Center
            )
        } else {
            LazyColumn {
                items(favorites) { fav ->
                    FavoriteArtistListItem(
                        fav = fav,
                        now = now,
                        onClick = { onArtistClick(fav.artistId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteArtistListItem(
    fav: FavoriteItem,
    now: Instant,
    onClick: () -> Unit
) {
    // parse once
    val then     = remember(fav.addedAt) { Instant.parse(fav.addedAt) }
    val relative = computeRelativeTime(then, now)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() }
            // if you still want a subtle background, you can uncomment:
            // .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .padding(12.dp),  // inner padding that used to live on the Card
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(fav.title, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${fav.nationality}, b. ${fav.birthday}",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = relative,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(end = 8.dp)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Go to details"
            )
        }
    }
}


private fun computeRelativeTime(then: Instant, now: Instant): String {
    val d = Duration.between(then, now)
    return when {
        d.seconds < 60   -> "${d.seconds} second${if (d.seconds==1L) "" else "s"} ago"
        d.toMinutes()<60 -> "${d.toMinutes()} minute${if (d.toMinutes()==1L) "" else "s"} ago"
        d.toHours() <24  -> "${d.toHours()} hour${if (d.toHours()==1L) "" else "s"} ago"
        else             -> "${d.toDays()} day${if (d.toDays()==1L) "" else "s"} ago"
    }
}