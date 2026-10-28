package com.vetclinic.auth;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByOauthProviderAndOauthSubject(String oauthProvider, String oauthSubject);

    boolean existsByEmailIgnoreCase(String email);

    @Query("select (count(u) > 0) from User u where u.owner.id = :ownerId")
    boolean existsByOwnerId(@Param("ownerId") Long ownerId);

    long countByRoleAndEnabledTrue(Role role);

    long countByRoleAndIdNotAndEnabledTrue(Role role, Long id);

    @Query("""
            select u from User u
            where (:role is null or u.role = :role)
              and (:enabled is null or u.enabled = :enabled)
              and (:q is null
                   or lower(u.fullName) like lower(concat('%', :q, '%'))
                   or lower(u.email) like lower(concat('%', :q, '%')))
            """)
    Page<User> search(@Param("role") Role role,
                      @Param("enabled") Boolean enabled,
                      @Param("q") String q,
                      Pageable pageable);
}
