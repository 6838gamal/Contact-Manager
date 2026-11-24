package gamalsolutions.gamalprojects.contactmanager.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import gamalsolutions.gamalprojects.contactmanager.data.model.Contact
import gamalsolutions.gamalprojects.contactmanager.data.model.ContactCategory
import gamalsolutions.gamalprojects.contactmanager.data.model.ContactPlatform

/**
 * كيان قاعدة بيانات Room لجدول جهات الاتصال المحلية.
 */
@Entity(
    tableName = "contacts",
    indices = [
        Index(value = ["normalizedPhone"]),
        Index(value = ["name"])
    ]
)
data class ContactEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val phone: String,
    val normalizedPhone: String,
    val email: String = "",
    val company: String = "",
    val photoUri: String? = null,
    val notes: String = "",
    val category: String = ContactCategory.GENERAL.storageKey,
    val preferredPlatform: String = ContactPlatform.ALL.storageKey,
    val isFavorite: Boolean = false,
    val systemContactId: Long? = null,
    val isSyncedToDevice: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

fun ContactEntity.toDomain(): Contact = Contact(
    id = id,
    name = name,
    phone = phone,
    email = email,
    company = company,
    photoUri = photoUri,
    notes = notes,
    category = ContactCategory.fromKey(category),
    preferredPlatform = ContactPlatform.fromKey(preferredPlatform),
    isFavorite = isFavorite,
    systemContactId = systemContactId,
    isSyncedToDevice = isSyncedToDevice,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Contact.toEntity(normalizedPhone: String): ContactEntity = ContactEntity(
    id = id,
    name = name.trim(),
    phone = phone.trim(),
    normalizedPhone = normalizedPhone,
    email = email.trim(),
    company = company.trim(),
    photoUri = photoUri,
    notes = notes.trim(),
    category = category.storageKey,
    preferredPlatform = preferredPlatform.storageKey,
    isFavorite = isFavorite,
    systemContactId = systemContactId,
    isSyncedToDevice = isSyncedToDevice,
    createdAt = createdAt,
    updatedAt = System.currentTimeMillis()
)
