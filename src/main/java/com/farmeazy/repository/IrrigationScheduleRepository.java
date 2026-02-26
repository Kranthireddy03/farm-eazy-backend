package com.farmeazy.repository;

import com.farmeazy.entity.IrrigationSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface IrrigationScheduleRepository extends JpaRepository<IrrigationSchedule, Long> {
    List<IrrigationSchedule> findByCropId(Long cropId);
    List<IrrigationSchedule> findByFarmId(Long farmId);
    Page<IrrigationSchedule> findByFarmId(Long farmId, Pageable pageable);
    List<IrrigationSchedule> findByFarmIdAndStatus(Long farmId, String status);
    List<IrrigationSchedule> findByIrrigationDateAfter(LocalDate date);
    List<IrrigationSchedule> findByFarmIdAndIrrigationDateBetween(Long farmId, LocalDate startDate, LocalDate endDate);
}
