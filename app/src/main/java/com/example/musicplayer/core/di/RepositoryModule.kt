package com.example.musicplayer.core.di

import com.example.musicplayer.data.repository.DownloadRepositoryImpl
import com.example.musicplayer.data.repository.LocalMusicRepositoryImpl
import com.example.musicplayer.data.repository.MusicRepositoryImpl
import com.example.musicplayer.data.repository.PlaylistRepositoryImpl
import com.example.musicplayer.domain.repository.DownloadRepository
import com.example.musicplayer.domain.repository.LocalMusicRepository
import com.example.musicplayer.domain.repository.MusicRepository
import com.example.musicplayer.domain.repository.PlaylistRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMusicRepository(
        impl: MusicRepositoryImpl
    ): MusicRepository

    @Binds
    @Singleton
    abstract fun bindLocalMusicRepository(
        impl: LocalMusicRepositoryImpl
    ): LocalMusicRepository

    @Binds
    @Singleton
    abstract fun bindPlaylistRepository(
        impl: PlaylistRepositoryImpl
    ): PlaylistRepository

    @Binds
    @Singleton
    abstract fun bindDownloadRepository(
        impl: DownloadRepositoryImpl
    ): DownloadRepository
}