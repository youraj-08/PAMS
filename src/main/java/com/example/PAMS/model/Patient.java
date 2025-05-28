package com.example.PAMS.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long patientId;
    private String name;
    private String email;
    private String phone;
    private String address;
    private LocalDate dob;
    private String password;
    @Enumerated(EnumType.STRING)
    private Role role = Role.PATIENT;
}
