package com.example.musicplayer.data.repository

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import com.example.musicplayer.data.local.db.SongDao
import com.example.musicplayer.data.local.toDomain
import com.example.musicplayer.data.local.toEntity
import com.example.musicplayer.domain.model.Resource
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.repository.DownloadRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songDao: SongDao
) : DownloadRepository {

    override fun downloadSong(song: Song): Flow<Resource<Int>> = callbackFlow {
        trySend(Resource.Loading(0))

        if (song.streamUrl.isBlank()) {
            trySend(Resource.Error("Stream URL nahi mila"))
            close()
            return@callbackFlow
        }

        val safeName = song.title.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(50)
        val fileName = "${song.id}_$safeName.mp3"
        val downloadDir = File(context.getExternalFilesDir(null), "MusicPlayer/downloads")
        val outputFile = File(downloadDir, fileName)

        // Pehle se downloaded hai to seedha success
        if (outputFile.exists() && outputFile.length() > 0) {
            songDao.markAsDownloaded(song.id, outputFile.absolutePath)
            trySend(Resource.Success(100))
            close()
            return@callbackFlow
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(song.streamUrl)).apply {
            setTitle(song.title)
            setDescription("Downloading ${song.artist}")
            // 👇 VISIBILITY FIX: Download poora hone par notification hata dega automatically (optional, par clean rakhta hai)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            setDestinationInExternalFilesDir(context, "MusicPlayer/downloads", fileName)
            setAllowedOverMetered(true)
        }

        val downloadId = downloadManager.enqueue(request)
        var isCompleted = false // Ek flag taaki loop band ho jaye

        // BroadcastReceiver — jab download complete ho tab fire hoga
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id != downloadId) return

                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)

                if (cursor != null && cursor.moveToFirst()) {
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        launch(Dispatchers.IO) {
                            songDao.insertSong(song.copy(isDownloaded = true, localPath = outputFile.absolutePath).toEntity())
                            songDao.markAsDownloaded(song.id, outputFile.absolutePath)
                            isCompleted = true
                            trySend(Resource.Success(100))
                            close() // Flow band karo
                        }
                    } else if (status == DownloadManager.STATUS_FAILED) {
                        isCompleted = true
                        trySend(Resource.Error("Download fail ho gaya"))
                        close()
                    }
                    cursor.close()
                } else {
                    // Agar cursor null hai matlab gaana process hi nahi hua
                    isCompleted = true
                    trySend(Resource.Error("Download error"))
                    close()
                }
            }
        }

        // Receiver register karo
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }

        // Progress polling loop
        launch(Dispatchers.IO) {
            var previousProgress = 0
            while (!isCompleted) {
                delay(500L) // Thoda fast polling
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)
                if (cursor != null && cursor.moveToFirst()) {
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    if (status == DownloadManager.STATUS_RUNNING) {
                        val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                        if (total > 0) {
                            val progress = ((downloaded * 100) / total).toInt()
                            if (progress != previousProgress) {
                                previousProgress = progress
                                trySend(Resource.Loading(progress))
                            }
                        }
                    } else if (status == DownloadManager.STATUS_FAILED || status == DownloadManager.STATUS_SUCCESSFUL) {
                        cursor.close()
                        break // Receiver sambhal lega
                    }
                    cursor.close()
                }
            }
        }

        awaitClose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }.flowOn(Dispatchers.IO)

    // ... (baaki functions same rahenge)
    override fun getDownloadedSongs(): Flow<List<Song>> {
        return songDao.getDownloadedSongs().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun isSongDownloaded(songId: String): Boolean {
        return songDao.isSongDownloaded(songId)
    }

    override suspend fun deleteDownloadedSong(songId: String) {
        val song = songDao.getSongById(songId)
        song?.localPath?.let { path ->
            val file = File(path)
            if (file.exists()) file.delete()
        }
        songDao.deleteSong(songId)
    }
}