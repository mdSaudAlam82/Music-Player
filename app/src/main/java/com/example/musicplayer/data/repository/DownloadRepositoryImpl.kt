package com.example.musicplayer.data.repository

import android.content.Context
import com.example.musicplayer.data.local.db.SongDao
import com.example.musicplayer.data.local.toDomain
import com.example.musicplayer.data.local.toEntity
import com.example.musicplayer.domain.model.Resource
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.repository.DownloadRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songDao: SongDao,
    private val okHttpClient: OkHttpClient
) : DownloadRepository {

    override fun downloadSong(song: Song): Flow<Resource<Int>> = flow {
        emit(Resource.Loading(0))
        try {
            // Stream URL check karo
            if (song.streamUrl.isBlank()) {
                emit(Resource.Error("Stream URL nahi mila"))
                return@flow
            }

            // Downloads folder
            val downloadDir = File(context.getExternalFilesDir(null), "MusicPlayer/downloads")
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }

            // File name safe banao
            val safeName = song.title
                .replace(Regex("[^a-zA-Z0-9._-]"), "_")
                .take(50)
            val fileName = "${song.id}_$safeName.mp3"
            val outputFile = File(downloadDir, fileName)

            // Pehle se exist karta hai to skip
            if (outputFile.exists() && outputFile.length() > 0) {
                songDao.markAsDownloaded(song.id, outputFile.absolutePath)
                emit(Resource.Success(100))
                return@flow
            }

            // OkHttp request
            val request = Request.Builder()
                .url(song.streamUrl)
                .addHeader("User-Agent", "Mozilla/5.0")
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                emit(Resource.Error("Server error: ${response.code}"))
                return@flow
            }

            val body = response.body
            if (body == null) {
                emit(Resource.Error("Empty response aaya"))
                return@flow
            }

            val totalBytes = body.contentLength()
            var downloadedBytes = 0L

            // Temp file me pehle likhenge
            val tempFile = File(downloadDir, "$fileName.tmp")

            try {
                FileOutputStream(tempFile).use { outputStream ->
                    body.byteStream().use { inputStream ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            if (totalBytes > 0) {
                                val progress = ((downloadedBytes * 100) / totalBytes).toInt()
                                emit(Resource.Loading(progress))
                            }
                        }
                    }
                }

                // Temp se final file pe move karo
                tempFile.renameTo(outputFile)

            } catch (e: Exception) {
                // Error pe temp file delete karo
                tempFile.delete()
                throw e
            }

            // File successfully download hui
            val downloadedSong = song.copy(
                isDownloaded = true,
                localPath = outputFile.absolutePath
            )

            // Pehle insert karo
            songDao.insertSong(downloadedSong.toEntity())

            // Phir explicitly mark karo — double confirm
            songDao.markAsDownloaded(song.id, outputFile.absolutePath)

            // Verify karo — file exist karti hai?
            if (!outputFile.exists() || outputFile.length() == 0L) {
                emit(Resource.Error("File save nahi hui"))
                return@flow
            }

            emit(Resource.Success(100))

        } catch (e: Exception) {
            emit(Resource.Error("Download fail: ${e.localizedMessage ?: "Unknown error"}"))
        }
    }.flowOn(Dispatchers.IO) // IO thread pe chalao

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