package com.droidamp.data.api

import okhttp3.Interceptor
import okhttp3.Response
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerUrlProvider @Inject constructor() {
    private var _baseUrl: String = "http://100.122.7.119:4533"
    private var _username: String = "admin"
    private var _password: String = "admin"

    fun baseUrl() = _baseUrl
    fun username() = _username
    fun password() = _password

    fun update(baseUrl: String, username: String, password: String) {
        _baseUrl  = baseUrl.trimEnd('/')
        _username = username
        _password = password
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
