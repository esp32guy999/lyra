package com.resonance.music.data.prowlarr

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

@Module
@InstallIn(SingletonComponent::class)
object ProwlarrModule {

    @Provides
    @Singleton
    @Named("prowlarr")
    fun provideProwlarrOkHttp(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder()
                    .addHeader("X-Api-Key", BuildConfig.PROWLARR_KEY).build())
            }
            .build()

    @Provides
    @Singleton
    fun provideProwlarrApi(@Named("prowlarr") client: OkHttpClient): ProwlarrApi {
        val base = BuildConfig.PROWLARR_URL.ifBlank { "http://localhost:9696/" }
        return Retrofit.Builder()
            .baseUrl(base)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ProwlarrApi::class.java)
    }
}
