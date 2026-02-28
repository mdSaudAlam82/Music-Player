package com.example.musicplayer.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.musicplayer.data.local.db.AppDatabase
import com.example.musicplayer.data.local.db.PlaylistDao
import com.example.musicplayer.data.local.db.RecentlyPlayedDao
import com.example.musicplayer.data.local.db.SongDao
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
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "music_player_db"
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3) // Dono migrations yahan add ho gayi hain
            .build()
    }

    // Migration 1 se 2: isTrending column add karne ke liye
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE songs ADD COLUMN isTrending INTEGER NOT NULL DEFAULT 0"
            )
        }
    }

    // Migration 2 se 3: streamUrl column hatane aur cachedAt add karne ke liye
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Naya table banao bina streamUrl ke
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS songs_new (
                    id TEXT NOT NULL PRIMARY KEY,
                    title TEXT NOT NULL,
                    artist TEXT NOT NULL,
                    album TEXT NOT NULL,
                    duration INTEGER NOT NULL,
                    artworkUrl TEXT NOT NULL,
                    localPath TEXT,
                    isLocal INTEGER NOT NULL DEFAULT 0,
                    isDownloaded INTEGER NOT NULL DEFAULT 0,
                    isTrending INTEGER NOT NULL DEFAULT 0,
                    hasLyrics INTEGER NOT NULL DEFAULT 0,
                    cachedAt INTEGER NOT NULL DEFAULT 0
                )
            """)

            // Purana data copy karo (streamUrl ko chhod kar)
            database.execSQL("""
                INSERT INTO songs_new 
                SELECT id, title, artist, album, duration, artworkUrl,
                       localPath, isLocal, isDownloaded, isTrending, hasLyrics,
                       ${System.currentTimeMillis()}
                FROM songs
            """)

            // Purana table hatao
            database.execSQL("DROP TABLE songs")

            // Naye table ka naam rename karo
            database.execSQL("ALTER TABLE songs_new RENAME TO songs")
        }
    }

    @Provides
    @Singleton
    fun provideSongDao(db: AppDatabase): SongDao = db.songDao()

    @Provides
    @Singleton
    fun provideRecentlyPlayedDao(db: AppDatabase): RecentlyPlayedDao = db.recentlyPlayedDao()

    @Provides
    @Singleton
    fun providePlaylistDao(db: AppDatabase): PlaylistDao = db.playlistDao()
}