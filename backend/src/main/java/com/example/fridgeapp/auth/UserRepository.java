package com.example.fridgeapp.auth;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** ユーザーの永続化。 */
public interface UserRepository extends JpaRepository<User, UUID> {

  /** Google アカウント ID（sub）でユーザーを検索する。退会済みユーザーも返るため、呼び出し側で判定すること。 */
  Optional<User> findByGoogleSub(String googleSub);
}
