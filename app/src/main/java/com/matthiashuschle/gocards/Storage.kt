package com.matthiashuschle.gocards

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.persistence.db.SupportSQLiteDatabase
import android.util.Log
import android.arch.persistence.room.*
import android.support.annotation.NonNull
import android.arch.persistence.room.Room
import android.content.Context
import android.support.annotation.WorkerThread
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch


@Entity(
    tableName = "card_set",
    indices = [
        Index(
            value=["name"],
            unique=true
        )
    ]
)
data class CardSet(
    @PrimaryKey(autoGenerate = true) var sid: Long?,
    @NonNull
    @ColumnInfo(name = "name") var name: String,
    @ColumnInfo(name = "info") var info: String?,
    @ColumnInfo(name = "info_left") var infoLeft: String?,
    @ColumnInfo(name = "info_right") var infoRight: String?,
    @ColumnInfo(name = "active") var active: Boolean
){
    constructor(): this(
        sid = null,
        name = "",
        info = null,
        infoLeft = null,
        infoRight = null,
        active = false
    )
    constructor(name: String, info: String?, infoLeft: String?, infoRight: String?): this(
        sid = null,
        name = name,
        info = info,
        infoLeft = infoLeft,
        infoRight = infoRight,
        active = false
    )
}

@Entity(
    tableName = "card",
    foreignKeys = [
        ForeignKey(
            entity = CardSet::class,
            parentColumns=["sid"],
            childColumns=["fk_card_set_id"]
        )
    ]
)
data class Card(
    @PrimaryKey(autoGenerate = true) var cid: Long?,
    @ColumnInfo(name = "fk_card_set_id") var fkCardSetId: Long?,
    @ColumnInfo(name = "left") var left: String?,
    @ColumnInfo(name = "right") var right: String?,
    @ColumnInfo(name = "info_left") var infoLeft: String?,
    @ColumnInfo(name = "info_right") var infoRight: String?,
    @ColumnInfo(name = "last_seen") var lastSeen: String?,
    @ColumnInfo(name = "streak") var streak: Int,
    @ColumnInfo(name = "hidden_until") var hiddenUntil: String?
){
    constructor(cardSetId: Long): this(
        cid = null,
        fkCardSetId = cardSetId,
        left = null,
        right = null,
        infoLeft = null,
        infoRight = null,
        lastSeen = null,
        streak = 0,
        hiddenUntil = null
    )
    constructor(cardSet: CardSet, left: String?, right: String?): this(
        cid = null,
        fkCardSetId = cardSet.sid,
        left = left,
        right = right,
        infoLeft = cardSet.infoLeft,
        infoRight = cardSet.infoRight,
        lastSeen = null,
        streak = 0,
        hiddenUntil = null
    )
}

@Dao
interface CardSetDao {
    @Query("SELECT * FROM card_set")
    fun getAll(): LiveData<List<CardSet>>

//    @Query("SELECT * FROM card_set WHERE sid IN (:setIds)")
//    fun loadAllByIds(setIds: IntArray): LiveData<List<CardSet>>
//
//    @Query("SELECT * FROM card_set WHERE name LIKE :name LIMIT 1")
//    fun findByName(name: String): LiveData<CardSet>

    @Query("SELECT COUNT(*) FROM card_set")
    fun countSets(): Boolean

    @Insert
    fun insert(vararg card_sets: CardSet): Array<Long>

    @Delete
    fun delete(card_set: CardSet)
}

@Dao
interface CardDao {
    @Query("SELECT * FROM card")
    fun getAll(): LiveData<List<Card>>

//    @Query("SELECT * FROM card WHERE cid IN (:cardIds)")
//    fun loadAllByIds(cardIds: IntArray): LiveData<List<Card>>
//
//    @Query("SELECT * FROM card WHERE fk_card_set_id LIKE :setId")
//    fun findBySetId(setId: Int): LiveData<List<Card>>

    @Insert
    fun insert(vararg card: Card): Array<Long>

    @Delete
    fun delete(card: Card)
}


@Database(entities = [CardSet::class, Card::class], version = 1)
abstract class CardDatabase : RoomDatabase() {

    abstract fun cardSetDao(): CardSetDao
    abstract fun cardDao(): CardDao

    companion object {
        @Volatile
        private var INSTANCE: CardDatabase? = null

        fun getInstance(context: Context, scope: CoroutineScope): CardDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val newInstance = Room.databaseBuilder(
                    context.applicationContext,
                    CardDatabase::class.java, "card_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(CardDatabaseCallback(scope))
                    .build()
                INSTANCE = newInstance
                return newInstance
            }
        }

        fun destroyInstance() {
            INSTANCE = null
        }

        private class CardDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateDatabase(database.cardDao(), database.cardSetDao())
                    }
                }
            }
        }

        fun populateDatabase(cardDao: CardDao, cardSetDao: CardSetDao) {
            Log.w("GoCards", "entering populateDatabase")
            val nSets = cardSetDao.countSets()
            Log.w("GoCards", "found $nSets entries")
            if (cardSetDao.countSets()) return
            Log.w("GoCards", "filling test entry")
            val cardSet = CardSet(null, "test", null, "EN", "DE", false)
            val insertedSIds = cardSetDao.insert(cardSet)
            if (insertedSIds.isNotEmpty()) cardSet.sid = insertedSIds[0]
            val card = Card(cardSet, "hello", "hallo")
            val insertedCIds = cardSetDao.insert(cardSet)
            if (insertedCIds.isNotEmpty()) card.cid = insertedCIds[0]
        }
    }
}


class CardRepository(
    private val cardDao: CardDao,
    private val cardSetDao: CardSetDao
) {
    val allCards: LiveData<List<Card>> = cardDao.getAll()
    val allCardSets: LiveData<List<CardSet>> = cardSetDao.getAll()

    @WorkerThread
    suspend fun insertCard(card: Card) {
        cardDao.insert(card)
    }

    @WorkerThread
    suspend fun insertCardSet(cardSet: CardSet) {
        cardSetDao.insert(cardSet)
    }

    @WorkerThread
    suspend fun deleteCard(card: Card) {
        cardDao.delete(card)
    }

    @WorkerThread
    suspend fun deleteCardSet(cardSet: CardSet) {
        cardSetDao.delete(cardSet)
    }
}


class CardViewModel(application: Application) : AndroidViewModel(application) {

    private var parentJob = Job()

    private val coroutineContext: CoroutineContext
        get() = parentJob + Dispatchers.Main

    private val scope = CoroutineScope(coroutineContext)

    private val repository: CardRepository
    val allCards: LiveData<List<Card>>
    val allCardSets: LiveData<List<CardSet>>

    init {
        val cardDao = CardDatabase.getInstance(application, scope).cardDao()
        val cardSetDao = CardDatabase.getInstance(application, scope).cardSetDao()
        repository = CardRepository(cardDao, cardSetDao)
        allCards = repository.allCards
        allCardSets = repository.allCardSets
    }

    override fun onCleared() {
        super.onCleared()
        parentJob.cancel()
    }

    fun insertCard(card: Card) = scope.launch(Dispatchers.IO) {
        repository.insertCard(card)
    }

    fun insertCardSet(cardSet: CardSet) = scope.launch(Dispatchers.IO) {
        repository.insertCardSet(cardSet)
    }
}



