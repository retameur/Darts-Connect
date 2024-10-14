package com.retameur.dartsconnect

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform