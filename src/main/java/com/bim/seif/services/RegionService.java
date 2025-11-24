package com.bim.seif.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.stereotype.Service;

import javax.naming.NamingEnumeration;
import javax.naming.directory.*;
import javax.naming.ldap.LdapName;
import org.springframework.ldap.support.LdapUtils;
import javax.naming.ldap.Rdn;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegionService {

    private final LdapTemplate ldapTemplate;

    private String base() {
        return ((LdapContextSource) ldapTemplate.getContextSource()).getBaseLdapName().toString();
    }

    private LdapName regionDn(String cn) {

        return LdapUtils.newLdapName("cn=" + Rdn.escapeValue(cn) + ",ou=regiones"); // relativo
    }

    private String userAbsDn(String uid) {
        return "uid=" + uid + ",ou=empleados," + base();
    }

    private static String safe(Attributes a, String attr) {

        try {
            Attribute at = a.get(attr);
            return (at == null || at.get() == null) ? null : at.get().toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static List<String> readMembers(Attributes a) {
        List<String> out = new ArrayList<>();
        try {
            Attribute m = a.get("member");
            if (m != null) {
                NamingEnumeration<?> en = m.getAll();
                while (en.hasMore()) out.add(en.next().toString());
            }
        } catch (Exception ignore) { }
        return out;
    }

    private static String dnToUid(String dn) {
        try {
            LdapName ln = new LdapName(dn);
            for (Rdn r : ln.getRdns()) {
                if (r.getType().equalsIgnoreCase("uid")) return r.getValue().toString();
            }
        } catch (Exception ignore) {}
        return dn;
    }

    // ===== CRUD =====

    public List<com.bim.seif.models.dto.RegionDto> listar() {
        return ldapTemplate.search("ou=regiones", "(objectClass=groupOfNames)", (Attributes attrs) -> {
            com.bim.seif.models.dto.RegionDto dto = new com.bim.seif.models.dto.RegionDto();
            dto.setCve(safe(attrs, "cn"));
            dto.setDescripcion(safe(attrs, "description"));
            return dto;
        });
    }

    public com.bim.seif.models.dto.RegionDto obtener(String cn) {
        return ldapTemplate.lookup(regionDn(cn), (Attributes attrs) -> {
            com.bim.seif.models.dto.RegionDto dto = new com.bim.seif.models.dto.RegionDto();
            dto.setCve(safe(attrs, "cn"));
            dto.setDescripcion(safe(attrs, "description"));
            return dto;
        });
    }

    public void crear(String cn, String descripcion, List<String> miembrosUid) {
        if (miembrosUid == null || miembrosUid.isEmpty()) {
            throw new IllegalArgumentException("groupOfNames requiere al menos un 'member'. Envía al menos un UID.");
        }
        DirContextAdapter ctx = new DirContextAdapter(regionDn(cn));
        ctx.setAttributeValues("objectClass", new String[]{"top", "groupOfNames"});
        ctx.setAttributeValue("cn", cn);
        if (descripcion != null) ctx.setAttributeValue("description", descripcion);

        String[] members = miembrosUid.stream().map(this::userAbsDn).toArray(String[]::new);
        ctx.setAttributeValues("member", members);

        ldapTemplate.bind(ctx);
    }

    public void actualizarDescripcion(String cn, String nuevaDescripcion) {
        List<ModificationItem> mods = new ArrayList<>();
        if (nuevaDescripcion == null || nuevaDescripcion.isBlank()) {
            mods.add(new ModificationItem(DirContext.REMOVE_ATTRIBUTE, new BasicAttribute("description")));
        } else {
            mods.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
                    new BasicAttribute("description", nuevaDescripcion.getBytes(StandardCharsets.UTF_8))));
        }
        ldapTemplate.modifyAttributes(regionDn(cn), mods.toArray(new ModificationItem[0]));
    }

    public void renombrar(String cnActual, String cnNuevo) {
        ldapTemplate.rename(regionDn(cnActual), regionDn(cnNuevo));
    }

    public void eliminar(String cn) {
        ldapTemplate.unbind(regionDn(cn));
    }

    // ===== miembros =====

    public List<String> listarMiembros(String cn) {
        return ldapTemplate.lookup(regionDn(cn), (Attributes a) ->
                readMembers(a).stream().map(RegionService::dnToUid).collect(Collectors.toList()));
    }

    public void agregarMiembros(String cn, List<String> uids) {
        if (uids == null || uids.isEmpty()) return;
        for (String uid : uids) {
            ModificationItem add = new ModificationItem(
                    DirContext.ADD_ATTRIBUTE,
                    new BasicAttribute("member", userAbsDn(uid))
            );
            try {
                ldapTemplate.modifyAttributes(regionDn(cn), new ModificationItem[]{add});
            } catch (Exception ignore) {
                // ya existe -> ignorar
            }
        }
    }

    public void removerMiembro(String cn, String uid) {
        ModificationItem rem = new ModificationItem(
                DirContext.REMOVE_ATTRIBUTE,
                new BasicAttribute("member", userAbsDn(uid))
        );
        ldapTemplate.modifyAttributes(regionDn(cn), new ModificationItem[]{rem});
    }

    public void reemplazarMiembros(String cn, List<String> uids) {
        if (uids == null || uids.isEmpty()) {
            throw new IllegalArgumentException("No puedes dejar el grupo sin miembros.");
        }
        String[] members = uids.stream().map(this::userAbsDn).toArray(String[]::new);
        ModificationItem rep = new ModificationItem(
                DirContext.REPLACE_ATTRIBUTE,
                new BasicAttribute("member", members)
        );
        ldapTemplate.modifyAttributes(regionDn(cn), new ModificationItem[]{rep});
    }
}