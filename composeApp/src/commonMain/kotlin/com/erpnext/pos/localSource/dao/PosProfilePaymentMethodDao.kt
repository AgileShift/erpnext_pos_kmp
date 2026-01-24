package com.erpnext.pos.localSource.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.erpnext.pos.localSource.entities.PosProfilePaymentMethodEntity

data class ResolvedPaymentMethod(
    val mopName: String,
    val type: String?,
    val enabled: Boolean,
    val isDefault: Boolean,
    val currency: String?,
    val account: String?,
    val allowInReturns: Boolean,
    val idx: Int,
    val enabledInProfile: Boolean
)

@Dao
interface PosProfilePaymentMethodDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<PosProfilePaymentMethodEntity>)

    @Query(
        """
        SELECT
            pm.mop_name AS mopName,
            mop.type AS type,
            mop.enabled AS enabled,
            pm.is_default AS isDefault,
            mop.currency AS currency,
            mop.account AS account,
            pm.allow_in_returns AS allowInReturns,
            pm.idx AS idx,
            pm.enabled_in_profile AS enabledInProfile
        FROM tabPOSProfilePaymentMethod pm
        INNER JOIN tabModeOfPayment mop
            ON mop.name = pm.mop_name
        WHERE pm.profile_id = :profileId
        ORDER BY pm.idx ASC
        """
    )
    suspend fun getResolvedMethodsForProfile(profileId: String): List<ResolvedPaymentMethod>

    @Query(
        """
        SELECT COUNT(*) FROM tabPOSProfilePaymentMethod pm
        INNER JOIN tabModeOfPayment mop
            ON mop.name = pm.mop_name
        WHERE pm.profile_id = :profileId
        """
    )
    suspend fun countResolvedForProfile(profileId: String): Int

    @Query("SELECT COUNT(*) FROM tabPOSProfilePaymentMethod WHERE profile_id = :profileId")
    suspend fun countRelationsForProfile(profileId: String): Int

    @Query("SELECT COUNT(*) FROM tabPOSProfilePaymentMethod")
    suspend fun countAllRelations(): Int

    @Query(
        """
        DELETE FROM tabPOSProfilePaymentMethod
        WHERE profile_id = :profileId
          AND mop_name NOT IN (:activeMops)
        """
    )
    suspend fun deleteStaleForProfile(profileId: String, activeMops: List<String>)

    @Query("DELETE FROM tabPOSProfilePaymentMethod WHERE profile_id = :profileId")
    suspend fun deleteAllForProfile(profileId: String)

    @Query("DELETE FROM tabPOSProfilePaymentMethod WHERE profile_id NOT IN (:profileIds)")
    suspend fun deleteForProfilesNotIn(profileIds: List<String>)

    @Query("DELETE FROM tabPOSProfilePaymentMethod")
    suspend fun deleteAllRelations()
}
