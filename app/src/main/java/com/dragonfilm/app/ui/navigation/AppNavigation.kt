package com.dragonfilm.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.dragonfilm.app.data.model.Episode
import com.dragonfilm.app.data.model.Movie
import com.dragonfilm.app.data.repository.MovieRepository
import com.dragonfilm.app.data.storage.AnalyticsManager
import com.dragonfilm.app.data.storage.AuthManager
import com.dragonfilm.app.data.storage.CloudSync
import com.dragonfilm.app.data.storage.LocalStore
import com.dragonfilm.app.ui.detail.MovieDetailScreen
import com.dragonfilm.app.ui.home.HomeScreen
import com.dragonfilm.app.ui.library.LibraryScreen
import com.dragonfilm.app.ui.player.PlayerScreen
import com.dragonfilm.app.ui.profile.ProfileScreen
import com.dragonfilm.app.ui.schedule.ScheduleScreen
import com.dragonfilm.app.ui.search.SearchScreen
import com.dragonfilm.app.ui.theme.DFColor
import com.dragonfilm.app.util.SourceServer

sealed class AppDestination {
    data class Main(val tab: NavScreen = NavScreen.HOME) : AppDestination()
    data class Detail(val slug: String) : AppDestination()
    data class Player(
        val movie: Movie,
        val server: SourceServer,
        val episode: Episode,
        val allEpisodes: List<Episode>
    ) : AppDestination()
}

@Composable
fun AppNavigation(
    repository: MovieRepository,
    localStore: LocalStore,
    authManager: AuthManager,
    cloudSync: CloudSync,
    analyticsManager: AnalyticsManager? = null
) {
    var destinationStack by remember {
        mutableStateOf<List<AppDestination>>(listOf(AppDestination.Main(NavScreen.HOME)))
    }

    val currentDestination = destinationStack.lastOrNull() ?: AppDestination.Main(NavScreen.HOME)

    fun navigateTo(dest: AppDestination) {
        destinationStack = destinationStack + dest
    }

    fun popBack(): Boolean {
        if (destinationStack.size > 1) {
            destinationStack = destinationStack.dropLast(1)
            return true
        }
        return false
    }

    fun switchTab(tab: NavScreen) {
        destinationStack = listOf(AppDestination.Main(tab))
    }

    // Auto telemetry logging on destination change
    LaunchedEffect(currentDestination) {
        when (val dest = currentDestination) {
            is AppDestination.Main -> {
                analyticsManager?.trackScreen(dest.tab.route, "Tab: ${dest.tab.title}")
            }
            is AppDestination.Detail -> {
                analyticsManager?.trackMovieView(dest.slug, "Phim: ${dest.slug}")
            }
            is AppDestination.Player -> {
                analyticsManager?.trackWatchEpisode(
                    dest.movie.slug,
                    dest.movie.name,
                    dest.episode.name,
                    dest.server.displayName
                )
            }
        }
    }

    when (val dest = currentDestination) {
        is AppDestination.Player -> {
            PlayerScreen(
                movie = dest.movie,
                server = dest.server,
                initialEpisode = dest.episode,
                allEpisodes = dest.allEpisodes,
                localStore = localStore,
                cloudSync = cloudSync,
                onBack = { popBack() }
            )
        }

        is AppDestination.Detail -> {
            MovieDetailScreen(
                slug = dest.slug,
                repository = repository,
                localStore = localStore,
                authManager = authManager,
                onBack = { popBack() },
                onPlayEpisode = { movie, server, ep, allEps ->
                    navigateTo(AppDestination.Player(movie, server, ep, allEps))
                }
            )
        }

        is AppDestination.Main -> {
            Scaffold(
                containerColor = DFColor.Bg,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    BottomNavBar(
                        currentRoute = dest.tab.route,
                        onNavigate = { screen -> switchTab(screen) }
                    )
                }
            ) { _ ->
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    when (dest.tab) {
                        NavScreen.HOME -> HomeScreen(
                            repository = repository,
                            localStore = localStore,
                            onMovieClick = { slug -> navigateTo(AppDestination.Detail(slug)) },
                            onSearchClick = { switchTab(NavScreen.SEARCH) }
                        )

                        NavScreen.SCHEDULE -> ScheduleScreen(
                            repository = repository,
                            onMovieClick = { slug -> navigateTo(AppDestination.Detail(slug)) }
                        )

                        NavScreen.SEARCH -> SearchScreen(
                            repository = repository,
                            localStore = localStore,
                            onMovieClick = { slug -> navigateTo(AppDestination.Detail(slug)) }
                        )

                        NavScreen.LIBRARY -> LibraryScreen(
                            localStore = localStore,
                            onMovieClick = { slug -> navigateTo(AppDestination.Detail(slug)) }
                        )

                        NavScreen.PROFILE -> ProfileScreen(
                            authManager = authManager,
                            localStore = localStore,
                            cloudSync = cloudSync
                        )
                    }
                }
            }
        }
    }
}
