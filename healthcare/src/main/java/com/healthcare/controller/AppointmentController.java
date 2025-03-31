package com.healthcare.controller;

import com.healthcare.model.Appointment;
import com.healthcare.model.Doctor;
import com.healthcare.model.User;
import com.healthcare.service.AppointmentService;
import com.healthcare.service.DoctorService;
import com.healthcare.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Controller
@RequestMapping("/appointments")
public class AppointmentController {
    
    @Autowired
    private AppointmentService appointmentService;
    
    @Autowired
    private DoctorService doctorService;
    
    @Autowired
    private UserService userService;
    
    @GetMapping
    public String listAppointments(Model model, Principal principal) {
        User currentUser = userService.findByEmail(principal.getName()).orElseThrow();
        List<Appointment> appointments = appointmentService.findByPatient(currentUser);
        model.addAttribute("appointments", appointments);
        return "appointment/list";
    }
    
    @GetMapping("/book")
    public String showBookingForm(Model model) {
        model.addAttribute("appointment", new Appointment());
        model.addAttribute("doctors", doctorService.findAllDoctors());
        return "appointment/book";
    }
    
    @PostMapping("/book")
    public String bookAppointment(@Valid @ModelAttribute("appointment") Appointment appointment,
                                 BindingResult result,
                                 @RequestParam("doctorId") Long doctorId,
                                 @RequestParam("appointmentDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate appointmentDate,
                                 @RequestParam("appointmentTime") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime appointmentTime,
                                 Principal principal,
                                 Model model) {
        
        if (result.hasErrors()) {
            model.addAttribute("doctors", doctorService.findAllDoctors());
            return "appointment/book";
        }
        
        try {
            // Set the appointment date and time
            LocalDateTime appointmentDateTime = LocalDateTime.of(appointmentDate, appointmentTime);
            appointment.setAppointmentDateTime(appointmentDateTime);
            
            // Set the doctor
            Doctor doctor = doctorService.findById(doctorId);
            appointment.setDoctor(doctor);
            
            // Set the patient (current user)
            User patient = userService.findByEmail(principal.getName()).orElseThrow();
            appointment.setPatient(patient);
            
            // Book the appointment
            appointmentService.bookAppointment(appointment);
            
            return "redirect:/appointments?booked";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("doctors", doctorService.findAllDoctors());
           // return "appointment/book";  doctorService.findAllDoctors());
            return "appointment/book";
        }
    }
    
    @GetMapping("/{id}")
    public String viewAppointment(@PathVariable("id") Long id, Model model, Principal principal) {
        Appointment appointment = appointmentService.findById(id);
        
        // Security check - ensure the appointment belongs to the current user
        User currentUser = userService.findByEmail(principal.getName()).orElseThrow();
        if (!appointment.getPatient().getId().equals(currentUser.getId())) {
            return "redirect:/appointments?error";
        }
        
        model.addAttribute("appointment", appointment);
        return "appointment/view";
    }
    
    @GetMapping("/{id}/cancel")
    public String cancelAppointment(@PathVariable("id") Long id, Principal principal) {
        Appointment appointment = appointmentService.findById(id);
        
        // Security check - ensure the appointment belongs to the current user
        User currentUser = userService.findByEmail(principal.getName()).orElseThrow();
        if (!appointment.getPatient().getId().equals(currentUser.getId())) {
            return "redirect:/appointments?error";
        }
        
        appointmentService.cancelAppointment(id);
        return "redirect:/appointments?cancelled";
    }
}