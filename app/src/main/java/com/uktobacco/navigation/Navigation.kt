package com.uktobacco.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.uktobacco.ui.screens.*

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Brands : Screen("brands")
    object Favorites : Screen("favorites")
    object Settings : Screen("settings")
    object SmokingProfile : Screen("smoking_profile")
    object GlobalPrices : Screen("global_prices")
    object Awareness : Screen("awareness")
    object ProductDetail : Screen("product/{productId}") {
        fun createRoute(productId: String) = "product/$productId"
    }
    object CompanyHistory : Screen("company/{brandName}") {
        fun createRoute(brandName: String) = "company/$brandName"
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppNavigation(
    navController: NavHostController,
    viewModel: com.uktobacco.TobaccoViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { 1000 },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -1000 },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -1000 },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { 1000 },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onProductClick = { productId ->
                    navController.navigate(Screen.ProductDetail.createRoute(productId))
                },
                onBrandClick = { brandName ->
                    navController.navigate(Screen.CompanyHistory.createRoute(brandName))
                },
                viewModel = viewModel
            )
        }

        composable(Screen.Brands.route) {
            BrandsScreen(
                onBrandClick = { brandName ->
                    navController.navigate(Screen.CompanyHistory.createRoute(brandName))
                }
            )
        }

        composable(Screen.Favorites.route) {
            FavoritesScreen(
                onProductClick = { productId ->
                    navController.navigate(Screen.ProductDetail.createRoute(productId))
                },
                viewModel = viewModel
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateToProfile = {
                    navController.navigate(Screen.SmokingProfile.route)
                },
                onNavigateToGlobalPrices = {
                    navController.navigate(Screen.GlobalPrices.route)
                },
                onNavigateToAwareness = {
                    navController.navigate(Screen.Awareness.route)
                }
            )
        }

        composable(Screen.SmokingProfile.route) {
            SmokingProfileScreen(
                currentProfile = viewModel.getSmokingProfile(),
                onProfileSaved = { profile ->
                    viewModel.updateSmokingProfile(profile)
                }
            )
        }

        composable(Screen.GlobalPrices.route) {
            GlobalPricesScreen()
        }

        composable(Screen.Awareness.route) {
            AwarenessScreen()
        }

        composable(
            route = Screen.ProductDetail.route,
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: return@composable
            ProductDetailScreen(
                productId = productId,
                onBackClick = { navController.popBackStack() },
                onBrandClick = { brandName ->
                    navController.navigate(Screen.CompanyHistory.createRoute(brandName))
                },
                viewModel = viewModel
            )
        }

        composable(
            route = Screen.CompanyHistory.route,
            arguments = listOf(navArgument("brandName") { type = NavType.StringType })
        ) { backStackEntry ->
            val brandName = backStackEntry.arguments?.getString("brandName") ?: return@composable
            CompanyHistoryScreen(
                brandName = brandName,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
