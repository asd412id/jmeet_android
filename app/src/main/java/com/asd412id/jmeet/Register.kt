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

class Register : AppCompatActivity() {
    private lateinit var name: EditText
    private lateinit var email: EditText
    private lateinit var phone: EditText
    private lateinit var password: EditText
    private lateinit var confirmPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var toLogin: Button
    private lateinit var progressBackdrop: RelativeLayout
    private lateinit var progressWrap: RelativeLayout
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        name = findViewById(R.id.name)
        email = findViewById(R.id.email)
        phone = findViewById(R.id.phone)
        password = findViewById(R.id.password)
        confirmPassword = findViewById(R.id.confirm_password)
        btnRegister = findViewById(R.id.btn_register)
        toLogin = findViewById(R.id.to_Login)
        progressBackdrop = findViewById(R.id.progress_backdrop)
        progressWrap = findViewById(R.id.progress_wrap)
        progressBar = findViewById(R.id.progressBar)
        loading(false)

        toLogin.setOnClickListener {
            startActivity(Intent(this,Login::class.java))
        }

        btnRegister.setOnClickListener {
            activate(false)
            loading(true)
            val queue = Volley.newRequestQueue(this)
            val url = UserApi(this).register()

            val request = object: JsonObjectRequest(Method.POST, url, null, { response ->
                val data: JSONObject = response.getJSONObject("data")
                Storages(this).setData("user","credential",data)
                startActivity(Intent(this,MainActivity::class.java))
                finishAffinity()
            }, {error ->
                if (error?.networkResponse != null){
                    val errorJSON = JSONObject(String(error.networkResponse.data))
                    if (errorJSON.getInt("code") == 406){
                        val errMsg = errorJSON.getJSONArray("message")
                        val err: String? = errMsg[0] as String?
                        Toast.makeText(this,err,Toast.LENGTH_LONG).show()
                    }
                }else{
                    Toast.makeText(this,"Tidak dapat terhubung ke server",Toast.LENGTH_LONG).show()
                }
                activate(true)
                loading(false)
            }){
                override fun getHeaders(): MutableMap<String, String> {
                    val headers = HashMap<String, String>()
                    headers["name"] = name.text.toString()
                    headers["email"] = email.text.toString()
                    headers["telp"] = phone.text.toString()
                    headers["password"] = password.text.toString()
                    headers["repassword"] = confirmPassword.text.toString()
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
        name.isEnabled = status
        email.isEnabled = status
        phone.isEnabled = status
        password.isEnabled = status
        confirmPassword.isEnabled = status
        btnRegister.isEnabled = status
        toLogin.isEnabled = status
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