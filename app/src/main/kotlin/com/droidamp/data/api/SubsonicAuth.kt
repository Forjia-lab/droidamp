package com.droidamp.data.api

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerUrlProvider @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    companion object {
        private val KEY_URL      = stringPreferencesKey("server_url")
        private val KEY_USERNAME = stringPreferencesKey("username")
        private val KEY_PASSWORD = stringPreferencesKey("password")
        private const val DEFAULT_URL      = "http://100.122.7.119:4533"
        private const val DEFAULT_USERNAME = "techbrooo"
        private const val DEFAULT_PASSWORD = "w!iE80EgCv&3*KiBBcy4n3&J"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var _baseUrl: String
    private var _username: String
    private var _password: String

    init {
        val prefs = runBlocking { dataStore.data.first() }
        _baseUrl  = prefs[KEY_URL]      ?: DEFAULT_URL
        _username = prefs[KEY_USERNAME] ?: DEFAULT_USERNAME
        _password = prefs[KEY_PASSWORD] ?: DEFAULT_PASSWORD
    }

    fun baseUrl() = _baseUrl
    fun username() = _username
    fun password() = _password

    fun update(baseUrl: String, username: String, password: String) {
        _baseUrl  = baseUrl.trimEnd('/')
        _username = username
        _password = password
        scope.launch {
            dataStore.edit { prefs ->
                prefs[KEY_URL]      = _baseUrl
                prefs[KEY_USERNAME] = _username
                prefs[KEY_PASSWORD] = _password
            }
        }
    }

    fun authParams(): String {
        val salt  = UUID.randomUUID().toString().replace("-", "").take(10)
        val token = md5("$_password$salt")
        return "u=$_username&t=$token&s=$salt&v=1.16.1&c=droidamp&f=json"
    }

    fun isConfigured() = _baseUrl.isNotBlank() && _username.isNotBlank() && _password.isNotBlank()

    private fun md5(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

@Singleton
class SubsonicAuthInterceptor @Inject constructor(
    private val serverUrlProvider: ServerUrlProvider,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original   = chain.request()
        val urlBuilder = original.url.newBuilder()
        val salt  = UUID.randomUUID().toString().replace("-", "").take(10)
        val token = md5("${serverUrlProvider.password()}$salt")
        urlBuilder
            .addQueryParameter("u", serverUrlProvider.username())
            .addQueryParameter("t", token)
            .addQueryParameter("s", salt)
            .addQueryParameter("v", "1.16.1")
            .addQueryParameter("c", "droidamp")
            .addQueryParameter("f", "json")
        return chain.proceed(original.newBuilder().url(urlBuilder.build()).build())
    }

    private fun md5(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
