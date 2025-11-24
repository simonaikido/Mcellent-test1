package com.bim.seif.models;

import lombok.Data;
import org.springframework.ldap.odm.annotations.Attribute;
import org.springframework.ldap.odm.annotations.Entry;
import org.springframework.ldap.odm.annotations.Id;

import javax.naming.Name;

@Data
@Entry(base = "ou=empleados", objectClasses = { "person", "inetOrgPerson", "top" })
public class Empleado {

    @Id
    private Name id;

    @Attribute(name = "uid")
    private String uid;

    @Attribute(name = "mail")
    private String email;

    @Attribute(name = "cn")
    private String nombre;


}
