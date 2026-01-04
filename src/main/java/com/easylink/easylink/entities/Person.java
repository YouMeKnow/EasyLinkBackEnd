package com.easylink.easylink.entities;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name="person")
public class Person {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String firstName;
    private String lastName;

    @OneToMany(mappedBy = "person",cascade = CascadeType.ALL)
    private List<PhoneNumber> phoneNumber;

    private String email;
    private String password;

    public String getPassword() { return password;}

    public void setPassword(String password) { this.password = password; }

    @OneToMany(mappedBy = "person", cascade = CascadeType.ALL)
    private List<BusinessCard> businessCards;

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public List<PhoneNumber> getPhoneNumber() {
        return phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setPhoneNumber(List<PhoneNumber> phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
