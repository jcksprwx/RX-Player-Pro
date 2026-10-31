package com.rxplayer.pro.ui.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Onboarding : Screen("onboarding")
    data object Permission : Screen("permission")
    data object Home : Screen("home")
    data object Videos : Screen("videos")
    data object Folders : Screen("folders")
    data object FolderDetail : Screen("folder_detail/{folderPath}/{folderName}") {
        fun createRoute(folderPath: String, folderName: String) =
            "folder_detail/${java.net.URLEncoder.encode(folderPath, "UTF-8")}/${java.net.URLEncoder.encode(folderName, "UTF-8")}"
    }
    data object Music : Screen("music")
    data object MusicPlayer : Screen("music_player")
    data object VideoPlayer : Screen("video_player/{mediaId}") {
        fun createRoute(mediaId: Long) = "video_player/$mediaId"
    }
    data object Equalizer : Screen("equalizer")
    data object Network : Screen("network")
    data object AddServer : Screen("add_server")
    data object Playlists : Screen("playlists")
    data object PlaylistDetail : Screen("playlist_detail/{playlistId}/{playlistName}") {
        fun createRoute(playlistId: Long, playlistName: String) =
            "playlist_detail/$playlistId/${java.net.URLEncoder.encode(playlistName, "UTF-8")}"
    }
    data object VideoCutter : Screen("video_cutter/{mediaId}") {
        fun createRoute(mediaId: Long) = "video_cutter/$mediaId"
    }
    data object AudioExtract : Screen("audio_extract/{mediaId}") {
        fun createRoute(mediaId: Long) = "audio_extract/$mediaId"
    }
    data object Settings : Screen("settings")
    data object About : Screen("about")
    data object PrivacyPolicy : Screen("privacy_policy")
    data object AppLock : Screen("app_lock")
}
