package com.asd412id.jmeet.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.asd412id.jmeet.R

class MeetsAdapter(val name: MutableList<String>, private val token: MutableList<String>, private val desc: MutableList<String>, private val start: MutableList<String>, private val end: MutableList<String>): BaseAdapter() {
    override fun getCount(): Int = name.size

    override fun getItem(p0: Int): Any = 0

    override fun getItemId(p0: Int): Long = 0

    @SuppressLint("ViewHolder", "SetTextI18n", "CutPasteId")
    override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
        val view = LayoutInflater.from(p2?.context)
                .inflate(R.layout.item_meets,p2,false)

        view.findViewById<TextView>(R.id.name).text = name[p0]
        view.findViewById<TextView>(R.id.token).text = token[p0]
        view.findViewById<TextView>(R.id.desc).text = desc[p0]
        view.findViewById<TextView>(R.id.start).text = start[p0]
        view.findViewById<TextView>(R.id.end).text = end[p0]

        if (desc[p0] == "" || desc[p0] == "null"){
            view.findViewById<TextView>(R.id.desc).visibility = View.GONE
        }

        return view
    }

}