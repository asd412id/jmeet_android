package com.asd412id.jmeet.modules

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class UserApi(val context: Context) {
    private val baseURL = ApiConnection().baseURL()+"user"

    fun index(): String {
        return baseURL
    }
    fun register(): String {
        return "$baseURL/register"
    }
    fun login(): String {
        return "$baseURL/login"
    }
    fun logout(): String {
        return "$baseURL/logout"
    }
}