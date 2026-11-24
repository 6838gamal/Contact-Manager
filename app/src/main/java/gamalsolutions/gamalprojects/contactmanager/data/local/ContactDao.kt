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

    @Query("SELECT * FROM contacts ORDER BY name COLLATE NOCASE ASC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllContactsSnapshot(): List<ContactEntity>

    @Query(
        """
        SELECT * FROM contacts 
        WHERE name LIKE '%' || :query || '%' 
           OR phone LIKE '%' || :query || '%' 
           OR company LIKE '%' || :query || '%'
           OR email LIKE '%' || :query || '%'
        ORDER BY name COLLATE NOCASE ASC
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
}
