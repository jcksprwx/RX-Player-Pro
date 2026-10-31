package com.rxplayer.pro.core.di

import android.content.Context
import androidx.room.Room
import com.rxplayer.pro.core.database.NovaDatabase
import com.rxplayer.pro.core.database.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NovaDatabase {
        return Room.databaseBuilder(
            context,
            NovaDatabase::class.java,
            "nova_player_pro.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideMediaDao(db: NovaDatabase): MediaDao = db.mediaDao()

    @Provides
    fun provideHistoryDao(db: NovaDatabase): HistoryDao = db.historyDao()

    @Provides
    fun providePlaylistDao(db: NovaDatabase): PlaylistDao = db.playlistDao()

    @Provides
    fun provideServerDao(db: NovaDatabase): ServerDao = db.serverDao()

    @Provides
    fun provideSettingsDao(db: NovaDatabase): SettingsDao = db.settingsDao()

    @Provides
    fun provideHiddenFolderDao(db: NovaDatabase): HiddenFolderDao = db.hiddenFolderDao()
}
