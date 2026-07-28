package com.resonance.music.data.lidarr

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

data class LidarrConfig(
    val baseUrl: String = "http://localhost:8989/",
    val apiKey: String = "your-api-key-here"
)

@Module
@InstallIn(SingletonComponent::class)
object LidarrModule {

    @Provides
    @Singleton
    fun provideLidarrConfig(): LidarrConfig {
        return LidarrConfig()
    }

    @Provides
    @Singleton
    fun provideLidarrApi(
        okHttpClient: OkHttpClient,
        config: LidarrConfig
    ): LidarrApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(config.baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(LidarrApi::class.java)
    }
}
