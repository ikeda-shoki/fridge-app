package com.example.fridgeapp.fridge;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** 消費履歴の永続化。履歴は不変のため、記録（save）のみを想定する。 */
public interface ConsumptionEventRepository extends JpaRepository<ConsumptionEvent, UUID> {}
