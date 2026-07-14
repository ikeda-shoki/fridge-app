package com.example.fridgeapp.group;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** グループの永続化。 */
public interface GroupRepository extends JpaRepository<Group, UUID> {}
