package kz.example.splitbill

import android.app.Application
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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

    /** Lives as long as the app process; used for work that is not tied to a screen. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Bills older than 30 days in the trash are removed for good.
        appScope.launch { container.repository.purgeOldTrash() }
    }
}
