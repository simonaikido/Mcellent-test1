package com.bim.seif.repositories;

import com.bim.seif.models.Empleado;
//import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

//@Repository
public interface AuthRepository {//extends JpaRepository<User, Long> {

   //User findByName(String name);
   Optional<Empleado> findByNameAndPassword(String name, String password);

}
