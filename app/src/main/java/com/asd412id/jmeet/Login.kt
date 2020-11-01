package com.asd412id.jmeet

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.asd412id.jmeet.modules.Storages
import com.asd412id.jmeet.modules.UserApi
import org.json.JSONObject

class Login : AppCompatActivity() {
    private lateinit var username: EditText
    private lateinit var password: EditText
    private lateinit var toRegister: Button
    private lateinit var btnLogin: Button
    private lateinit var progressBackdrop: RelativeLayout
    private lateinit var progressWrap: RelativeLayout
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        username = findViewById(R.id.username)
        password = findViewById(R.id.password)
        toRegister = findViewById(R.id.to_register)
        btnLogin = findViewById(R.id.btn_Login)
        progressBackdrop = findViewById(R.id.progress_backdrop)
        progressWrap = findViewById(R.id.progress_wrap)
        progressBar = findViewById(R.id.progressBar)
        loading(false)

        toRegister.setOnClickListener {
            startActivity(Intent(this,Register::class.java))
        }
        btnLogin.setOnClickListener {
            activate(false)
            loading(true)
            val queue = Volley.newRequestQueue(this)
            val url = UserApi(this).login()

            val request = object: JsonObjectRequest(Method.POST, url, null, { response ->
                val data: JSONObject = response.getJSONObject("data")
                Storages(this@Login).setData("user","credential",data)
                startActivity(Intent(this,MainActivity::class.java))
                finishAffinity()
            }, {error ->
                if (error?.networkResponse != null){
                    val errorJSON = JSONObject(String(error.networkResponse.data))
                    if (errorJSON.getInt("code") == 406){
                        Toast.makeText(this,"Data login tidak benar!", Toast.LENGTH_LONG).show()
                    }
                }else{
                    Toast.makeText(this,"Tidak dapat terhubung ke server", Toast.LENGTH_LONG).show()
                }
                activate(true)
                loading(false)
            }){
                override fun getHeaders(): MutableMap<String, String> {
                    val headers = HashMap<String, String>()
                    headers["username"] = username.text.toString()
                    headers["password"] = password.text.toString()
                    return headers
                }
            }

            request.retryPolicy = DefaultRetryPolicy(
                    DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
                    0,
                    1f
            )

            queue.add(request)
        }

    }

    private fun activate(status: Boolean){
        username.isEnabled = status
        password.isEnabled = status
        toRegister.isEnabled = status
        btnLogin.isEnabled = status
    }

    private fun loading(status: Boolean){
        if (status){
            progressWrap.visibility = View.VISIBLE
            progressBackdrop.visibility = View.VISIBLE
        }else{
            progressWrap.visibility = View.GONE
            progressBackdrop.visibility = View.GONE
        }
    }
}