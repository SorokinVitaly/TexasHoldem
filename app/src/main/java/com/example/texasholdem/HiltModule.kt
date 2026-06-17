package com.example.texasholdem

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
abstract class HiltModule {
    @Suppress("unused")
    @Binds
    abstract fun bindLocalDataRepository(impl: LocalDataRepositoryImpl): LocalDataRepository

    @Suppress("unused")
    @Binds
    abstract fun bindHistory(impl: HistoryImpl): History

    companion object {
        const val SHARED_PREFERENCES_NAME = "TexasHoldem"

        @Provides
        @Singleton
        fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences =
            context.getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE)
    }
}