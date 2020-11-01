package com.asd412id.jmeet

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.asd412id.jmeet.fragments.Meets
import com.asd412id.jmeet.modules.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

@Suppress("NAME_SHADOWING")
class MainActivity : AppCompatActivity(), DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {
    private lateinit var appBarConfiguration: AppBarConfiguration
    var day = 0
    var month: Int = 0
    var year: Int = 0
    var hour: Int = 0
    var minute: Int = 0
    var myDay = 0
    var myMonth: Int = 0
    var myYear: Int = 0
    var setTime: String = ""
    @SuppressLint("SimpleDateFormat")
    val dateFormat =  SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    private lateinit var view: View
    private lateinit var loading: View
    private lateinit var builder: AlertDialog.Builder
    private lateinit var loadingBuilder: AlertDialog.Builder
    private lateinit var dialog: AlertDialog
    private lateinit var dialogLoading: AlertDialog
    private lateinit var token: JSONObject

    @SuppressLint("ResourceType", "InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        token = Storages(this).getData("user","credential")!!
        MeetClass(this).getServerDataMeet()

        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener { view ->
            val builder = AlertDialog.Builder(this)
            val view = layoutInflater.inflate(R.layout.join_dialog,null)

            builder.apply {
                setCancelable(false)
                setTitle("IKUTI PERTEMUAN")
                setView(view)
            }
            val dialog = builder.create()
            dialog.show()

            view.findViewById<Button>(R.id.join_meet).setOnClickListener {
                joinMeeting(view)
            }
            view.findViewById<Button>(R.id.d_cancel).setOnClickListener {
                dialog.dismiss()
            }
        }
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(setOf(
                R.id.nav_meets), drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        navView.setNavigationItemSelectedListener {
            when(it.itemId){
                R.id.nav_logout -> {
                    val b = AlertDialog.Builder(this)
                    b.apply {
                        setTitle("Konfirmasi")
                        setMessage("Yakin akan logout?")
                        setNegativeButton("Tidak"){dialog,i ->
                            dialog.dismiss()
                        }
                        setPositiveButton("Ya"){dialog,i ->
                            dialogLoading.show()
                            dialog.dismiss()
                            logOut()
                        }
                    }
                    val d = b.create()
                    d.show()
                    return@setNavigationItemSelectedListener false
                }
                R.id.nav_meets -> {
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                    startActivity(intent)
                    finishAffinity()
                    return@setNavigationItemSelectedListener true
                }
                else -> return@setNavigationItemSelectedListener false
            }
        }

        val headerView = navView.getHeaderView(0)
        val user = Storages(this).getData("user","credential")
        val txtName: TextView = headerView.findViewById(R.id.nav_name)
        val txtEmail: TextView = headerView.findViewById(R.id.nav_email)
        val txtPhone: TextView = headerView.findViewById(R.id.nav_phone)

        loading = layoutInflater.inflate(R.layout.loading,null)

        txtName.text = user?.getString("name")
        txtEmail.text = user?.getString("email")
        txtPhone.text = user?.getString("telp")

        loadingBuilder = AlertDialog.Builder(this)
        loadingBuilder.setView(loading)
        loadingBuilder.setCancelable(false)
        dialogLoading = loadingBuilder.create()

        builder = AlertDialog.Builder(this)
        view = layoutInflater.inflate(R.layout.meet_form,null)
        builder.apply {
            setTitle("BUAT PERTEMUAN")
            setCancelable(false)
            setView(view)
        }
        dialog = builder.create()
    }

    private fun logOut() {
        val queue = Volley.newRequestQueue(this)
        val url = UserApi(this).logout()

        Storages(this).destroyData("user","credential")
        val request = object: JsonObjectRequest(Method.POST, url, null, {
            dialogLoading.dismiss()
            startActivity(Intent(this,Login::class.java))
            finishAffinity()
        }, {error ->
            if (error?.networkResponse != null){
                Toast.makeText(this,"Tidak dapat memproses data", Toast.LENGTH_LONG).show()
            }else{
                Toast.makeText(this,"Tidak dapat terhubung ke server", Toast.LENGTH_LONG).show()
            }
            dialogLoading.dismiss()
            startActivity(Intent(this,Login::class.java))
            finishAffinity()
        }){
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer "+token.getString("api_token")
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when(item.itemId){
            R.id.new_meet -> createMeeting()
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("InflateParams")
    private fun createMeeting() {
        dialog.show()
        view.findViewById<TextView>(R.id.start).setOnClickListener {
            setTime = "start"
            val calendar: Calendar = Calendar.getInstance()
            if (view.findViewById<TextView>(R.id.start).text.toString() != ""){
                val date = dateFormat.parse(view.findViewById<TextView>(R.id.start).text.toString())
                calendar.time = date
            }
            day = calendar.get(Calendar.DAY_OF_MONTH)
            month = calendar.get(Calendar.MONTH)
            year = calendar.get(Calendar.YEAR)
            val datePickerDialog = DatePickerDialog(this@MainActivity, this@MainActivity, year, month,day)
            datePickerDialog.show()
        }
        view.findViewById<TextView>(R.id.end).setOnClickListener {
            setTime = "end"
            val calendar: Calendar = Calendar.getInstance()
            if (view.findViewById<TextView>(R.id.end).text.toString() != ""){
                val date = dateFormat.parse(view.findViewById<TextView>(R.id.end).text.toString())
                calendar.time = date
            }
            day = calendar.get(Calendar.DAY_OF_MONTH)
            month = calendar.get(Calendar.MONTH)
            year = calendar.get(Calendar.YEAR)
            val datePickerDialog = DatePickerDialog(this@MainActivity, this@MainActivity, year, month,day)
            datePickerDialog.show()
        }
        view.findViewById<Button>(R.id.d_cancel).setOnClickListener {
            dialog.dismiss()
        }
        view.findViewById<Button>(R.id.btn_save).setOnClickListener {
            dialogLoading.show()
            val queue = Volley.newRequestQueue(this)
            val url = MeetApi().create()

            val request = object: JsonObjectRequest(Method.POST, url, null, {
                view.findViewById<EditText>(R.id.name).text.clear()
                view.findViewById<EditText>(R.id.desc).text.clear()
                view.findViewById<TextView>(R.id.start).text = null
                view.findViewById<TextView>(R.id.end).text = null
                dialogLoading.dismiss()
                dialog.dismiss()
                finish()
                startActivity(intent)
                finishAffinity()
            }, {error ->
                if (error?.networkResponse != null){
                    val errorJSON = JSONObject(String(error.networkResponse.data))
                    if (errorJSON.getInt("code") == 406){
                        Toast.makeText(this,errorJSON.getJSONArray("message")[0].toString(), Toast.LENGTH_LONG).show()
                    }
                }else{
                    Toast.makeText(this,"Tidak dapat terhubung ke server", Toast.LENGTH_LONG).show()
                }
                dialogLoading.dismiss()
            }){
                override fun getHeaders(): MutableMap<String, String> {
                    val headers = HashMap<String, String>()
                    headers["Authorization"] = "Bearer " + token.getString("api_token")
                    headers["name"] = view.findViewById<EditText>(R.id.name).text.toString()
                    headers["desc"] = view.findViewById<EditText>(R.id.desc).text.toString()
                    headers["start"] = view.findViewById<TextView>(R.id.start).text.toString()
                    headers["end"] = view.findViewById<TextView>(R.id.end).text.toString()
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

    private fun joinMeeting(view: View) {
        dialogLoading.show()
        val queue = Volley.newRequestQueue(this)
        val url = MeetApi().joinMeet()

        val request = object: JsonObjectRequest(Method.POST, url, null, {
            view.findViewById<EditText>(R.id.kode_meet).text.clear()
            dialogLoading.dismiss()
            dialog.dismiss()
            finish()
            startActivity(intent)
            finishAffinity()
        }, {error ->
            if (error?.networkResponse != null){
                Toast.makeText(this,"Tidak dapat memproses data", Toast.LENGTH_LONG).show()
            }else{
                Toast.makeText(this,"Tidak dapat terhubung ke server", Toast.LENGTH_LONG).show()
            }
            dialogLoading.dismiss()
        }){
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer " + token.getString("api_token")
                headers["Meet-Token"] = view.findViewById<EditText>(R.id.kode_meet).text.toString()
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

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onDateSet(p0: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        myDay = dayOfMonth
        myYear = year
        myMonth = month+1
        val gd = if (setTime == "start"){
            view.findViewById<TextView>(R.id.start).text.toString()
        }else{
            view.findViewById<TextView>(R.id.end).text.toString()
        }
        val calendar: Calendar = Calendar.getInstance()
        if (gd != ""){
            val date = dateFormat.parse(gd)
            calendar.time = date
        }
        hour = calendar.get(Calendar.HOUR)
        minute = calendar.get(Calendar.MINUTE)
        val timePickerDialog = TimePickerDialog(this@MainActivity, this@MainActivity, hour, minute,
                DateFormat.is24HourFormat(this))
        timePickerDialog.show()
    }

    @SuppressLint("InflateParams", "SetTextI18n")
    override fun onTimeSet(p0: TimePicker?, hourOfDay: Int, minuteOfHour: Int) {
        var mm = myMonth.toString()
        var dd = myDay.toString()
        var hh = hourOfDay.toString()
        var ii = minuteOfHour.toString()
        if (myMonth<10){
            mm = "0$myMonth"
        }
        if (myDay<10){
            dd = "0$myDay"
        }
        if (hourOfDay<10){
            hh = "0$hourOfDay"
        }
        if (minuteOfHour<10){
            ii = "0$minuteOfHour"
        }
        val et: TextView = if (setTime == "start"){
            view.findViewById(R.id.start)
        }else{
            view.findViewById(R.id.end)
        }
        et.text = "${myYear}-${mm}-${dd} ${hh}:${ii}:00"
    }
}