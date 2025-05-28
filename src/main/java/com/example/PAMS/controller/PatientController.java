package com.example.PAMS.controller;

import com.example.PAMS.model.Patient;
import com.example.PAMS.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class PatientController {
    @Autowired
    private PatientService patientService;

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("patient", new Patient());
        return "register";
    }

    @PostMapping("/register")
    public String registerPatient(@ModelAttribute Patient patient) {
        patientService.register(patient);
        return "redirect:/login";
    }

    @GetMapping("/patient/home")
    public String patientHome() {
        return "patient";
    }
}