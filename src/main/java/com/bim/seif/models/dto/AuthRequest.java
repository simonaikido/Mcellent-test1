package com.bim.seif.models.dto;

import com.bim.seif.models.Rol;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthRequest {
     private String usuario;
     private String password;
     private String otp;
     private Rol rol;
     private Set<String> regiones;

     public Set<String> getRegiones() {
          if(this.regiones == null){
               this.regiones = new HashSet<>();
          }
          return regiones;
     }
}