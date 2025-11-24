package com.bim.seif.repositories;


// private final LdapTemplate ldapTemplate;

import com.bim.seif.models.Empleado;
import org.springframework.data.ldap.repository.LdapRepository;

public interface EmpleadoRepository extends LdapRepository<Empleado> {//extends JpaRepository<Empleado, String> {
Empleado findByNombre(String nombre);


}
