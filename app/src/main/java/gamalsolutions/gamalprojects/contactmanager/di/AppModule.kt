package gamalsolutions.gamalprojects.contactmanager.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import gamalsolutions.gamalprojects.contactmanager.data.local.AppDatabase
import gamalsolutions.gamalprojects.contactmanager.data.local.StatsPreferences
import gamalsolutions.gamalprojects.contactmanager.data.repository.ContactRepository
import gamalsolutions.gamalprojects.contactmanager.data.repository.SystemContactsRepository
import gamalsolutions.gamalprojects.contactmanager.domain.usecase.ExportContactsUseCase
import gamalsolutions.gamalprojects.contactmanager.domain.usecase.ImportContactsUseCase
import gamalsolutions.gamalprojects.contactmanager.domain.usecase.OpenWhatsAppUseCase
import gamalsolutions.gamalprojects.contactmanager.presentation.contacts.ContactListViewModel
import gamalsolutions.gamalprojects.contactmanager.presentation.edit.EditContactViewModel

/**
 * حاوية حقن التبعيات (Manual DI / AppContainer) بنمط Singleton لتجميع المستودعات وحالات الاستخدام.
 */
class AppModule(context: Context) {

    private val appContext = context.applicationContext

    private val database: AppDatabase by lazy {
        AppDatabase.getInstance(appContext)
    }

    private val statsPreferences: StatsPreferences by lazy {
        StatsPreferences(appContext)
    }

    private val systemContactsRepository: SystemContactsRepository by lazy {
        SystemContactsRepository(appContext)
    }

    val contactRepository: ContactRepository by lazy {
        ContactRepository(
            contactDao = database.contactDao(),
            systemContactsRepository = systemContactsRepository,
            statsPreferences = statsPreferences
        )
    }

    val exportContactsUseCase: ExportContactsUseCase by lazy {
        ExportContactsUseCase(appContext, contactRepository)
    }

    val importContactsUseCase: ImportContactsUseCase by lazy {
        ImportContactsUseCase(appContext, contactRepository)
    }

    val openWhatsAppUseCase: OpenWhatsAppUseCase by lazy {
        OpenWhatsAppUseCase(appContext, contactRepository)
    }

    fun viewModelFactory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when {
                modelClass.isAssignableFrom(ContactListViewModel::class.java) -> {
                    ContactListViewModel(
                        appContext = appContext,
                        repository = contactRepository,
                        exportContactsUseCase = exportContactsUseCase,
                        importContactsUseCase = importContactsUseCase,
                        openWhatsAppUseCase = openWhatsAppUseCase
                    ) as T
                }
                modelClass.isAssignableFrom(EditContactViewModel::class.java) -> {
                    EditContactViewModel(
                        repository = contactRepository
                    ) as T
                }
                else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AppModule? = null

        fun getInstance(context: Context): AppModule {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppModule(context).also { INSTANCE = it }
            }
        }
    }
}
