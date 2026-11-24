package gamalsolutions.gamalprojects.contactmanager.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * واجهة الوصول إلى بيانات جهات الاتصال في Room باستخدام Flow للاستجابة الفورية.
 */
@Dao
interface ContactDao {

    @Query("SELECT * FROM contacts ORDER BY isFavorite DESC, name COLLATE NOCASE ASC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts ORDER BY isFavorite DESC, name COLLATE NOCASE ASC")
    suspend fun getAllContactsSnapshot(): List<ContactEntity>

    @Query(
        """
        SELECT * FROM contacts 
        WHERE name LIKE '%' || :query || '%' 
           OR phone LIKE '%' || :query || '%' 
           OR company LIKE '%' || :query || '%'
           OR email LIKE '%' || :query || '%'
           OR category LIKE '%' || :query || '%'
        ORDER BY isFavorite DESC, name COLLATE NOCASE ASC
        """
    )
    fun searchContacts(query: String): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE id = :id LIMIT 1")
    suspend fun getContactById(id: Long): ContactEntity?

    @Query("SELECT * FROM contacts WHERE normalizedPhone = :normalizedPhone LIMIT 1")
    suspend fun getContactByNormalizedPhone(normalizedPhone: String): ContactEntity?

    @Query("SELECT normalizedPhone FROM contacts")
    suspend fun getAllNormalizedPhones(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<ContactEntity>): List<Long>

    @Update
    suspend fun updateContact(contact: ContactEntity)

    @Delete
    suspend fun deleteContact(contact: ContactEntity)

    @Query("DELETE FROM contacts WHERE id IN (:ids)")
    suspend fun deleteContactsByIds(ids: List<Long>)

    @Query("UPDATE contacts SET category = :category, updatedAt = :updatedAt WHERE id IN (:ids)")
    suspend fun updateCategoryForIds(ids: List<Long>, category: String, updatedAt: Long)

    @Query("UPDATE contacts SET preferredPlatform = :platform, updatedAt = :updatedAt WHERE id IN (:ids)")
    suspend fun updatePlatformForIds(ids: List<Long>, platform: String, updatedAt: Long)

    @Query("UPDATE contacts SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean, updatedAt: Long)
}
