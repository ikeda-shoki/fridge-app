package com.example.fridgeapp.fridge;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsumptionEventRepository extends JpaRepository<ConsumptionEvent, UUID> {}
