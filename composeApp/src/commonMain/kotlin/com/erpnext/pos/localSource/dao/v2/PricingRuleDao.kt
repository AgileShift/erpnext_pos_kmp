package com.erpnext.pos.localSource.dao.v2

import androidx.room.Dao
import androidx.room.Query
import com.erpnext.pos.localSource.entities.v2.PricingRuleEntity

@Dao
interface PricingRuleDao {

    @Query(
        """
        SELECT * FROM pricing_rules
        WHERE instanceId = :instanceId
          AND companyId = :companyId
          AND is_deleted = 0
        ORDER BY priority ASC
    """
    )
    suspend fun getPricingRules(
        instanceId: String,
        companyId: String
    ): List<PricingRuleEntity>
}
