package gamalsolutions.gamalprojects.contactmanager.data.repository

import gamalsolutions.gamalprojects.contactmanager.data.local.ContactDao
import gamalsolutions.gamalprojects.contactmanager.data.local.StatsPreferences
import gamalsolutions.gamalprojects.contactmanager.data.local.StoredPreferences
import gamalsolutions.gamalprojects.contactmanager.data.local.toDomain
import gamalsolutions.gamalprojects.contactmanager.data.local.toEntity
import gamalsolutions.gamalprojects.contactmanager.data.model.Contact
import gamalsolutions.gamalprojects.contactmanager.utils.VCardHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * المستودع الرئيسي الذي يجمع بين قاعدة البيانات المحلية (Room) ودفتر هاتف الجهاز (ContactsContract).
 */
class ContactRepository(
    private val contactDao: ContactDao,
    private val systemContactsRepository: SystemContactsRepository,
    private val statsPreferences: StatsPreferences
) {

    val allContacts: Flow<List<Contact>> = contactDao.getAllContacts().map { list ->
        list.map { it.toDomain() }
    }

    val preferencesFlow: Flow<StoredPreferences> = statsPreferences.preferencesFlow

    fun searchContacts(query: String): Flow<List<Contact>> {
        val trimmed = query.trim()
        return if (trimmed.isEmpty()) {
            allContacts
        } else {
            contactDao.searchContacts(trimmed).map { list -> list.map { it.toDomain() } }
        }
    }

    suspend fun getContactById(id: Long): Contact? = withContext(Dispatchers.IO) {
        contactDao.getContactById(id)?.toDomain()
    }

    suspend fun getAllContactsSnapshot(): List<Contact> = withContext(Dispatchers.IO) {
        contactDao.getAllContactsSnapshot().map { it.toDomain() }
    }

    suspend fun getAllNormalizedPhones(): Set<String> = withContext(Dispatchers.IO) {
        contactDao.getAllNormalizedPhones().toSet()
    }

    suspend fun saveContact(
        contact: Contact,
        alsoSaveToPhonebook: Boolean
    ): Result<Contact> = withContext(Dispatchers.IO) {
        try {
            var syncedToDevice = contact.isSyncedToDevice
            var systemContactId = contact.systemContactId

            if (alsoSaveToPhonebook && !syncedToDevice) {
                val phonebookResult = systemContactsRepository.insertIntoDevicePhonebook(contact)
                if (phonebookResult.isSuccess) {
                    syncedToDevice = true
                    systemContactId = phonebookResult.getOrNull()
                }
            }

            val normalized = VCardHelper.normalizePhone(contact.phone)
            val entity = contact.copy(
                isSyncedToDevice = syncedToDevice,
                systemContactId = systemContactId
            ).toEntity(normalized)

            val savedId = if (entity.id == 0L) {
                contactDao.insertContact(entity)
            } else {
                contactDao.updateContact(entity)
                entity.id
            }

            Result.success(
                contact.copy(
                    id = savedId,
                    isSyncedToDevice = syncedToDevice,
                    systemContactId = systemContactId
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteContact(contact: Contact): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val normalized = VCardHelper.normalizePhone(contact.phone)
            contactDao.deleteContact(contact.toEntity(normalized))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteContactsByIds(ids: List<Long>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (ids.isNotEmpty()) {
                contactDao.deleteContactsByIds(ids)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * مزامنة جهات الاتصال من دفتر هاتف الجهاز إلى قاعدة البيانات المحلية مع منع التكرار.
     */
    suspend fun syncFromDevicePhonebook(): Result<Int> = withContext(Dispatchers.IO) {
        val deviceResult = systemContactsRepository.readDeviceContacts()
        if (deviceResult.isFailure) {
            return@withContext Result.failure(
                deviceResult.exceptionOrNull() ?: IllegalStateException("Sync failed")
            )
        }

        try {
            val deviceContacts = deviceResult.getOrDefault(emptyList())
            val existingPhones = contactDao.getAllNormalizedPhones().toMutableSet()
            val toInsert = deviceContacts.filter { contact ->
                val norm = VCardHelper.normalizePhone(contact.phone)
                norm.isNotBlank() && existingPhones.add(norm)
            }.map { contact ->
                contact.toEntity(VCardHelper.normalizePhone(contact.phone))
            }

            if (toInsert.isNotEmpty()) {
                contactDao.insertContacts(toInsert)
            }
            Result.success(toInsert.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertImportedContacts(
        contacts: List<Contact>,
        alsoSaveToPhonebook: Boolean
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val existingPhones = contactDao.getAllNormalizedPhones().toMutableSet()
            val entities = mutableListOf<gamalsolutions.gamalprojects.contactmanager.data.local.ContactEntity>()

            for (contact in contacts) {
                val normalized = VCardHelper.normalizePhone(contact.phone)
                if (normalized.isNotBlank() && existingPhones.add(normalized)) {
                    var synced = false
                    var sysId: Long? = null
                    if (alsoSaveToPhonebook) {
                        val phonebookRes = systemContactsRepository.insertIntoDevicePhonebook(contact)
                        if (phonebookRes.isSuccess) {
                            synced = true
                            sysId = phonebookRes.getOrNull()
                        }
                    }
                    entities.add(
                        contact.copy(
                            isSyncedToDevice = synced,
                            systemContactId = sysId
                        ).toEntity(normalized)
                    )
                }
            }

            if (entities.isNotEmpty()) {
                contactDao.insertContacts(entities)
                statsPreferences.incrementImported(entities.size)
            }
            Result.success(entities.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recordExport() = statsPreferences.incrementExported()

    suspend fun recordWhatsAppOpened() = statsPreferences.incrementWhatsAppOpened()

    suspend fun recordBackupCreated() = statsPreferences.updateLastBackupTimestamp()

    suspend fun setAppLanguage(languageCode: String) = statsPreferences.setAppLanguage(languageCode)
}
