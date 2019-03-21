package com.matthiashuschle.gocards

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast

class ManageActivity : AppCompatActivity() {

    private val newCardSetActivityRequestCode = 1
    private lateinit var cardViewModel: CardViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage)
        setSupportActionBar(findViewById(R.id.toolbar_manage))
        showSpinner()

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerview)
        val adapter = CardSetAdapter(this)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        cardViewModel = ViewModelProviders.of(this).get(CardViewModel::class.java)
        cardViewModel.allCardSets.observe(this, Observer { cardSets ->
            // Update the cached copy of the words in the adapter.
            cardSets?.let { adapter.setCardSets(it) }
        })
        showSpinner(false)
    }

    fun onClickCreateSet(item: MenuItem) {
        val intent = Intent(this@ManageActivity, NewSetActivity::class.java)
        startActivityForResult(intent, newCardSetActivityRequestCode)
    }

    private fun showSpinner(show: Boolean=true) {
        val spinner: ProgressBar = findViewById(R.id.progressBarManage)
        spinner.visibility = if (show) View.VISIBLE else View.GONE
    }

//    fun populateSets() {
//        val recyclerView = findViewById<RecyclerView>(R.id.recyclerview)
//        val adapter = CardSetAdapter(this)
//        recyclerView.adapter = adapter
//        recyclerView.layoutManager = LinearLayoutManager(this)
//    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_manage, menu)
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == newCardSetActivityRequestCode && resultCode == Activity.RESULT_OK) {
            data?.let {
                val cardSet = CardSet(
                    it.getStringExtra(NewSetActivity.EXTRA_REPLY_NAME),
                    it.getStringExtra(NewSetActivity.EXTRA_REPLY_INFO),
                    it.getStringExtra(NewSetActivity.EXTRA_REPLY_LEFT),
                    it.getStringExtra(NewSetActivity.EXTRA_REPLY_RIGHT)
                )
                cardViewModel.insertCardSet(cardSet)
            }
        } else {
            Toast.makeText(
                applicationContext,
                R.string.card_set_not_saved,
                Toast.LENGTH_LONG
            ).show()
        }
    }

//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        return when (item.itemId) {
//            R.id.action_settings -> true
//            else -> super.onOptionsItemSelected(item)
//        }
//    }

}


class CardSetAdapter internal constructor(
    context: Context
) : RecyclerView.Adapter<CardSetAdapter.CardSetViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var cardSets = emptyList<CardSet>() // Cached copy of cardSets

    inner class CardSetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardSetItemView: TextView = itemView.findViewById(R.id.textView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardSetViewHolder {
        val itemView = inflater.inflate(R.layout.recyclerview_item, parent, false)
        return CardSetViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: CardSetViewHolder, position: Int) {
        val current = cardSets[position]
        holder.cardSetItemView.text = current.name
    }

    internal fun setCardSets(words: List<CardSet>) {
        this.cardSets = words
        notifyDataSetChanged()
    }

    override fun getItemCount() = cardSets.size
}
