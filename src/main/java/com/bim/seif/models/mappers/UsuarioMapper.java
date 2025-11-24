package com.bim.seif.models.mappers;

import com.bim.seif.models.dto.EmpleadoDto;
import com.bim.seif.models.dto.RegionDto;
import com.bim.seif.models.dto.RolDto;
import lombok.RequiredArgsConstructor;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.query.LdapQuery;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import java.util.List;

import static org.springframework.ldap.query.LdapQueryBuilder.query;

@RequiredArgsConstructor
public class UsuarioMapper implements AttributesMapper<EmpleadoDto> {

        private final LdapTemplate ldapTemplate;

        @Override
        public EmpleadoDto mapFromAttributes(Attributes attrs) throws NamingException {
                EmpleadoDto u = new EmpleadoDto();
                String uid = (String) attrs.get("uid").get();
                u.setUid(uid);
                u.setEmail((String) attrs.get("mail").get());
                u.setNombre((String) attrs.get("cn").get());

                String dnUsuario = "uid=" + uid + ",ou=empleados,dc=bim,dc=com";

                LdapQuery query = query()
                                .base("ou=regiones")
                                .where("member").is(dnUsuario)
                                .and("objectClass").is("groupOfNames");
                System.out.println(query.filter().toString());
                List<RegionDto> regiones = ldapTemplate.search(
                                query,
                                (Attributes gAttrs) -> {
                                        RegionDto r = new RegionDto();
                                        r.setCve((String) gAttrs.get("cn").get());
                                        r.setDescripcion(gAttrs.get("description") != null
                                                        ? (String) gAttrs.get("description").get()
                                                        : "");
                                        return r;
                                });
                u.setRegiones(regiones);

                RolDto rol = ldapTemplate.searchForObject(
                                query()
                                                .base("ou=roles")
                                                .where("member").is(dnUsuario),
                                (Object ctx) -> {
                                        DirContextAdapter context = (DirContextAdapter) ctx;
                                        RolDto r = new RolDto();
                                        r.setCve("ROLE_" + context.getStringAttribute("cn"));
                                        r.setDescripcion(context.getStringAttribute("description"));
                                        return r;
                                });

                u.setRol(rol);

                return u;
        }
}