package com.luxshare.home.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.luxshare.home.adapter.MeetingAdapter.MeetingViewHolder
import com.luxshare.home.bean.Meeting
import com.luxshare.home.databinding.MeetingItemBinding

/**
 * @author hudebo
 *
 * @desc 功能描述
 * @date 2024/12/9 17:34
 */
class MeetingAdapter(private val meetingClickCallback: MeetingClickCallback?) :
    RecyclerView.Adapter<MeetingViewHolder>() {
    private val meetings = mutableListOf<Meeting>()

    fun setList(items: List<Meeting>) {
        meetings.clear()
        meetings.addAll(items)
        notifyItemRangeChanged(0, itemCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MeetingViewHolder {
        val meetingItemBinding = MeetingItemBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return MeetingViewHolder(meetingItemBinding, meetingClickCallback)
    }

    override fun onBindViewHolder(holder: MeetingViewHolder, position: Int) {
        val data = meetings[position]
        holder.bind(data)
    }

    override fun getItemCount(): Int {
        return meetings.size
    }

    class MeetingViewHolder(
        private val itemBinding: MeetingItemBinding,
        private val meetingClickCallback: MeetingClickCallback?) : RecyclerView.ViewHolder(
        itemBinding.getRoot()) {
        fun bind(meeting: Meeting) {
            itemBinding.meetingName.text = meeting.meetingName
            itemView.setOnClickListener {
                meetingClickCallback?.onClick(meeting)
            }
            itemBinding.share.setOnClickListener {
                meetingClickCallback?.share(meeting)
            }
            itemBinding.delete.setOnClickListener {
                meetingClickCallback?.delete(meeting)
            }
            itemBinding.meetingInfo.setOnClickListener {
                meetingClickCallback?.info(meeting)
            }
        }
    }

    fun delete(meeting: Meeting) {
        meetings.remove(meeting)
        notifyDataSetChanged()
    }

    interface MeetingClickCallback {
        fun onClick(meeting: Meeting)

        fun share(meeting: Meeting)

        fun delete(meeting: Meeting)

        fun info(meeting: Meeting)
    }
}
