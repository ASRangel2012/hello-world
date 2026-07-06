package com.example.helloworld.repository;

import com.example.helloworld.domain.Greeting;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GreetingRepository extends JpaRepository<Greeting, Long> {

    Optional<Greeting> findByLocale(String locale);
}
