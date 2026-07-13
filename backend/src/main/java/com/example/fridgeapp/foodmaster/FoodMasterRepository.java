package com.example.fridgeapp.foodmaster;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodMasterRepository extends JpaRepository<FoodMaster, UUID> {

  List<FoodMaster> findTop20ByActiveTrueAndNameKanaStartingWithOrderByNameKanaAsc(String prefix);
}
