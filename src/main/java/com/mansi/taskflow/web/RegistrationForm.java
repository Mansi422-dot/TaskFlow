package com.mansi.taskflow.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegistrationForm {
    @NotBlank @Size(min=3, max=32) private String username;
    @NotBlank @Email @Size(max=254) private String email;
    @NotBlank @Size(min=10, max=72) private String password;
    public String getUsername(){return username;} public void setUsername(String username){this.username=username;}
    public String getEmail(){return email;} public void setEmail(String email){this.email=email;}
    public String getPassword(){return password;} public void setPassword(String password){this.password=password;}
}
