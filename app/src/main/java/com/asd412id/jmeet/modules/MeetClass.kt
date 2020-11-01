package com.asd412id.jmeet.modules

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject
import java.util.HashMap

class MeetClass(val context: Context) {
    val id: MutableList<String> = ArrayList()
    val name: MutableList<String> = ArrayList()
    val token: MutableList<String> = ArrayList()
    val desc: MutableList<String> = ArrayList()
    val start: MutableList<String> = ArrayList()
    val end: MutableList<String> = ArrayList()

    fun getLocalDataMeet() {
        val data = Storages(context).getAllData("meets")
        if (data != null) {
            for (key in data){
                val obj = JSONObject(key.toString().split("=")[1])
                name.add(obj.getString("name"))
                token.add(obj.getString("_token"))
                desc.add(obj.getString("desc"))
                start.add(obj.getString("start"))
                end.add(obj.getString("end"))
            }
        }
    }

    fun getServerDataMeet() {
        val token = Storages(context).getData("user","credential")!!
        val queue = Volley.newRequestQueue(context)
        val url = MeetApi().list()

        Storages(context).destroyData("configs","meetsloaded")

        val request = object: JsonObjectRequest(Method.GET, url, null, { response ->
            val data: JSONArray = response.getJSONArray("data")
            Storages(context).destroyAll("meets")
            for (i in 0 until data.length()){
                val obj = JSONObject(data[i].toString())
                Storages(context).setData("meets",i.toString(),obj)
            }
            Storages(context).setData("configs","meetsloaded",JSONObject().put("status",true))
        }, {error ->
            if (error?.networkResponse != null){
                val errorJSON = JSONObject(String(error.networkResponse.data))
                if (errorJSON.getInt("code") == 406){
                    Toast.makeText(context,errorJSON.getJSONArray("message")[0].toString(), Toast.LENGTH_LONG).show()
                }
            }else{
                Toast.makeText(context,"Tidak dapat terhubung ke server", Toast.LENGTH_LONG).show()
            }
            Storages(context).setData("configs","meetsloaded",JSONObject().put("status",false))
        }){
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer " + token.getString("api_token")
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