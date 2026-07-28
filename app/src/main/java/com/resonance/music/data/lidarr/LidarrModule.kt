package com.resonance.music.data.lidarr

import com.resonance.music.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

data class LidarrConfig(
    val baseUrl: String,
    val apiKey: String
) {
    val isConfigured: Boolean get() = baseUrl.isNotBlank() && apiKey.isNotBlank()
}

@Module
@InstallIn(SingletonComponent::class)
object LidarrModule {

    @Provides
    @Singleton
    fun provideLidarrConfig(): LidarrConfig =
        LidarrConfig(baseUrl = BuildConfig.LIDARR_URL, apiKey = BuildConfig.LIDARR_KEY)

    // A DEDICATED client — not the Subsonic one — so its base-URL/auth interceptors
    // don't rewrite Lidarr requests. Adds the X-Api-Key header Lidarr requires.
    @Provides
    @Singleton
    @Named("lidarr")
    fun provideLidarrOkHttp(config: LidarrConfig): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .addHeader("X-Api-Key", config.apiKey)
                    .build()
                chain.proceed(req)
            }
            .build()

    @Provides
    @Singleton
    fun provideLidarrApi(
        @Named("lidarr") okHttpClient: OkHttpClient,
        config: LidarrConfig
    ): LidarrApi {
        val base = config.baseUrl.ifBlank { "http://localhost:8686/" }
        return Retrofit.Builder()
            .baseUrl(base)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LidarrApi::class.java)
    }
}
