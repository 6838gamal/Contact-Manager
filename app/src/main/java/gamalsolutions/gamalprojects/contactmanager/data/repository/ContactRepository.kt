package gamalsolutions.gamalprojects.contactmanager.data.repository

import gamalsolutions.gamalprojects.contactmanager.data.local.ContactDao
import gamalsolutions.gamalprojects.contactmanager.data.local.ContactEntity
import gamalsolutions.gamalprojects.contactmanager.data.local.StatsPreferences
import gamalsolutions.gamalprojects.contactmanager.data.local.StoredPreferences
import gamalsolutions.gamalprojects.contactmanager.data.local.toDomain
import gamalsolutions.gamalprojects.contactmanager.data.local.toEntity
import gamalsolutions.gamalprojects.contactmanager.data.model.Contact
import gamalsolutions.gamalprojects.contactmanager.data.model.ContactCategory
import gamalsolutions.gamalprojects.contactmanager.data.model.ContactPlatform
import gamalsolutions.gamalprojects.contactmanager.data.model.DuplicateStrategy
import gamalsolutions.gamalprojects.contactmanager.data.model.ImportOptions
import gamalsolutions.gamalprojects.contactmanager.data.model.ImportSummary
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

    suspend fun updateCategoryForIds(ids: List<Long>, category: ContactCategory): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                if (ids.isNotEmpty()) {
                    contactDao.updateCategoryForIds(
                        ids = ids,
                        category = category.storageKey,
                        updatedAt = System.currentTimeMillis()
                    )
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun updatePlatformForIds(ids: List<Long>, platform: ContactPlatform): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                if (ids.isNotEmpty()) {
                    contactDao.updatePlatformForIds(
                        ids = ids,
                        platform = platform.storageKey,
                        updatedAt = System.currentTimeMillis()
                    )
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun toggleFavorite(contact: Contact): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            contactDao.updateFavorite(
                id = contact.id,
                isFavorite = !contact.isFavorite,
                updatedAt = System.currentTimeMillis()
            )
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

    /**
     * استيراد الجهات المختارة مع خيارات التصنيف، وسم المنصة (واتساب/تيليجرام)، البادئة، وسياسة التكرار.
     */
    suspend fun importContactsWithOptions(
        contacts: List<Contact>,
        options: ImportOptions
    ): Result<ImportSummary> = withContext(Dispatchers.IO) {
        try {
            val existingPhones = contactDao.getAllNormalizedPhones().toMutableSet()
            val seenInBatch = mutableSetOf<String>()
            val newEntities = mutableListOf<ContactEntity>()
            var updatedCount = 0
            var skippedCount = 0
            val prefix = options.namePrefix.trim()

            for (rawContact in contacts) {
                val normalized = VCardHelper.normalizePhone(rawContact.phone)
                if (normalized.isBlank() || !seenInBatch.add(normalized)) {
                    skippedCount++
                    continue
                }

                val finalName = if (prefix.isNotEmpty() && !rawContact.name.startsWith(prefix)) {
                    "$prefix ${rawContact.name.trim()}"
                } else {
                    rawContact.name.trim()
                }
                val resolvedCategory = if (options.assignCategory != ContactCategory.GENERAL) {
                    options.assignCategory
                } else {
                    rawContact.category
                }
                val resolvedPlatform = if (options.sourcePlatform != ContactPlatform.ALL) {
                    options.sourcePlatform
                } else {
                    rawContact.preferredPlatform
                }

                if (existingPhones.contains(normalized)) {
                    if (options.duplicateStrategy == DuplicateStrategy.UPDATE_EXISTING) {
                        val existing = contactDao.getContactByNormalizedPhone(normalized)
                        if (existing != null) {
                            val updatedEntity = existing.copy(
                                name = finalName.ifBlank { existing.name },
                                email = rawContact.email.ifBlank { existing.email },
                                company = rawContact.company.ifBlank { existing.company },
                                notes = rawContact.notes.ifBlank { existing.notes },
                                category = resolvedCategory.storageKey,
                                preferredPlatform = resolvedPlatform.storageKey,
                                updatedAt = System.currentTimeMillis()
                            )
                            contactDao.updateContact(updatedEntity)
                            updatedCount++
                        } else {
                            skippedCount++
                        }
                    } else {
                        skippedCount++
                    }
                    continue
                }

                existingPhones.add(normalized)
                var synced = false
                var sysId: Long? = null
                val preparedContact = rawContact.copy(
                    name = finalName,
                    category = resolvedCategory,
                    preferredPlatform = resolvedPlatform
                )

                if (options.alsoSaveToPhonebook) {
                    val phonebookRes = systemContactsRepository.insertIntoDevicePhonebook(preparedContact)
                    if (phonebookRes.isSuccess) {
                        synced = true
                        sysId = phonebookRes.getOrNull()
                    }
                }

                newEntities.add(
                    preparedContact.copy(
                        isSyncedToDevice = synced,
                        systemContactId = sysId
                    ).toEntity(normalized)
                )
            }

            if (newEntities.isNotEmpty()) {
                contactDao.insertContacts(newEntities)
            }
            val totalChanged = newEntities.size + updatedCount
            if (totalChanged > 0) {
                statsPreferences.incrementImported(totalChanged)
            }
            Result.success(
                ImportSummary(
                    importedCount = newEntities.size,
                    updatedCount = updatedCount,
                    skippedCount = skippedCount
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recordExport() = statsPreferences.incrementExported()

    suspend fun recordWhatsAppOpened() = statsPreferences.incrementWhatsAppOpened()

    suspend fun recordBackupCreated() = statsPreferences.updateLastBackupTimestamp()

    suspend fun setAppLanguage(languageCode: String) = statsPreferences.setAppLanguage(languageCode)
}
