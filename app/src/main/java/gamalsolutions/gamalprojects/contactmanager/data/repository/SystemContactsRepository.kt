package gamalsolutions.gamalprojects.contactmanager.data.repository

import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.Context
import android.provider.ContactsContract
import gamalsolutions.gamalprojects.contactmanager.data.model.Contact
import gamalsolutions.gamalprojects.contactmanager.data.model.ContactPlatform
import gamalsolutions.gamalprojects.contactmanager.utils.PermissionHelper
import gamalsolutions.gamalprojects.contactmanager.utils.VCardHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * مستودع التعامل مع دفتر هاتف الجهاز عبر ContactsContract.
 * ملاحظة: يتم الكشف عن ارتباط الجهة بحسابات واتساب أو واتساب الأعمال أو تيليجرام عبر RawContacts.ACCOUNT_TYPE
 * بدون أي صلاحيات إضافية خارج READ_CONTACTS.
 */
class SystemContactsRepository(private val context: Context) {

    private val contentResolver: ContentResolver
        get() = context.contentResolver

    suspend fun readDeviceContacts(): Result<List<Contact>> = withContext(Dispatchers.IO) {
        if (!PermissionHelper.hasReadContactsPermission(context)) {
            return@withContext Result.failure(SecurityException("READ_CONTACTS permission not granted"))
        }

        try {
            val emailsByContactId = loadEmailsMap()
            val companiesByContactId = loadCompaniesMap()
            val platformsByContactId = loadDetectedPlatformsMap()
            val contactsMap = linkedMapOf<String, Contact>()

            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI
            )

            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
                val numberIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)

                while (cursor.moveToNext()) {
                    val sysId = cursor.getLong(idIdx)
                    val name = cursor.getString(nameIdx)?.trim().orEmpty()
                    val phone = cursor.getString(numberIdx)?.trim().orEmpty()
                    val photoUri = cursor.getString(photoIdx)
                    val normalized = VCardHelper.normalizePhone(phone)

                    if (name.isNotBlank() && normalized.isNotBlank() && !contactsMap.containsKey(normalized)) {
                        contactsMap[normalized] = Contact(
                            name = name,
                            phone = phone,
                            email = emailsByContactId[sysId].orEmpty(),
                            company = companiesByContactId[sysId].orEmpty(),
                            photoUri = photoUri,
                            preferredPlatform = platformsByContactId[sysId] ?: ContactPlatform.ALL,
                            systemContactId = sysId,
                            isSyncedToDevice = true
                        )
                    }
                }
            }

            Result.success(contactsMap.values.toList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertIntoDevicePhonebook(contact: Contact): Result<Long?> = withContext(Dispatchers.IO) {
        if (!PermissionHelper.hasWriteContactsPermission(context)) {
            return@withContext Result.failure(SecurityException("WRITE_CONTACTS permission not granted"))
        }

        try {
            val ops = ArrayList<ContentProviderOperation>()
            val rawContactInsertIndex = 0

            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build()
            )

            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                    )
                    .withValue(
                        ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
                        contact.name.trim()
                    )
                    .build()
            )

            if (contact.phone.isNotBlank()) {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                        .withValue(
                            ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                        )
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, contact.phone.trim())
                        .withValue(
                            ContactsContract.CommonDataKinds.Phone.TYPE,
                            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                        )
                        .build()
                )
            }

            if (contact.email.isNotBlank()) {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                        .withValue(
                            ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE
                        )
                        .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, contact.email.trim())
                        .withValue(
                            ContactsContract.CommonDataKinds.Email.TYPE,
                            ContactsContract.CommonDataKinds.Email.TYPE_WORK
                        )
                        .build()
                )
            }

            if (contact.company.isNotBlank()) {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                        .withValue(
                            ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE
                        )
                        .withValue(
                            ContactsContract.CommonDataKinds.Organization.COMPANY,
                            contact.company.trim()
                        )
                        .withValue(
                            ContactsContract.CommonDataKinds.Organization.TYPE,
                            ContactsContract.CommonDataKinds.Organization.TYPE_WORK
                        )
                        .build()
                )
            }

            val results = contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            val createdId = results.firstOrNull()?.uri?.lastPathSegment?.toLongOrNull()
            Result.success(createdId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun loadDetectedPlatformsMap(): Map<Long, ContactPlatform> {
        val map = mutableMapOf<Long, ContactPlatform>()
        try {
            val projection = arrayOf(
                ContactsContract.RawContacts.CONTACT_ID,
                ContactsContract.RawContacts.ACCOUNT_TYPE
            )
            contentResolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID)
                val accIdx = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE)
                if (idIdx < 0 || accIdx < 0) return map

                while (cursor.moveToNext()) {
                    val contactId = cursor.getLong(idIdx)
                    val accountType = cursor.getString(accIdx).orEmpty()
                    val detected = when {
                        accountType.contains("com.whatsapp.w4b", ignoreCase = true) ->
                            ContactPlatform.WHATSAPP_BUSINESS
                        accountType.contains("com.whatsapp", ignoreCase = true) ->
                            ContactPlatform.WHATSAPP
                        accountType.contains("org.telegram", ignoreCase = true) ->
                            ContactPlatform.TELEGRAM
                        else -> null
                    }
                    if (detected != null && !map.containsKey(contactId)) {
                        map[contactId] = detected
                    }
                }
            }
        } catch (_: Exception) {
            // تجاهل أي قيود مزود جهات اتصال مخصصة من الشركة المصنعة
        }
        return map
    }

    private fun loadEmailsMap(): Map<Long, String> {
        val map = mutableMapOf<Long, String>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Email.CONTACT_ID,
            ContactsContract.CommonDataKinds.Email.ADDRESS
        )
        contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
            val addrIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIdx)
                val email = cursor.getString(addrIdx)?.trim().orEmpty()
                if (email.isNotBlank() && !map.containsKey(id)) {
                    map[id] = email
                }
            }
        }
        return map
    }

    private fun loadCompaniesMap(): Map<Long, String> {
        val map = mutableMapOf<Long, String>()
        val projection = arrayOf(
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.CommonDataKinds.Organization.COMPANY
        )
        val selection = "${ContactsContract.Data.MIMETYPE} = ?"
        val selectionArgs = arrayOf(ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
        contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID)
            val companyIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Organization.COMPANY)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIdx)
                val company = cursor.getString(companyIdx)?.trim().orEmpty()
                if (company.isNotBlank() && !map.containsKey(id)) {
                    map[id] = company
                }
            }
        }
        return map
    }
}
