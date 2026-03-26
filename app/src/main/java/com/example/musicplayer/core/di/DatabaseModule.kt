package com.example.musicplayer.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.musicplayer.data.local.dao.LikedSongDao
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
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()
    }

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE songs ADD COLUMN isTrending INTEGER NOT NULL DEFAULT 0"
            )
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
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
            database.execSQL("""
                INSERT INTO songs_new 
                SELECT id, title, artist, album, duration, artworkUrl,
                       localPath, isLocal, isDownloaded, isTrending, hasLyrics,
                       ${System.currentTimeMillis()}
                FROM songs
            """)
            database.execSQL("DROP TABLE songs")
            database.execSQL("ALTER TABLE songs_new RENAME TO songs")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `liked_songs` (
                    `id` TEXT NOT NULL, 
                    `title` TEXT NOT NULL, 
                    `artist` TEXT, 
                    `artworkUrl` TEXT, 
                    `streamUrl` TEXT NOT NULL, 
                    `duration` INTEGER NOT NULL, 
                    `isLocal` INTEGER NOT NULL, 
                    `isDownloaded` INTEGER NOT NULL, 
                    `addedAt` INTEGER NOT NULL, 
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
        }
    }

    // Migration 4 → 5:
    // recently_played: extra columns hata diye (album, albumId, artistId, language,
    //                  year, playCount, isExplicit, lyricsId)
    // liked_songs: localPath, hasLyrics add kiye
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {

            // --- recently_played cleanup ---
            // Room SQLite mein columns drop nahi ho sakte directly,
            // isliye nayi table banao aur data copy karo
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS recently_played_new (
                    songId TEXT NOT NULL PRIMARY KEY,
                    title TEXT NOT NULL,
                    artist TEXT NOT NULL,
                    artworkUrl TEXT NOT NULL,
                    streamUrl TEXT NOT NULL,
                    duration INTEGER NOT NULL,
                    localPath TEXT,
                    isLocal INTEGER NOT NULL DEFAULT 0,
                    isDownloaded INTEGER NOT NULL DEFAULT 0,
                    hasLyrics INTEGER NOT NULL DEFAULT 0,
                    playedAt INTEGER NOT NULL DEFAULT 0
                )
            """)
            // Jo columns abhi bhi common hain unhe copy karo
            database.execSQL("""
                INSERT INTO recently_played_new 
                (songId, title, artist, artworkUrl, streamUrl, duration,
                 localPath, isLocal, isDownloaded, hasLyrics, playedAt)
                SELECT songId, title, artist, artworkUrl, streamUrl, duration,
                       localPath, isLocal, isDownloaded, hasLyrics, playedAt
                FROM recently_played
            """)
            database.execSQL("DROP TABLE recently_played")
            database.execSQL("ALTER TABLE recently_played_new RENAME TO recently_played")

            // --- liked_songs: localPath aur hasLyrics add karo ---
            database.execSQL(
                "ALTER TABLE liked_songs ADD COLUMN localPath TEXT"
            )
            database.execSQL(
                "ALTER TABLE liked_songs ADD COLUMN hasLyrics INTEGER NOT NULL DEFAULT 0"
            )
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

    @Provides
    @Singleton
    fun provideLikedSongDao(db: AppDatabase): LikedSongDao = db.likedSongDao()
}