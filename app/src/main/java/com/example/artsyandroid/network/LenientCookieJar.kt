package com.example.artsyandroid.network

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class LenientCookieJar(
    private val delegate: CookieJar
) : CookieJar {
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val stripped = cookies.map { c ->
            Cookie.Builder()
                .name(c.name)
                .value(c.value)
                .expiresAt(c.expiresAt)
                .path(c.path)
                .apply {
                    if (c.hostOnly) hostOnlyDomain(c.domain)
                    else domain(c.domain)
                }
                .also { if (c.httpOnly) it.httpOnly() }
                .build()
        }
        delegate.saveFromResponse(url, stripped)
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return delegate.loadForRequest(url)
    }
}
