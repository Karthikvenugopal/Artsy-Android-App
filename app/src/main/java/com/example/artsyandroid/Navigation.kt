@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.artsyandroid

import android.content.Intent
import android.util.Log
import android.util.Patterns
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import com.example.artsyandroid.ui.theme.ArtsyAndroidTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

@Composable
fun MyApp() {
    ArtsyAndroidTheme {
        val context = LocalContext.current
        var loggedIn by remember { mutableStateOf(false) }
        var profileImage by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(Unit) {
            loggedIn = AuthManager.getToken(context) != null
            if (loggedIn) {
                profileImage = AuthManager.getProfileImage(context)
            }
        }
        val navController = rememberNavController()
        NavHost(navController, startDestination = "splash") {
            composable("splash") { SplashScreen(navController) }
            composable(
                "home?showLogoutSuccess={showLogoutSuccess}&showDeleteSuccess={showDeleteSuccess}",
                arguments = listOf(
                    navArgument("showLogoutSuccess") {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                    navArgument("showDeleteSuccess") {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                )
            ) { backStackEntry ->
                HomeScreen(
                    navController,
                    showLogoutSuccess = backStackEntry.arguments?.getBoolean("showLogoutSuccess") ?: false,
                    showDeleteSuccess = backStackEntry.arguments?.getBoolean("showDeleteSuccess") ?: false
                )
            }
            composable("search") { SearchScreen(navController) }
            composable("login") { LoginScreen(navController) }
            composable("register") { RegisterScreen(navController) }
            composable(
                "artistDetail/{artistId}",
                arguments = listOf(navArgument("artistId") { type = NavType.StringType })
            ) { backStackEntry ->
                ArtistDetailScreen(
                    backStackEntry.arguments!!.getString("artistId")!!,
                    navController
                )
            }
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
                .size(200.dp)
        )
    }
}


@Composable
fun HomeScreen(navController: NavController, showLogoutSuccess: Boolean = false, showDeleteSuccess: Boolean = false) {
    val context = LocalContext.current
    val loggedIn   = AuthManager.isLoggedIn()
    var favorites  by remember { mutableStateOf<List<FavoriteItem>>(emptyList()) }
    var favLoading by remember { mutableStateOf(false) }
    val todayString = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope       = rememberCoroutineScope()
    val profileUrl = AuthManager.getProfileImage(context)
    var menuOpen  by remember { mutableStateOf(false) }
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(showLogoutSuccess) {
        if (showLogoutSuccess) {
            snackbarHostState.showSnackbar("Logged out successfully")
            navController.currentBackStackEntry
                ?.arguments
                ?.putBoolean("showLogoutSuccess", false)
        }
    }
    LaunchedEffect(showDeleteSuccess) {
        if (showDeleteSuccess) {
            snackbarHostState.showSnackbar("Deleted user successfully")
            navController.currentBackStackEntry
                ?.arguments
                ?.putBoolean("showDeleteSuccess", false)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            now = Instant.now()
        }
    }

    LaunchedEffect(loggedIn) {
        if (loggedIn != false) {
            favLoading = true
            favorites = try {
                RetrofitInstance.api.getFavorites()
                    .body()
                    ?.favorites
                    .orEmpty()
            } catch (_: Exception) {
                emptyList()
            } finally {
                favLoading = false
            }
        } else {
            favorites = emptyList()
        }
    }



    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ),
                title = {
                        Text("Artist Search", modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp), textAlign = TextAlign.Start)
                },
                actions = {
                        IconButton(onClick = { navController.navigate("search") }) {
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
                                    onDismissRequest = { menuOpen = false },
                                ) {
                                    DropdownMenuItem(text = { Text("Log out") }, onClick = {
                                        scope.launch {
                                            AuthManager.clearToken(context)
                                            AuthManager.clearProfileImage(context)
                                            navController.navigate("home?showLogoutSuccess=true&showDeleteSuccess=false") {
                                                popUpTo("home") { inclusive = true }
                                            }
                                        }
                                    })
                                    DropdownMenuItem(text = { Text("Delete account", color = MaterialTheme.colorScheme.error) }, onClick = {
                                        scope.launch {
                                            val resp = RetrofitInstance.api.deleteAccount()
                                            if (resp.isSuccessful) {
                                                AuthManager.clearToken(context)
                                                AuthManager.clearProfileImage(context)
                                                navController.navigate("home?showLogoutSuccess=false&showDeleteSuccess=true") {
                                                    popUpTo("home") { inclusive = true }
                                                }
                                            }
                                        }
                                    })
                                }
                            }
                            }
                        else {
                                IconButton(onClick = { navController.navigate("login") }) {
                                Icon(Icons.Outlined.Person, contentDescription = "Log in")
                                }
                            }

                }
            )
        }
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = todayString,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(start = 16.dp)
                    )
                    Spacer(Modifier.height(4.dp))

                    Surface(
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

        if (isLoggedIn) {
        IconButton(
            onClick = { onToggleFavorite(artist.id) },
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 12.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(),
                    shape = CircleShape
                )
                .padding(6.dp)
        ) {
            Icon(
                painter = painterResource(
                    if (isFav)
                        R.drawable.baseline_star_24
                    else
                        R.drawable.outline_star_outline_24
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
    var searchText by rememberSaveable { mutableStateOf("") }
    var searchResults by rememberSaveable { mutableStateOf(emptyList<Artist>()) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var currentJob by remember { mutableStateOf<Job?>(null) }
    val debounceDelay = 300L

    val listState = rememberSaveable(saver = LazyListState.Saver) {
        LazyListState()
    }

    var favorites by remember { mutableStateOf<List<FavoriteItem>>(emptyList()) }
    LaunchedEffect(Unit) {
        favorites = RetrofitInstance.api.getFavorites()
            .body()?.favorites.orEmpty()
    }
    val favoriteIds = remember(favorites) { favorites.map { it.artistId }.toSet() }

    val snackbarHostState = remember { SnackbarHostState() }

    val container = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = container
                ),
                title = {
                    TextField(
                        value = searchText,
                        onValueChange = { txt ->
                            searchText = txt
                            currentJob?.cancel()
                            if (txt.length >= 3) {
                                currentJob = scope.launch {
                                    delay(debounceDelay)
                                    isLoading = true
                                    searchResults = try {
                                        RetrofitInstance.api.searchArtists(txt.trim())
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
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Search for an artist") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        },
                        trailingIcon = {
                            IconButton(onClick = {
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = true }
                                }
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor   = container,
                            unfocusedContainerColor = container,
                            disabledContainerColor  = container,
                            errorContainerColor     = container,
                            focusedIndicatorColor   = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(8.dp)
                .padding(innerPadding)
        ) {
            Spacer(Modifier.height(4.dp))

            when {
                isLoading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                }
                !isLoading && searchText.length >= 3 && searchResults.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(container)
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No Artists Found",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(searchResults) { artist ->
                            ArtistRow(
                                artist = artist,
                                isFav = artist.id in favoriteIds,
                                isLoggedIn = AuthManager.isLoggedIn(),
                                onToggleFavorite = { id ->
                                    scope.launch {
                                        val resp = RetrofitInstance.api.toggleFavorite(FavoriteRequest(id))
                                        if (resp.isSuccessful) {
                                            val newFavs = resp.body()?.favorites.orEmpty()
                                            favorites = newFavs
                                            snackbarHostState.showSnackbar(
                                                if (newFavs.any { it.artistId == id })
                                                    "Added to favorites"
                                                else
                                                    "Removed from favorites",
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    navController.navigate("artistDetail/${artist.id}") {
                                        popUpTo("search") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
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


    LaunchedEffect(artistId, isLoggedIn) {
        if (isLoggedIn) {
            try {
                val resp = RetrofitInstance.api.getFavorites()
                isFav = resp.body()?.favorites?.any { it.artistId == artistId } == true
            } catch (_: Exception) {
                Log.d("ArtistDetailScreen", "Error fetching favorites")
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

    data class TabItem(
        val icon: @Composable () -> Unit,
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
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
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
                                    favorites = resp.body()?.favorites.orEmpty()

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
                                    tab.icon()
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
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

@Composable
private fun GeneCard(
    gene: Gene,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            AsyncImage(
                model = gene.links.thumbnail?.href,
                contentDescription = gene.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.artsy_logo),
                error = painterResource(R.drawable.artsy_logo)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = gene.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(4.dp))
            LatexFixer(
                description = gene.description.orEmpty(),
            )
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun GenesCarousel(
    genes: List<Gene>,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 280.dp,
    cardHeight: Dp = 600.dp,
    spacing: Dp = 16.dp,
    sideInset: Dp = 27.dp
) {
    if (genes.isEmpty()) return

    val infiniteCount = Int.MAX_VALUE
    val startIndex    = infiniteCount / 2 - (infiniteCount / 2 % genes.size)
    val listState     = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    val scope         = rememberCoroutineScope()

    Box(modifier = modifier.height(cardHeight)) {
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(
                start = sideInset,
                end   = sideInset
            ),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            modifier = Modifier.fillMaxSize()
        ) {
            items(count = infiniteCount) { idx ->
                val gene = genes[idx % genes.size]
                GeneCard(
                    gene = gene,
                    modifier = Modifier
                        .width(cardWidth)
                        .height(cardHeight)
                )
            }
        }

        IconButton(
            onClick = {
                scope.launch {
                    listState.animateScrollToItem(listState.firstVisibleItemIndex - 1)
                }
            },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .zIndex(1f)
        ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous")
        }

        IconButton(
            onClick = {
                scope.launch {
                    listState.animateScrollToItem(listState.firstVisibleItemIndex + 1)
                }
            },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .zIndex(1f)
        ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
        }
    }
}


@Composable
fun ArtworksTab(artistId: String) {
    var artworks by remember { mutableStateOf<List<Artwork>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var showGenesDialog by remember { mutableStateOf(false) }
    var selectedArtworkId by remember { mutableStateOf<String?>(null) }
    var genesLoading by remember { mutableStateOf(false) }
    var genes by remember { mutableStateOf<List<Gene>>(emptyList()) }

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
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                .padding(vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No Artworks",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
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
                        text = art.title + ", " + art.date,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
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


        if (showGenesDialog && selectedArtworkId != null) {
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



            if (showGenesDialog && selectedArtworkId != null) {
                AlertDialog(
                    onDismissRequest = { showGenesDialog = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false),
                    modifier = Modifier
                        .fillMaxWidth(0.85f),
                    title = {
                        Text(
                            "Categories",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )
                    },
                    text = {
                        if (genesLoading) {
                            Box(Modifier.fillMaxWidth(), Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else if (genes.isEmpty()) {
                            Text(
                                "No categories available",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            GenesCarousel(
                                genes = genes,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    ,
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

    LaunchedEffect(artistId) {
        loading = true
        similars = try {
            RetrofitInstance.api
                .getSimilar(artistId)
                .body()
                ?.embedded
                ?.artists
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
    val snackbarHostState = remember { SnackbarHostState() }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
        TopAppBar(
            title = { Text("Log In") },
            navigationIcon = {
                IconButton(onClick = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }) {
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
                            showPwdError = true
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
            Button(
                onClick = {
                    submitAttempted = true
                    emailTouched = true
                    showPwdError = true
                    val emailEmpty   = email.isBlank()
                    val emailInvalid = email.isNotBlank() &&
                            !Patterns.EMAIL_ADDRESS.matcher(email).matches()
                    val pwdEmpty     = password.isBlank()

                    if (emailEmpty || emailInvalid || pwdEmpty) {
                        isLoading = false
                        return@Button
                    }

                    isLoading = true
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
                            snackbarHostState.showSnackbar(
                                "Logged in successfully",
                                duration = SnackbarDuration.Short
                            )
                            navController.navigate("home?showLoginSuccess=true") {
                                popUpTo("home") { inclusive = true }
                            }
                        } else {
                            isLoading = false
                            errorMessage = "Username or password is incorrect"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Log In")
                }
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
    val snackbarHostState = remember { SnackbarHostState() }
    var isRegistering by remember { mutableStateOf(false) }


    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
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
                            showPwdError = true
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
            Button(
                onClick = {
                    submitAttempted = true
                    nameTouched    = true
                    emailTouched   = true
                    showPwdError   = true
                    val nameEmpty    = fullName.isBlank()
                    val emailEmpty   = email.isBlank()
                    val emailInvalid = email.isNotBlank() &&
                            !Patterns.EMAIL_ADDRESS.matcher(email).matches()
                    val pwdEmpty     = password.isBlank()

                    if (nameEmpty || emailEmpty || emailInvalid || pwdEmpty) {
                        isRegistering = false
                        return@Button
                    }

                    isRegistering = true
                    showPwdError   = false

                    scope.launch {
                        val resp = try {
                            RetrofitInstance.api.register(
                                RegisterRequest(fullName, email, password)
                            )
                        } catch (_: Exception) {
                            null
                        }
                        if (resp?.isSuccessful == true) {
                            val auth = resp.body()!!
                            AuthManager.saveToken(context, auth.token)
                            AuthManager.saveProfileImage(context, auth.user.profileImageUrl)
                            snackbarHostState.showSnackbar(
                                "Registered successfully",
                                duration = SnackbarDuration.Short
                            )
                            navController.navigate("home?showLoginSuccess=true") {
                                popUpTo("home") { inclusive = true }
                            }
                        } else {
                            isRegistering = false
                            errorMessage   = "User already exists"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isRegistering) {
                    CircularProgressIndicator(
                        modifier   = Modifier.size(24.dp),
                        strokeWidth= 2.dp,
                        color      = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Register")
                }
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
        if (favorites.isEmpty()) {
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No favorites",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
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
    val then     = remember(fav.addedAt) { Instant.parse(fav.addedAt) }
    val relative = computeRelativeTime(then, now)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(fav.title, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${fav.nationality}, ${fav.birthday}",
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
    val diff = Duration.between(then, now).let { if (it.isNegative) Duration.ZERO else it }
    val secs = diff.seconds

    return when {
        secs <= 60 -> {
            "$secs second${if (secs == 1L) "" else "s"} ago"
        }
        secs < 3_600 -> {
            val mins = secs / 60
            "$mins minute${if (mins == 1L) "" else "s"} ago"
        }
        secs < 86_400 -> {
            val hours = secs / 3_600
            "$hours hour${if (hours == 1L) "" else "s"} ago"
        }
        else -> {
            val days = secs / 86_400
            "$days day${if (days == 1L) "" else "s"} ago"
        }
    }
}



@Composable
fun LatexFixer(description: String, modifier: Modifier = Modifier) {
    val uriH = LocalUriHandler.current
    val aString = buildAnnotatedString {
        val regex = Regex("""\[(.*?)\]\((.*?)\)""")
        var lIdx = 0
        for (i in regex.findAll(description)) {
            val r = i.range
            val label = i.groupValues[1]
            val ogUrl = i.groupValues[2]
            val completeUrl = "https://www.artsy.net$ogUrl"

            if (r.first > lIdx) {
                append(description.substring(lIdx, r.first))
            }

            pushStringAnnotation(tag = "URL", annotation = completeUrl)
            withStyle(style = SpanStyle(
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline
            )) {
                append(label)
            }
            pop()

            lIdx = r.last + 1
        }

        if (lIdx < description.length) {
            append(description.substring(lIdx))
        }
    }

    ClickableText(
        text = aString,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        onClick = { offset ->
            aString.getStringAnnotations("URL", offset, offset)
                .firstOrNull()?.let { uriH.openUri(it.item) }
        }
    )
}