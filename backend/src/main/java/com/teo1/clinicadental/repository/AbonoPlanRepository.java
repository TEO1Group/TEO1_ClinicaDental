package com.teo1.clinicadental.repository;

import com.teo1.clinicadental.model.AbonoPlan;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AbonoPlanRepository extends JpaRepository<AbonoPlan, UUID> {

    @Query("select abono.plan.id as idPlan, sum(abono.monto) as total from AbonoPlan abono "
            + "where abono.plan.id in :planes group by abono.plan.id")
    List<TotalAbonado> sumarPorPlan(@Param("planes") Collection<UUID> planes);

    interface TotalAbonado {

        UUID getIdPlan();

        BigDecimal getTotal();
    }
}
