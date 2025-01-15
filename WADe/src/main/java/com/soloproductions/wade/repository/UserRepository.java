package com.soloproductions.wade.repository;

import com.soloproductions.wade.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long>
{
    User findByUsername(String username);
    User findById(long id);
}
