package com.lowjunkie.esummit.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.lowjunkie.esummit.R
import com.lowjunkie.esummit.models.Destination
import kotlinx.android.synthetic.main.card_item.view.*

class HomeAdapter : RecyclerView.Adapter<HomeAdapter.ItemViewHolder>() {



    private var onItemClickListener : ((Destination) -> Unit) ?= null

    fun setOnItemClickListener(listener: (Destination) -> Unit){
        onItemClickListener = listener
    }

    inner class ItemViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    private val differCallback = object : DiffUtil.ItemCallback<Destination>(){
        override fun areItemsTheSame(oldItem: Destination, newItem: Destination): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Destination, newItem: Destination): Boolean {
            return oldItem.name == newItem.name
        }

    }

    val differ = AsyncListDiffer(this, differCallback)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.card_item,parent,false ))
    }

    override fun getItemCount(): Int {
//        return 2
        return differ.currentList.size
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = differ.currentList[position]
        //val storageRef = FirebaseStorage.getInstance()
        with(holder){






            itemView.setOnClickListener {
                onItemClickListener?.let { it(item) }
            }
            itemView.apply {
                tvName.text = item.name
                tvDept.text = item.dept
                tvLocation.text = item.location
                Glide
                    .with(itemView.context)
                    .load(item.imageUri)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(ivProduct)
            }

        }

    }

}