package com.rxplayer.pro.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.rxplayer.pro.core.theme.NovaDarkSurface
import com.rxplayer.pro.core.theme.NovaElectricCyan
import com.rxplayer.pro.core.theme.NovaTextSecondary
import com.rxplayer.pro.ui.navigation.Screen

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Videos : BottomNavItem(Screen.Videos.route, "Videos", Icons.Filled.Movie, Icons.Outlined.Movie)
    data object Folders : BottomNavItem(Screen.Folders.route, "Folders", Icons.Filled.Folder, Icons.Outlined.Folder)
    data object Music : BottomNavItem(Screen.Music.route, "Music", Icons.Filled.Audiotrack, Icons.Outlined.Audiotrack)
    data object Network : BottomNavItem(Screen.Network.route, "Network", Icons.Filled.Lan, Icons.Outlined.Lan)
    data object Playlists : BottomNavItem(Screen.Playlists.route, "Playlists", Icons.Filled.PlaylistPlay, Icons.Outlined.PlaylistPlay)
}

@Composable
fun NovaBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        BottomNavItem.Videos,
        BottomNavItem.Folders,
        BottomNavItem.Music,
        BottomNavItem.Network,
        BottomNavItem.Playlists
    )

    NavigationBar(
        containerColor = NovaDarkSurface,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (currentRoute != item.route) {
                        onNavigate(item.route)
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.title
                    )
                },
                label = { Text(item.title, style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = NovaElectricCyan,
                    selectedTextColor = NovaElectricCyan,
                    indicatorColor = NovaElectricCyan.copy(alpha = 0.22f),
                    unselectedIconColor = NovaTextSecondary,
                    unselectedTextColor = NovaTextSecondary
                )
            )
        }
    }
}
