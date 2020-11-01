package com.asd412id.jmeet.modules

class MeetApi {
    private val baseURL = ApiConnection().baseURL()+"meet"

    fun list(): String {
        return "$baseURL/lists"
    }
    fun detail(): String {
        return "$baseURL/meet-detail"
    }
    fun create(): String {
        return "$baseURL/create"
    }
    fun update(): String {
        return "$baseURL/update"
    }
    fun destroy(): String {
        return "$baseURL/destroy"
    }
    fun activate(): String {
        return "$baseURL/active"
    }
    fun refreshToken(): String {
        return "$baseURL/refresh-token"
    }
    fun joinMeet(): String {
        return "$baseURL/join-meet"
    }
    fun signIn(): String {
        return "$baseURL/signin"
    }
    fun signOut(): String {
        return "$baseURL/signout"
    }
    fun print(): String {
        return "$baseURL/print"
    }
}