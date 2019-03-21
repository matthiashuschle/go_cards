package com.matthiashuschle.gocards

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText

class NewSetActivity : AppCompatActivity() {

    private lateinit var createSetViewName: EditText
    private lateinit var createSetViewInfo: EditText
    private lateinit var createSetViewLeft: EditText
    private lateinit var createSetViewRight: EditText

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_set)
        createSetViewName = findViewById(R.id.new_card_set_name)
        createSetViewInfo = findViewById(R.id.new_card_set_info)
        createSetViewLeft = findViewById(R.id.new_card_set_left)
        createSetViewRight = findViewById(R.id.new_card_set_right)

        val button = findViewById<Button>(R.id.button_save_new_set)
        button.setOnClickListener {
            val replyIntent = Intent()
            if (TextUtils.isEmpty(createSetViewName.text)) {
                setResult(Activity.RESULT_CANCELED, replyIntent)
            } else {
                val setName = createSetViewName.text.toString()
                val setInfo = createSetViewInfo.text.toString()
                val setLeft = createSetViewLeft.text.toString()
                val setRight = createSetViewRight.text.toString()
                replyIntent.putExtra(EXTRA_REPLY_NAME, setName)
                replyIntent.putExtra(EXTRA_REPLY_INFO, setInfo)
                replyIntent.putExtra(EXTRA_REPLY_LEFT, setLeft)
                replyIntent.putExtra(EXTRA_REPLY_RIGHT, setRight)
                setResult(Activity.RESULT_OK, replyIntent)
            }
            finish()
        }
    }

    companion object {
        const val EXTRA_REPLY_NAME = "com.matthiashuschle.gocards.REPLY_NAME"
        const val EXTRA_REPLY_INFO = "com.matthiashuschle.gocards.REPLY_INFO"
        const val EXTRA_REPLY_LEFT = "com.matthiashuschle.gocards.REPLY_LEFT"
        const val EXTRA_REPLY_RIGHT = "com.matthiashuschle.gocards.REPLY_RIGHT"
    }
}
