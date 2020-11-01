package com.asd412id.jmeet.fragments

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.asd412id.jmeet.MeetDetail
import com.asd412id.jmeet.R
import com.asd412id.jmeet.adapters.MeetsAdapter
import com.asd412id.jmeet.modules.MeetClass
import com.asd412id.jmeet.modules.Storages
import java.util.*

class Meets : Fragment() {
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var builder: AlertDialog.Builder
    private lateinit var dialog: AlertDialog
    private var loaded = false
    private var retry = 5
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_meets, container, false)
        swipeRefreshLayout = root.findViewById(R.id.refresh)
        swipeRefreshLayout.isRefreshing = true
        loadData(root)
        swipeRefreshLayout.setOnRefreshListener {
            retry = 5
            loaded = false
            loadData(root)
        }
        root.findViewById<ListView>(R.id.meets_list).setOnItemClickListener { adapterView, view, i, l ->
            val intent = Intent(requireContext(),MeetDetail::class.java)
            intent.putExtra("key",i.toString())
            startActivity(intent)
        }
        root.findViewById<ListView>(R.id.meets_list).setOnItemLongClickListener { adapterView, view, i, l ->
            val clipboard = activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val dt = Storages(requireContext()).getData("meets",i.toString())
            val clip = ClipData.newPlainText(UUID.randomUUID().toString(),dt?.getString("_token"))
            clipboard.setPrimaryClip(clip)
            Toast.makeText(requireContext(),"Token pertemuan berhasil disalin!",Toast.LENGTH_LONG).show()
            true
        }
        return root
    }

    private fun loadData(root: View) {
        val meets = MeetClass(requireContext())
        if (!loaded){
            loaded = true
            meets.getServerDataMeet()
        }
        val statusReq: Boolean? = Storages(requireContext()).getData("configs","meetsloaded")?.getBoolean("status")
        if (statusReq == null){
            if (retry > 0){
                android.os.Handler(Looper.getMainLooper()).postDelayed({
                    retry--
                    loadData(root)
                },1000)
            }
        }else{
            meets.getLocalDataMeet()
            root.findViewById<ListView>(R.id.meets_list).adapter = MeetsAdapter(meets.name,meets.token,meets.desc,meets.start,meets.end)
            swipeRefreshLayout.isRefreshing = false
        }
    }
}