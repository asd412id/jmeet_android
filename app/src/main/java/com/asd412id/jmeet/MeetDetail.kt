package com.asd412id.jmeet

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.DownloadManager
import android.app.TimePickerDialog
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.format.DateFormat
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley.newRequestQueue
import com.asd412id.jmeet.modules.MeetApi
import com.asd412id.jmeet.modules.PaintView
import com.asd412id.jmeet.modules.Storages
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class MeetDetail : AppCompatActivity(), DatePickerDialog.OnDateSetListener,
    TimePickerDialog.OnTimeSetListener {
    lateinit var bundle: Bundle
    lateinit var credential: JSONObject
    lateinit var token: String
    lateinit var builder: AlertDialog.Builder
    lateinit var dialog: AlertDialog
    lateinit var data: JSONObject
    lateinit var status: TextView
    lateinit var refresh: SwipeRefreshLayout
    @SuppressLint("SimpleDateFormat")
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    lateinit var mfv: View

    var day = 0
    var month: Int = 0
    var year: Int = 0
    var hour: Int = 0
    var minute: Int = 0
    var myDay = 0
    var myMonth: Int = 0
    var myYear: Int = 0
    var setTime: String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meet_detail)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        bundle = intent.extras!!
        data = Storages(this).getData("meets", bundle.getString("key").toString())!!
        supportActionBar?.title = data.getString("name")

        builder = AlertDialog.Builder(this)
        builder.apply {
            setCancelable(false)
            setMessage("Silahkan Tunggu ...")
        }
        dialog = builder.create()
        initMeetDetail()

        refresh = findViewById(R.id.refresh)
        refresh.isRefreshing = true
        refresh.setOnRefreshListener {
            initMeetDetail()
        }
    }

    private fun initMeetDetail() {
        val ttd = this.findViewById<Button>(R.id.ttd)
        ttd.isVisible = false
        var join = false
        var done = false

        status = findViewById(R.id.status)
        findViewById<TextView>(R.id.name).text = data.getString("name")
        findViewById<TextView>(R.id.desc).text = data.getString("desc")
        findViewById<TextView>(R.id.token).text = data.getString("_token")
        findViewById<TextView>(R.id.start).text = data.getString("start")
        findViewById<TextView>(R.id.end).text = data.getString("end")

        findViewById<TextView>(R.id.token).setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(UUID.randomUUID().toString(),findViewById<TextView>(R.id.token).text.toString())
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this,"Token pertemuan berhasil disalin!",Toast.LENGTH_LONG).show()
        }

        credential = Storages(this).getData("user", "credential")!!
        token = credential.getString("api_token")

        status.setTextColor(Color.WHITE)
        if (data.getString("active") == "1"){
            status.text = "AKTIF"
            status.setBackgroundColor(Color.rgb(0, 128, 0))
        }else{
            status.text = "TIDAK AKTIF"
            status.setBackgroundColor(Color.rgb(223, 0, 0))
        }

        ttd.setOnClickListener {
            val view = layoutInflater.inflate(R.layout.ttd_meet_dialog, null)
            val bttd = AlertDialog.Builder(this)
            val paintView = view.findViewById<PaintView>(R.id.ttd)
            bttd.apply {
                setCancelable(false)
                setTitle("Tanda Tangan Pertemuan")
                setView(view)
            }
            val dttd = bttd.create()
            dttd.show()

            view.findViewById<Button>(R.id.cancel).setOnClickListener {
                dttd.dismiss()
            }
            view.findViewById<Button>(R.id.clear).setOnClickListener {
                paintView.clear()
            }
            view.findViewById<Button>(R.id.ok).setOnClickListener {
                sendSign(join, paintView.getBase64())
            }
        }
        findViewById<Button>(R.id.download_pdf).setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==  PackageManager.PERMISSION_DENIED){
                    requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1000)
                }else{
                    startDownloading()
                }
            }else{
                startDownloading()
            }
        }
        val queue = newRequestQueue(this)
        val url = MeetApi().detail()
        val request = @SuppressLint("InflateParams")
        object: JsonObjectRequest(Method.GET, url, null, { response ->
            val detail = response.getJSONObject("data")
            if (detail.getString("start") == "" || detail.getString("start") == "null") {
                ttd.text = "MULAI PERTEMUAN"
                ttd.setBackgroundColor(Color.rgb(0, 128, 0))
            } else if (detail.getString("end") == "" || detail.getString("end") == "null") {
                join = true
                ttd.text = "SELESAI PERTEMUAN"
                ttd.setBackgroundColor(Color.rgb(223, 0, 0))
            } else {
                done = true
                findViewById<LinearLayout>(R.id.done).visibility = View.VISIBLE
                findViewById<TextView>(R.id.signin).text = detail.getString("start")
                findViewById<TextView>(R.id.signout).text = detail.getString("end")
            }

            if (data.getString("active") == "1" && !done) {
                ttd.isVisible = true
            }
            refresh.isRefreshing = false
        }, { error ->
            if (error?.networkResponse != null) {
                ttd.text = "MULAI PERTEMUAN"
                ttd.setBackgroundColor(Color.rgb(0, 128, 0))
                if (data.getString("active") == "1") {
                    ttd.isVisible = true
                }
            } else {
                Toast.makeText(this, "Tidak dapat terhubung ke server", Toast.LENGTH_LONG).show()
            }
            refresh.isRefreshing = false
        }){
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer " + token
                headers["uuid"] = data.getString("uuid")
                return headers
            }
        }
        request.retryPolicy = DefaultRetryPolicy(
            DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
            3,
            1f
        )
        queue.add(request)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        when(requestCode){
            1000 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startDownloading()
                } else {
                    Toast.makeText(this, "Permission denied!", Toast.LENGTH_LONG)
                }
            }
        }
    }

    private fun startDownloading() {
        findViewById<Button>(R.id.download_pdf).isEnabled = false
        val request = DownloadManager.Request(Uri.parse(MeetApi().print()))
            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE or DownloadManager.Request.NETWORK_WIFI)
            .setTitle("Daftar Hadir ${data.getString("name")}.pdf")
            .setDescription("Mendownload daftar hadir ...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Daftar Hadir ${data.getString("name")}.pdf")
            .addRequestHeader("Authorization", "Bearer $token")
            .addRequestHeader("uuid", data.getString("uuid"))

        val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadit = manager.enqueue(request)

        val br = object:BroadcastReceiver(){
            override fun onReceive(p0: Context?, p1: Intent?) {
                val id = p1?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadit){
                    Toast.makeText(this@MeetDetail,"Daftar hadir berhasil di download!",Toast.LENGTH_LONG).show()
                    findViewById<Button>(R.id.download_pdf).isEnabled = true
                }
            }
        }
        registerReceiver(br, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    private fun sendSign(join: Boolean, base64: String) {
        dialog.show()
        val queue = newRequestQueue(this)
        val url = if(!join){
            MeetApi().signIn()
        }else{
            MeetApi().signOut()
        }
        val params = JSONObject()
        params.put("ttd", base64)

        val request = object: JsonObjectRequest(Method.PATCH, url, params, { response ->
            finish()
            startActivity(intent)
            dialog.dismiss()
        }, { error ->
            if (error?.networkResponse != null) {
                Toast.makeText(this, "Tidak dapat memproses data", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Tidak dapat terhubung ke server", Toast.LENGTH_LONG).show()
            }
            dialog.dismiss()
        }){
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $token"
                headers["uuid"] = data.getString("uuid")
                return headers
            }
        }
        queue.add(request)
    }

    override fun onSupportNavigateUp(): Boolean {
        startActivity(Intent(this, MainActivity::class.java))
        finishAffinity()
        return super.onSupportNavigateUp()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return if (data.getString("user_id") == credential.getString("id")){
            menuInflater.inflate(R.menu.meet_detail_menu, menu)
            if (data.getString("active") == "1"){
                menu?.findItem(R.id.activate)?.setTitle("Non-Aktifkan")
            }
            true
        }else{
            false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.activate -> activateMeeting()
            R.id.edit -> updateMeeting()
            R.id.delete -> {
                val alertBuilder = AlertDialog.Builder(this@MeetDetail)
                alertBuilder.apply {
                    setTitle("Konfirmasi")
                    setMessage("Yakin akan menghapus?")
                    setNegativeButton("Tidak") { dialogInterface, i ->
                        dialogInterface.dismiss()
                    }
                    setPositiveButton("Ya") { dialogInterface, i ->
                        deleteMeeting()
                    }
                }
                val alert = alertBuilder.create()
                alert.show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun activateMeeting() {
        dialog.show()
        var act = 1
        if (data.getString("active") == "1"){
            act = 0
        }
        val queue = newRequestQueue(this)
        val url = MeetApi().activate()
        val request = object: JsonObjectRequest(Method.PATCH, url, null, { response ->
            val data = response.getJSONObject("data")
            Storages(this).setData("meets", bundle.getString("key").toString(), data)
            finish()
            val intent = Intent(this@MeetDetail, MeetDetail::class.java)
            intent.putExtra("key", bundle.getString("key"))
            startActivity(intent)
        }, { error ->
            if (error?.networkResponse != null) {
                Toast.makeText(this, "Tidak dapat memproses permintaan", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Tidak dapat terhubung ke server", Toast.LENGTH_LONG).show()
            }
            dialog.dismiss()
        }){
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer " + token
                headers["active"] = act.toString()
                headers["uuid"] = data.getString("uuid")
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

    private fun deleteMeeting() {
        dialog.show()
        val queue = newRequestQueue(this)
        val url = MeetApi().destroy()
        val request = object: JsonObjectRequest(Method.DELETE, url, null, {
            Storages(this).destroyData("meets", bundle.getString("key").toString())
            val intent = Intent(this@MeetDetail, MainActivity::class.java)
            startActivity(intent)
            finishAffinity()
        }, { error ->
            if (error?.networkResponse != null) {
                Toast.makeText(this, "Tidak dapat memproses permintaan", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Tidak dapat terhubung ke server", Toast.LENGTH_LONG).show()
            }
            dialog.dismiss()
        }){
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $token"
                headers["uuid"] = data.getString("uuid")
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

    @SuppressLint("InflateParams", "CutPasteId")
    private fun updateMeeting() {
        val b = AlertDialog.Builder(this)
        mfv = layoutInflater.inflate(R.layout.meet_form, null)
        mfv.findViewById<PaintView>(R.id.ttd)
        b.apply {
            setCancelable(false)
            setTitle("UBAH PERTEMUAN")
            setView(mfv)
        }
        val d = b.create()
        d.show()
        mfv.findViewById<EditText>(R.id.name).setText(data.getString("name"))
        mfv.findViewById<EditText>(R.id.desc).setText(data.getString("desc"))
        mfv.findViewById<TextView>(R.id.start).text = data.getString("start")
        mfv.findViewById<TextView>(R.id.end).text = data.getString("end")

        mfv.findViewById<TextView>(R.id.start).setOnClickListener {
            setTime = "start"
            val calendar: Calendar = Calendar.getInstance()
            if (mfv.findViewById<TextView>(R.id.start).text.toString() != ""){
                val date = dateFormat.parse(mfv.findViewById<TextView>(R.id.start).text.toString())
                calendar.time = date
            }
            day = calendar.get(Calendar.DAY_OF_MONTH)
            month = calendar.get(Calendar.MONTH)
            year = calendar.get(Calendar.YEAR)
            val datePickerDialog = DatePickerDialog(this@MeetDetail,
                this@MeetDetail,
                year,
                month,
                day)
            datePickerDialog.show()
        }
        mfv.findViewById<TextView>(R.id.end).setOnClickListener {
            setTime = "end"
            val calendar: Calendar = Calendar.getInstance()
            if (mfv.findViewById<TextView>(R.id.end).text.toString() != ""){
                val date = dateFormat.parse(mfv.findViewById<TextView>(R.id.end).text.toString())
                calendar.time = date
            }
            day = calendar.get(Calendar.DAY_OF_MONTH)
            month = calendar.get(Calendar.MONTH)
            year = calendar.get(Calendar.YEAR)
            val datePickerDialog = DatePickerDialog(this, this, year, month, day)
            datePickerDialog.show()
        }

        mfv.findViewById<Button>(R.id.d_cancel).setOnClickListener {
            d.dismiss()
        }
        mfv.findViewById<Button>(R.id.btn_save).setOnClickListener {
            val queue = newRequestQueue(this)
            val url = MeetApi().update()

            val request = object: JsonObjectRequest(Method.PUT, url, null, { response ->
                val dd = response.getJSONObject("data")
                Storages(this).setData("meets", bundle.getString("key").toString(), dd)
                mfv.findViewById<EditText>(R.id.name).text.clear()
                mfv.findViewById<EditText>(R.id.desc).text.clear()
                mfv.findViewById<TextView>(R.id.start).text = null
                mfv.findViewById<TextView>(R.id.end).text = null
                finish()
                val intent = Intent(this@MeetDetail, MeetDetail::class.java)
                intent.putExtra("key", bundle.getString("key"))
                startActivity(intent)
                dialog.dismiss()
                d.dismiss()
            }, { error ->
                if (error?.networkResponse != null) {
                    val errorJSON = JSONObject(String(error.networkResponse.data))
                    if (errorJSON.getInt("code") == 406) {
                        Toast.makeText(this,
                            errorJSON.getJSONArray("message")[0].toString(),
                            Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this, "Tidak dapat terhubung ke server", Toast.LENGTH_LONG)
                        .show()
                }
                dialog.dismiss()
                d.dismiss()
            }){
                override fun getHeaders(): MutableMap<String, String> {
                    val headers = java.util.HashMap<String, String>()
                    headers["Authorization"] = "Bearer " + token
                    headers["uuid"] = data.getString("uuid")
                    headers["name"] = mfv.findViewById<EditText>(R.id.name).text.toString()
                    headers["desc"] = mfv.findViewById<EditText>(R.id.desc).text.toString()
                    headers["start"] = mfv.findViewById<TextView>(R.id.start).text.toString()
                    headers["end"] = mfv.findViewById<TextView>(R.id.end).text.toString()
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
    override fun onDateSet(p0: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        myDay = dayOfMonth
        myYear = year
        myMonth = month+1
        val gd = if (setTime == "start"){
            mfv.findViewById<TextView>(R.id.start).text.toString()
        }else{
            mfv.findViewById<TextView>(R.id.end).text.toString()
        }
        val calendar: Calendar = Calendar.getInstance()
        if (gd != ""){
            val date = dateFormat.parse(gd)
            calendar.time = date
        }
        hour = calendar.get(Calendar.HOUR)
        minute = calendar.get(Calendar.MINUTE)
        val timePickerDialog = TimePickerDialog(this@MeetDetail, this@MeetDetail, hour, minute,
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
            mfv.findViewById(R.id.start)
        }else{
            mfv.findViewById(R.id.end)
        }
        et.text = "${myYear}-${mm}-${dd} ${hh}:${ii}:00"
    }
}