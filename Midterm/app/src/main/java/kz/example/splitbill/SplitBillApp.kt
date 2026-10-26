package kz.example.splitbill

import android.app.Application
import android.content.Context
import kz.example.splitbill.data.AppDatabase
import kz.example.splitbill.data.SplitBillRepository

/** Simple manual dependency injection: one database and one repository for the whole app. */
class AppContainer(context: Context) {
    private val database by lazy { AppDatabase.create(context) }
    val repository by lazy { SplitBillRepository(database.dao()) }
}

class SplitBillApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
