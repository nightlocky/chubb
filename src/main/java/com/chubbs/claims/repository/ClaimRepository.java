package com.chubbs.claims.repository;

import com.chubbs.claims.model.Claim;
import com.chubbs.claims.model.ClaimStatus;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClaimRepository extends JpaRepository<Claim, Long> {

    List<Claim> findByStatusAndAssignedOfficerIdIsNull(ClaimStatus status);

    @Query("select sum(c.liabilityAmount) from Claim c where c.status in :statuses")
    BigDecimal sumLiabilityAmountByStatusIn(@Param("statuses") Collection<ClaimStatus> statuses);

    @Query("select sum(c.liabilityAmount) from Claim c where c.status = :status")
    BigDecimal sumLiabilityAmountByStatus(@Param("status") ClaimStatus status);

    long countByStatus(ClaimStatus status);

    long countByStatusIn(Collection<ClaimStatus> statuses);
}
