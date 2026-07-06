package com.example.helloworld.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "greeting")
public class Greeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 8)
    private String locale;

    /** Template with a single %s placeholder for the caller's name. */
    @Column(nullable = false)
    private String template;

    protected Greeting() {
        // JPA
    }

    public Greeting(String locale, String template) {
        this.locale = locale;
        this.template = template;
    }

    public Long getId() {
        return id;
    }

    public String getLocale() {
        return locale;
    }

    public String getTemplate() {
        return template;
    }
}
