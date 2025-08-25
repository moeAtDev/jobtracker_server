package com.mAtiehDev.jobtracker.repository;

import com.mAtiehDev.jobtracker.model.User;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmailAddressAndPassword(String emailAddress, String password);

    Optional<User> findByUserNameAndPassword(String userName, String password);

}
