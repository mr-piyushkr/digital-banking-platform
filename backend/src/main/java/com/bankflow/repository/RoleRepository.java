package com.bankflow.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bankflow.entity.Role;
import com.bankflow.entity.enums.RoleName;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);
}
