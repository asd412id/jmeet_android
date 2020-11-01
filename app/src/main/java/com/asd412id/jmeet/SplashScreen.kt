package com.asd412id.jmeet

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.asd412id.jmeet.modules.Storages
import com.asd412id.jmeet.modules.UserApi

class SplashScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        Handler(Looper.getMainLooper()).postDelayed({
            val token = Storages(this).getData("user","credential")
            if(token != null){
                val queue = Volley.newRequestQueue(this)
                val request = object: JsonObjectRequest(Method.GET, UserApi(this).index(), null, { response ->
                    val code: Int = response.getInt("code")
                    if(code == 200){
                        startActivity(Intent(this,MainActivity::class.java))
                        finishAffinity()
                    }
                }, {
                    startActivity(Intent(this,MainActivity::class.java))
                    finishAffinity()
                }){
                    override fun getHeaders(): MutableMap<String, String> {
                        val headers = HashMap<String, String>()
                        headers["Authorization"] = "Bearer " + token.getString("api_token")
                        return headers
                    }
                }
                request.retryPolicy = DefaultRetryPolicy(
                    DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
                    3,
                    1f
                )
                queue.add(request)
            }else{
                startActivity(Intent(this,Login::class.java))
                finishAffinity()
            }
        },2500)
    }
}