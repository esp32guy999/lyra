package com.resonance.music.data.qbit

import com.resonance.music.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/** qBit hands out a SID cookie on login; keep it for the rest of the session. */
private class SessionCookieJar : CookieJar {
    private var cookies: List<Cookie> = emptyList()
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isNotEmpty()) this.cookies = cookies
    }
    override fun loadForRequest(url: HttpUrl): List<Cookie> = cookies
}

@Module
@InstallIn(SingletonComponent::class)
object QbitModule {

    @Provides
    @Singleton
    @Named("qbit")
    fun provideQbitOkHttp(): OkHttpClient =
        OkHttpClient.Builder()
            .cookieJar(SessionCookieJar())
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideQbitApi(@Named("qbit") client: OkHttpClient): QbitApi {
        val base = BuildConfig.QBIT_URL.ifBlank { "http://localhost:8090/" }
        return Retrofit.Builder()
            .baseUrl(base)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(QbitApi::class.java)
    }

    @Provides
    @Singleton
    fun provideQbitConfig(): QbitConfig =
        QbitConfig(BuildConfig.QBIT_URL, BuildConfig.QBIT_USER, BuildConfig.QBIT_PASS, BuildConfig.QBIT_SAVE_PATH)
}

data class QbitConfig(
    val baseUrl: String,
    val user: String,
    val pass: String,
    /** Optional save dir (must be a path visible inside qBit's container). Empty → qBit default. */
    val savePath: String = ""
) {
    val isConfigured: Boolean get() = baseUrl.isNotBlank() && user.isNotBlank()
}
