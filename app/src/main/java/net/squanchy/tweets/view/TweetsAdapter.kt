package net.squanchy.tweets.view

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup

import net.squanchy.R
import net.squanchy.tweets.domain.TweetLinkInfo
import net.squanchy.tweets.domain.view.TweetViewModel

class TweetsAdapter(private val context: Context) : RecyclerView.Adapter<TweetViewHolder>() {

    private var tweets: List<TweetViewModel> = emptyList()
    private lateinit var listener: (TweetLinkInfo) -> Unit

    val isEmpty: Boolean
        get() = tweets.isEmpty()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TweetViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_tweet, parent, false)
        return TweetViewHolder(view)
    }

    override fun onBindViewHolder(holder: TweetViewHolder, position: Int) {
        holder.updateWith(tweets[position], listener)
    }

    override fun getItemCount() = tweets.size

    fun updateWith(tweets: List<TweetViewModel>, listener: (TweetLinkInfo) -> Unit) {
        this.tweets = tweets
        this.listener = listener
        notifyDataSetChanged()
    }
}
