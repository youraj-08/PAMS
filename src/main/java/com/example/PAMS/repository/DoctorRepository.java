package com.example.PAMS.repository;

import com.example.PAMS.entities.Doctor;
import com.example.PAMS.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Integer> {
    Optional<User> findByDoctorId(Integer id);
}