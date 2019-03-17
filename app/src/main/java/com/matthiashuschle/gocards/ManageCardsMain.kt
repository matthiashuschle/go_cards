package com.matthiashuschle.gocards

import android.support.v7.app.AppCompatActivity
import android.os.Bundle

class ManageCardsMain : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_cards_main)
        createTestDB()
    }

    fun createTestDB() {

    }
}
