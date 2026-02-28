package com.example.musicplayer.data.local

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.musicplayer.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStoreHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // Device ke saare audio files scan karo
    fun scanLocalSongs(): List<Song> {
        val songs = mutableListOf<Song>()

        // Kaunse columns chahiye
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,        // file path
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.MIME_TYPE
        )

        // Sirf music files chahiye, 30 sec se zyada
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 " +
                "AND ${MediaStore.Audio.Media.DURATION} >= 30000"

        // Latest pehle
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        // Android 10+ ke liye alag URI
        val collection: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val cursor: Cursor? = context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            sortOrder
        )

        cursor?.use {
            // Column indexes lao — baar baar dhundne se better
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val artistIdColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
            val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val yearColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val title = it.getString(titleColumn) ?: "Unknown"
                val artist = it.getString(artistColumn) ?: "Unknown Artist"
                val artistId = it.getLong(artistIdColumn).toString()
                val album = it.getString(albumColumn) ?: "Unknown Album"
                val albumId = it.getLong(albumIdColumn).toString()
                val duration = it.getLong(durationColumn)
                val path = it.getString(dataColumn) ?: continue
                val year = it.getInt(yearColumn).toString()

                // Album art URI banao
                val artworkUri = "content://media/external/audio/albumart/$albumId"

                // File content URI banao — ExoPlayer isse play karega
                val contentUri = Uri.withAppendedPath(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                ).toString()

                val song = Song(
                    id = "local_$id",
                    title = title,
                    artist = artist,
                    artistId = artistId,
                    album = album,
                    albumId = albumId,
                    duration = duration,
                    artworkUrl = artworkUri,
                    streamUrl = contentUri,   // ExoPlayer content URI use karega
                    localPath = path,
                    isLocal = true,
                    isDownloaded = false,
                )

                songs.add(song)
            }
        }

        return songs
    }

    // Specific song exist karti hai ya nahi check karo
    fun doesSongExist(path: String): Boolean {
        return try {
            val file = java.io.File(path)
            file.exists()
        } catch (e: Exception) {
            false
        }
    }
}