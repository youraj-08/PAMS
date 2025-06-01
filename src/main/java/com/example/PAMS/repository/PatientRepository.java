package com.example.PAMS.repository;

import com.example.PAMS.entities.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface PatientRepository extends JpaRepository<Patient, Integer> { }


