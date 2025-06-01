package com.example.PAMS.controller;

import com.example.PAMS.entities.Patient;
import com.example.PAMS.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    @Autowired
    private RegistrationService registrationService;

    // Patient Registration
    @GetMapping("/register/patient")
    public String showPatientRegistration(Model model) {
        model.addAttribute("patient", new Patient());
        return "patient-register";
    }

    @PostMapping("/register/patient")
    public String registerPatient(@Valid @ModelAttribute("patient") Patient patient,
                                  BindingResult result,
                                  Model model) {
        if (result.hasErrors()) {
            System.out.println(result.getAllErrors());
            return "patient-register";
        }
        registrationService.registerPatient(patient);
        System.out.println(patient.getPassword());
        return "redirect:/login?registered";
    }
//    // Login Page
//    @GetMapping("/login")
//    public String login() {
//        return "login";
//    }
}


