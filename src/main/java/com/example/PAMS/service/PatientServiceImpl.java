package com.example.PAMS.service;

import com.example.PAMS.model.Patient;
import com.example.PAMS.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PatientServiceImpl implements PatientService {
    @Autowired
    private PatientRepository repo;
    @Autowired private PasswordEncoder encoder;

    @Override
    public Patient register(Patient patient) {
        patient.setPassword(encoder.encode(patient.getPassword()));
        return repo.save(patient);
    }
}
