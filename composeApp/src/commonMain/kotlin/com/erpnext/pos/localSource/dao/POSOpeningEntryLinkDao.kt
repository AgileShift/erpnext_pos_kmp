package com.erpnext.pos.localSource.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.erpnext.pos.localSource.entities.POSOpeningEntryLinkEntity
import com.erpnext.pos.localSource.entities.PendingOpeningEntrySync

@Dao
interface POSOpeningEntryLinkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(link: POSOpeningEntryLinkEntity)

    @Transaction
    @Query("SELECT * FROM tab_pos_opening_entry_link WHERE pending_sync = 1")
    suspend fun getPendingSync(): List<PendingOpeningEntrySync>

    @Query("SELECT * FROM tab_pos_opening_entry_link WHERE cashbox_id = :cashboxId LIMIT 1")
    suspend fun getByCashboxId(cashboxId: Long): POSOpeningEntryLinkEntity?

    @Query(
        """
        SELECT remote_opening_entry_name
          FROM tab_pos_opening_entry_link
         WHERE cashbox_id = :cashboxId
         LIMIT 1
        """
    )
    suspend fun getRemoteOpeningEntryName(cashboxId: Long): String?

    @Query(
        """
        SELECT remote_closing_entry_name
          FROM tab_pos_opening_entry_link
         WHERE cashbox_id = :cashboxId
         LIMIT 1
        """
    )
    suspend fun getRemoteClosingEntryName(cashboxId: Long): String?

    @Query(
        """
        UPDATE tab_pos_opening_entry_link
           SET remote_opening_entry_name = :remoteName,
               pending_sync = 0
         WHERE id = :id
        """
    )
    suspend fun markSynced(id: Long, remoteName: String)

    @Query(
        """
        UPDATE tab_pos_opening_entry_link
           SET remote_opening_entry_name = :remoteName
         WHERE id = :id
        """
    )
    suspend fun updateRemoteName(id: Long, remoteName: String)

    @Query(
        """
        UPDATE tab_pos_opening_entry_link
           SET remote_closing_entry_name = :remoteClosingName
         WHERE id = :id
        """
    )
    suspend fun updateRemoteClosingName(id: Long, remoteClosingName: String): Int

    @Query(
        """
        UPDATE tab_pos_opening_entry_link
           SET remote_closing_entry_name = :remoteClosingName
         WHERE cashbox_id = :cashboxId
        """
    )
    suspend fun updateRemoteClosingNameByCashboxId(cashboxId: Long, remoteClosingName: String): Int

    @Query(
        """
        UPDATE tab_pos_opening_entry_link
           SET pending_sync = 0
         WHERE cashbox_id = :cashboxId
        """
    )
    suspend fun clearPendingSyncByCashboxId(cashboxId: Long): Int
}
