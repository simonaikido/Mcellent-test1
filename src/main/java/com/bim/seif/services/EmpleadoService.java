package com.bim.seif.services;

import static org.springframework.ldap.query.LdapQueryBuilder.query;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.springframework.data.domain.Page;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.ldap.support.LdapEncoder;
import org.springframework.ldap.support.LdapUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.bim.seif.exceptions.DuplicateUidException;
import com.bim.seif.models.dto.EmpleadoDto;
import com.bim.seif.models.dto.EmpleadoReporteDto;
import com.bim.seif.models.dto.FiltroUsuariosInternosDto;
import com.bim.seif.models.dto.RolDto;
import com.bim.seif.models.mappers.UsuarioMapper;
import com.bim.seif.repositories.EmpleadoAuditRepository;
import com.bim.seif.utils.Validators;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmpleadoService {

    private final LdapTemplate ldapTemplate;
    private final EmpleadoAuditRepository empleadoAuditRepository;

    public EmpleadoDto buscarEmpleado(String user) {
        log.info("Buscando empleado con usuario: {}", user);
        LdapQuery query = query()
                .base("ou=empleados")
                .where("objectClass").is("inetOrgPerson")
                .and("uid").is(user);
        List<EmpleadoDto> usuarios = ldapTemplate.search(query, new UsuarioMapper(ldapTemplate));

        return usuarios.isEmpty() ? new EmpleadoDto() : usuarios.get(0);

    }

    public EmpleadoDto loadUserByUsername(String uid) throws InvalidNameException {
        String userDn = String.format("uid=%s,ou=empleados", uid);
        LdapName dn = new LdapName(userDn);

        // 1) Leer datos base (tu mapper actual)
        EmpleadoDto user = ldapTemplate.lookup(dn, new UsuarioMapper(ldapTemplate));

        // 2) Extra: sn y password (tal como ya lo tenías)
        String[] extra = ldapTemplate.lookup(dn, (Attributes attrs) -> new String[] {
                safeAttr(attrs, "sn"),
                readPassword(attrs)
        });
        user.setSn(extra[0]);
        user.setPassword(extra[1]);

        // 2.1) NUEVO: leer los campos adicionales que quieres mostrar
        String[] extrasMore = ldapTemplate.lookup(dn, (Attributes attrs) -> new String[] {
                safeAttr(attrs, "employeeNumber"), // numeroEmpleado
                safeAttr(attrs, "mobile"), // celular
                safeAttr(attrs, "telephoneNumber"), // extension
                safeAttr(attrs, "departmentNumber"), // area
                safeAttr(attrs, "employeeType"), // activo (ACTIVE/INACTIVE)
                safeAttr(attrs, "createTimestamp") // fechaCreacion (operacional)
        });

        user.setNumeroEmpleado(extrasMore[0]);
        user.setCelular(extrasMore[1]);
        user.setExtension(extrasMore[2]);
        user.setArea(extrasMore[3]);

        // Mapear employeeType -> Boolean (o null si no viene)
        String empType = extrasMore[4];
        if (empType == null || empType.isBlank()) {
            user.setActivo(null);
        } else if ("ACTIVE".equalsIgnoreCase(empType)) {
            user.setActivo(Boolean.TRUE);
        } else if ("INACTIVE".equalsIgnoreCase(empType)) {
            user.setActivo(Boolean.FALSE);
        } else {
            user.setActivo(null);
        }

        // Convertir createTimestamp (Generalized Time) a ISO8601 simple
        user.setFechaCreacion(parseLdapGeneralizedTimeToIso(extrasMore[5]));

        // 3) Enriquecer membresías (roles/regiones)
        try {
            enrichMembership(user);
        } catch (Exception ignore) {
        }

        // 4) NORMALIZAR ROLES → siempre con prefijo ROLE_
        normalizeRolesOn(user);
        log.info("Usuario cargado: {}", user.getUid());
        return user;
    }

    /**
     * Convierte LDAP Generalized Time (p.ej. 20241009125423Z o 20241009T125423Z) a
     * ISO8601 UTC.
     */
    private String parseLdapGeneralizedTimeToIso(String v) {
        if (v == null || v.isBlank())
            return null;
        try {
            String s = v.replace("T", "");
            if (s.endsWith("Z"))
                s = s.substring(0, s.length() - 1);
            if (s.length() < 14)
                return null;
            String yyyy = s.substring(0, 4);
            String MM = s.substring(4, 6);
            String dd = s.substring(6, 8);
            String HH = s.substring(8, 10);
            String mm = s.substring(10, 12);
            String ss = s.substring(12, 14);
            return String.format("%s-%s-%sT%s:%s:%sZ", yyyy, MM, dd, HH, mm, ss);
        } catch (Exception ignore) {
            return null;
        }
    }

    private static void normalizeRolesOn(EmpleadoDto user) {
        if (user == null || user.getRol() == null) {
            return;
        }

        RolDto rolDto = user.getRol();
        String cve = rolDto.getCve();

        if (cve != null && !cve.isBlank()) {
            rolDto.setCve(ensureRolePrefix(cve));
        }
    }

    /** Asegura que cualquier rol quede en formato ROLE_<lo que venga> */
    private static String ensureRolePrefix(String role) {
        if (role == null)
            return null;
        String trimmed = role.trim();
        if (trimmed.isEmpty())
            return trimmed;
        // quita el prefijo solo si está al inicio y vuelve a ponérselo
        String base = trimmed.replaceFirst("^ROLE_", "");
        return "ROLE_" + base.toUpperCase(); // opcional: normalizar a mayúsculas
    }

    public List<EmpleadoDto> buscarEmpleados(String rol, String region) {
        // Normalizamos el rol para que siempre tenga ROLE_
        String rolNormalizado = rol.startsWith("ROLE_") ? rol : "ROLE_" + rol;

        // Para el DN, quitamos ROLE_
        String rolDn = String.format("cn=%s,ou=roles", rolNormalizado.replaceFirst("^ROLE_", ""));
        String regionDn = String.format("cn=%s,ou=regiones", region);

        List<EmpleadoDto> usuarios = new ArrayList<>();
        List<String> commonMembers = getUsersInBothGroups(rolDn, regionDn);

        for (String userDn : commonMembers) {
            try {
                EmpleadoDto user = getUserByDn(userDn);
                usuarios.add(user);
            } catch (InvalidNameException e) {
                System.out.println(e);
            }
        }
        log.info("Empleados encontrados: {}", usuarios.size());

        return usuarios;
    }

    private List<String> getMembersOfGroup(String groupDn) {
        return ldapTemplate.search(
                groupDn,
                "(objectClass=groupOfNames)",
                (Attributes attrs) -> {
                    Attribute memberAttr = attrs.get("member");
                    List<String> members = new ArrayList<>();
                    if (memberAttr != null) {
                        NamingEnumeration<?> enumeration = memberAttr.getAll();
                        while (enumeration.hasMore()) {
                            members.add(enumeration.next().toString());
                        }
                    }
                    return members;
                }).stream().flatMap(List::stream).collect(Collectors.toList());
    }

    private List<String> getUsersInBothGroups(String groupDn1, String groupDn2) {
        List<String> group1Members = getMembersOfGroup(groupDn1);
        List<String> group2Members = getMembersOfGroup(groupDn2);
        return group1Members.stream()
                .filter(group2Members::contains)
                .collect(Collectors.toList());
    }

    private EmpleadoDto getUserByDn(String userDn) throws InvalidNameException {
        String baseDn = ((LdapContextSource) ldapTemplate.getContextSource()).getBaseLdapName().toString();
        LdapName fullDn = new LdapName(userDn);
        LdapName base = new LdapName(baseDn);

        List<Rdn> relativeDn = fullDn.getRdns().subList(fullDn.size() - base.size(), fullDn.size());
        LdapName relativeName = new LdapName(relativeDn);
        return ldapTemplate.lookup(relativeName, new ContextMapper<EmpleadoDto>() {
            @Override
            public EmpleadoDto mapFromContext(Object ctx) {
                DirContextAdapter context = (DirContextAdapter) ctx;
                EmpleadoDto user = new EmpleadoDto();
                user.setUid(context.getStringAttribute("uid"));
                user.setEmail(context.getStringAttribute("mail"));
                user.setNombre(context.getStringAttribute("cn"));
                return user;
            }
        });
    }

    // Helpers
    private Name buildEmpleadoDn(String uid) {
        try {
            // DN RELATIVO (la base se añade desde LdapContextSource)
            return new LdapName("uid=" + uid + ",ou=empleados");
        } catch (InvalidNameException e) {
            throw new IllegalArgumentException("DN inválido para uid=" + uid, e);
        }
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String firstNonBlank(String a, String b) {
        return notBlank(a) ? a : b;
    }

    private static String localPart(String uid) {
        int at = uid == null ? -1 : uid.indexOf('@');
        return at > 0 ? uid.substring(0, at) : uid;
    }

    // LISTAR con filtro opcional (reusa tu UsuarioMapper)
    public List<EmpleadoDto> listarEmpleados(String filtro) {
        // Pide explícitamente userPassword y los campos nuevos que necesites
        String[] attrs = new String[] {
                "uid", "mail", "cn", "sn",
                "userPassword", // ← para que readPassword(attrs) tenga el valor
                "employeeNumber", // opcional: número de empleado
                "mobile", // opcional: celular
                "telephoneNumber", // opcional: extensión
                "departmentNumber", // opcional: área
                "employeeType", // opcional: activo/inactivo
                "createTimestamp" // opcional: fecha creación (operacional)
        };

        LdapQuery q;
        if (org.springframework.util.StringUtils.hasText(filtro)) {
        	String safeFiltro = Validators.escapeLdapFilter(filtro);
        	
            q = query()
                    .base("ou=empleados")
                    .attributes(attrs)
                    .filter("(&(objectClass=inetOrgPerson)(|(uid={0})(cn={1})(mail={2})))",
                            "*" + safeFiltro + "*", "*" + safeFiltro + "*", "*" + safeFiltro + "*");
        } else {
            q = query()
                    .base("ou=empleados")
                    .attributes(attrs)
                    .where("objectClass").is("inetOrgPerson");
        }

        List<EmpleadoDto> list = ldapTemplate.search(q, (Attributes attrsMap) -> {
            EmpleadoDto u = new EmpleadoDto();
            u.setUid(safeAttr(attrsMap, "uid"));
            u.setEmail(safeAttr(attrsMap, "mail"));
            u.setNombre(safeAttr(attrsMap, "cn"));
            u.setSn(safeAttr(attrsMap, "sn"));

            // ← conserva tu lectura del hash/valor de password
            u.setPassword(readPassword(attrsMap));

            // Si ya agregaste estos campos al DTO, los llenas:
            u.setNumeroEmpleado(safeAttr(attrsMap, "employeeNumber"));
            u.setCelular(safeAttr(attrsMap, "mobile"));
            u.setExtension(safeAttr(attrsMap, "telephoneNumber"));
            u.setArea(safeAttr(attrsMap, "departmentNumber"));
            u.setActivo(parseActivo(safeAttr(attrsMap, "employeeType")));
            u.setFechaCreacion(parseLdapGeneralizedTime(safeAttr(attrsMap, "createTimestamp")));

            return u;
        });

        // Enriquecimiento con regiones/rol como ya lo tenías
        for (EmpleadoDto u : list) {
            enrichMembership(u);
        }
        return list;
    }

    // ====== Helpers ======
    private static String safeAttr(Attributes attrs, String key) {
        try {
            Attribute a = attrs.get(key);
            return (a == null || a.get() == null) ? null : a.get().toString();
        } catch (Exception ignore) {
            return null;
        }
    }

    private Boolean parseActivo(String employeeType) {
        if (employeeType == null)
            return null; // no seteado
        String v = employeeType.trim().toUpperCase();
        if ("ACTIVE".equals(v))
            return Boolean.TRUE;
        if ("INACTIVE".equals(v))
            return Boolean.FALSE;

        return null; // valor inesperado
    }

    /**
     * Convierte Generalized Time LDAP (ej. 20250109T175430Z) a ISO-8601 (ej.
     * 2025-01-09T17:54:30Z).
     * Si no puede parsear, devuelve el original.
     */
    private String parseLdapGeneralizedTime(String gt) {
        if (gt == null || gt.isEmpty())
            return null;
        // Formatos típicos: yyyyMMddHHmmss'Z' o yyyyMMddHHmmss.SSS'Z' o con 'T'
        String s = gt.trim();
        try {
            // Normaliza: quita 'T' si viene y puntos de fracción
            s = s.replace("T", "");
            int z = s.indexOf('Z');
            String core = z >= 0 ? s.substring(0, z) : s;
            // quitar fracciones si existen (p.ej. 20250109 175430.123)
            int dot = core.indexOf('.');
            if (dot > 0)
                core = core.substring(0, dot);

            // core esperado: yyyyMMddHHmmss
            if (core.length() < 14)
                return gt;

            String iso = core.substring(0, 4) + "-" + core.substring(4, 6) + "-" + core.substring(6, 8)
                    + "T" + core.substring(8, 10) + ":" + core.substring(10, 12) + ":" + core.substring(12, 14)
                    + "Z";
            return iso;
        } catch (Exception e) {
            ;
            return gt;
        }
    }

    private void enrichMembership(EmpleadoDto u) {
        if (u == null || u.getUid() == null)
            return;

        // Base configurada en tu LdapContextSource (p.ej. "dc=bim,dc=com")
        String base = ((LdapContextSource) ldapTemplate.getContextSource())
                .getBaseLdapName().toString();

        // DN ABSOLUTO del usuario (así está en member= del groupOfNames)
        String userAbsDn = "uid=" + u.getUid() + ",ou=empleados," + base;
        String memberEsc = LdapEncoder.filterEncode(userAbsDn);

        // Regiones (lista)
        String regionesFilter = "(&(objectClass=groupOfNames)(member=" + memberEsc + "))";

        // Traemos cn y description de cada grupo en ou=regiones
        List<String[]> regionesData = ldapTemplate.search(
                "ou=regiones",
                regionesFilter,
                (Attributes a) -> new String[] {
                        safeAttr(a, "cn"),
                        safeAttr(a, "description")
                });

        if (regionesData != null && !regionesData.isEmpty()) {
            try {
                Class<?> regionDtoClass = Class.forName("com.bim.seif.models.dto.RegionDto");
                List<Object> regiones = new ArrayList<>();
                for (String[] row : regionesData) {
                    String cn = row[0];
                    String desc = row[1];
                    Object r = regionDtoClass.getDeclaredConstructor().newInstance();
                    // mapeo explícito a tus setters
                    trySet(r, "setCve", cn);
                    trySet(r, "setDescripcion", desc);
                    regiones.add(r);
                }
                // u.setRegiones(List<RegionDto>)
                u.getClass().getMethod("setRegiones", List.class).invoke(u, regiones);
            } catch (Exception ignore) {
                /* si no existe RegionDto o setters, lo omitimos */ }
        } else {
            try {
                u.getClass().getMethod("setRegiones", List.class).invoke(u, (Object) null);
            } catch (Exception ignore) {
            }
        }

        // Rol (toma el primero)
        String rolesFilter = "(&(objectClass=groupOfNames)(member=" + memberEsc + "))";

        List<String[]> rolesData = ldapTemplate.search(
                "ou=roles",
                rolesFilter,
                (Attributes a) -> new String[] {
                        safeAttr(a, "cn"),
                        safeAttr(a, "description")
                });

        if (rolesData != null && !rolesData.isEmpty()) {
            String cn = rolesData.get(0)[0];
            String desc = rolesData.get(0)[1];
            try {
                Class<?> rolDtoClass = Class.forName("com.bim.seif.models.dto.RolDto");
                Object rol = rolDtoClass.getDeclaredConstructor().newInstance();
                trySet(rol, "setCve", cn);
                trySet(rol, "setDescripcion", desc);
                u.getClass().getMethod("setRol", rolDtoClass).invoke(u, rol);
            } catch (Exception ignore) {
                /* si no existe RolDto o setters, lo omitimos */ }
        } else {
            try {
                Class<?> rolDtoClass = Class.forName("com.bim.seif.models.dto.RolDto");
                u.getClass().getMethod("setRol", rolDtoClass).invoke(u, new Object[] { null });
            } catch (Exception ignore) {
            }
        }
    }

    private static void trySet(Object obj, String method, String value) {
        try {
            obj.getClass().getMethod(method, String.class).invoke(obj, value);
        } catch (Exception ignore) {
            /* método no existe, lo saltamos */ }
    }

    // EXISTS
    public boolean existeEmpleado(String uid) {
        try {
            ldapTemplate.lookup(buildEmpleadoDn(uid));
            return true;
        } catch (Exception ignore) {
            return false;
        }
    }

    // CREATE
    public void crearEmpleado(EmpleadoDto dto) throws Exception {

        if (dto == null || !notBlank(dto.getUid())) {
            throw new IllegalArgumentException("uid es obligatorio");
        }
        final String uid = dto.getUid();

        String safeUid = Validators.sanitizeUid(uid);

        if (existeEmpleado(safeUid)) {
            // Caso “ya existe” detectado por tu consulta previa
            throw new DuplicateUidException(safeUid);
        }

        Name dn = buildEmpleadoDn(safeUid);
        DirContextAdapter ctx = new DirContextAdapter(dn);
        ctx.setAttributeValues("objectClass",
                new String[] { "top", "person", "organizationalPerson", "inetOrgPerson" });

        String cn = firstNonBlank(dto.getNombre(), localPart(safeUid));
        String sn = firstNonBlank(dto.getSn(), cn);

        ctx.setAttributeValue("uid", safeUid);
        ctx.setAttributeValue("cn", cn);
        ctx.setAttributeValue("sn", sn);

        if (notBlank(dto.getEmail()))
            ctx.setAttributeValue("mail", dto.getEmail());

        if (notBlank(dto.getPassword()))
            ctx.setAttributeValue("userPassword", dto.getPassword());

        if (notBlank(dto.getNumeroEmpleado()))
            ctx.setAttributeValue("employeeNumber", dto.getNumeroEmpleado());

        if (notBlank(dto.getCelular()))
            ctx.setAttributeValue("mobile", dto.getCelular());

        if (notBlank(dto.getExtension()))
            ctx.setAttributeValue("telephoneNumber", dto.getExtension());

        if (notBlank(dto.getArea()))
            ctx.setAttributeValue("departmentNumber", dto.getArea());

        // === Bandera de primer login ===
        // Activo => FIRST_LOGIN (obliga a cambiar pass en el primer inicio)
        // Inactivo => INACTIVE
        final Boolean activo = dto.getActivo();
        final String employeeType = (activo == null || activo) ? "FIRST_LOGIN" : "INACTIVE";
        ctx.setAttributeValue("employeeType", employeeType);

        try {
            ldapTemplate.bind(ctx);
        } catch (org.springframework.ldap.NameAlreadyBoundException e) {
            // carrera entre “existeEmpleado” y el bind real
            throw new DuplicateUidException(safeUid);
        }
    }

    // UPDATE
    public void actualizarEmpleado(String uid, EmpleadoDto cambios) {
        if (!existeEmpleado(uid)) {
            throw new IllegalArgumentException("No existe empleado con uid=" + uid);
        }
        if (cambios == null) {
            return;
        }
        // Normaliza/valida mail (IA5/ASCII). Si quieres fallar en lugar de normalizar,
        // lanza IllegalArgumentException.
        if (notBlank(cambios.getEmail())) {
            cambios.setEmail(normalizeEmailIA5(cambios.getEmail()));
        }

        List<ModificationItem> mods = new ArrayList<>();

        // --- Nombre (cn) y sn (mantener esquema consistente)
        if (notBlank(cambios.getNombre())) {
            mods.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
                    new BasicAttribute("cn", cambios.getNombre())));
            String sn = notBlank(cambios.getSn()) ? cambios.getSn() : cambios.getNombre();
            mods.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
                    new BasicAttribute("sn", sn)));
        } else if (notBlank(cambios.getSn())) {
            // si sólo vino sn
            mods.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
                    new BasicAttribute("sn", cambios.getSn())));
        }

        // --- Correo
        if (cambios.getEmail() != null) {
            addReplaceOrRemove(mods, "mail", vacioComoNulo(cambios.getEmail()));
        }

        // --- Password (hash en LDAP)
        if (notBlank(cambios.getPassword())) {
            byte[] pwd = encodeUserPassword(cambios.getPassword(), "SHA");
            mods.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
                    new BasicAttribute("userPassword", pwd)));
        }

        // --- Número de empleado -> employeeNumber
        if (cambios.getNumeroEmpleado() != null) {
            addReplaceOrRemove(mods, "employeeNumber", vacioComoNulo(cambios.getNumeroEmpleado()));
        }

        // --- Celular -> mobile
        if (cambios.getCelular() != null) {
            addReplaceOrRemove(mods, "mobile", vacioComoNulo(cambios.getCelular()));
        }

        // --- Extensión -> telephoneNumber
        if (cambios.getExtension() != null) {
            addReplaceOrRemove(mods, "telephoneNumber", vacioComoNulo(cambios.getExtension()));
        }

        // --- Área -> departmentNumber
        if (cambios.getArea() != null) {
            addReplaceOrRemove(mods, "departmentNumber", vacioComoNulo(cambios.getArea()));
        }

        // --- Activo -> employeeType (ACTIVE / INACTIVE)
        if (cambios.getActivo() != null) {
            String employeeType = cambios.getActivo() ? "ACTIVE" : "INACTIVE";
            mods.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
                    new BasicAttribute("employeeType", employeeType)));
        }

        if (!mods.isEmpty()) {
            ldapTemplate.modifyAttributes(buildEmpleadoDn(uid),
                    mods.toArray(new ModificationItem[0]));
        }
    }

    // DELETE
    public void eliminarEmpleado(String uid) {
        if (!existeEmpleado(uid))
            return;
        ldapTemplate.unbind(buildEmpleadoDn(uid));
    }

    private static String readPassword(Attributes attrs) {
        try {
            Attribute a = attrs.get("userPassword");
            if (a == null)
                return null;
            Object v = a.get();
            if (v == null)
                return null;

            if (v instanceof byte[]) {
                byte[] b = (byte[]) v;
                // Muchos LDAP guardan "{SSHA}..." como texto pero el LDIF lo muestra base64.
                // Intentamos leerlo como UTF-8. Si no “parece” texto con esquema, lo regresamos
                // en base64.
                String asUtf8 = new String(b, StandardCharsets.UTF_8);
                if (asUtf8.startsWith("{") && asUtf8.contains("}")) {
                    return asUtf8; // p.ej. {SSHA}abc...
                } else {
                    return "base64:" + Base64.getEncoder().encodeToString(b);
                }
            } else {
                return v.toString(); // ya es String
            }

        } catch (Exception ignore) {
            return null;
        }
    }

    public boolean verificarPasswordBind(String uid, String rawPassword) {
        // Construye el DN ABSOLUTO del usuario
        String base = ((LdapContextSource) ldapTemplate.getContextSource())
                .getBaseLdapName().toString(); // ej: "dc=bim,dc=com"
        String userDnAbs = "uid=" + uid + ",ou=empleados," + base;

        DirContext ctx = null;
        try {
            // Si el DN/credenciales son correctos, esto devuelve un contexto válido
            ctx = ((LdapContextSource) ldapTemplate.getContextSource())
                    .getContext(userDnAbs, rawPassword);
            return true;
        } catch (org.springframework.ldap.AuthenticationException e) {
            // credenciales inválidas
            return false;
        } catch (org.springframework.ldap.NamingException e) {
            // problemas de conexión/DN inválido/otros errores LDAP
            throw e; // o: throw new RuntimeException("Error LDAP", e);
        } finally {
            LdapUtils.closeContext(ctx);
        }
    }

    // Verificación por hash ({CRYPT}$2a$... ==> bcrypt)
    public boolean verificarPasswordPorHash(String uid, String rawPassword) {
        log.info("Verificando password por hash para uid: {}", uid);
        try {
            String userDnRel = "uid=" + uid + ",ou=empleados";
            String stored = ldapTemplate.lookup(new LdapName(userDnRel), (Attributes attrs) -> readPassword(attrs));
            if (stored == null) {
                // Si no tienes permiso de lectura sobre userPassword, stored será null
                throw new IllegalStateException("No se pudo leer userPassword (ACL/permiso).");
            }
            String hash = stored.startsWith("{CRYPT}") ? stored.substring("{CRYPT}".length()) : stored;

            // Para bcrypt (prefijo $2a$, $2b$, etc.)
            if (hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$")) {
                return org.springframework.security.crypto.bcrypt.BCrypt.checkpw(rawPassword, hash);
            }

            // Si tuvieses {SSHA} u otros esquemas, aquí podrías añadir verificación
            // específica.
            // Como fallback, intenta bind.
            return verificarPasswordBind(uid, rawPassword);
        } catch (InvalidNameException e) {
            throw new IllegalArgumentException("UID inválido: " + uid, e);
        }
    }

    private static byte[] encodeUserPassword(String rawOrScheme, String preferredAlgForPlainText) {

        log.info("Codificando password de usuario");
        if (rawOrScheme == null || rawOrScheme.isBlank())
            return null;

        // Si viene con esquema, lo guardamos tal cual
        if (rawOrScheme.startsWith("{") && rawOrScheme.contains("}")) {
            return rawOrScheme.getBytes(StandardCharsets.UTF_8);
        }

        // Texto plano: elegimos algoritmo preferido (default SHA)
        String alg = (preferredAlgForPlainText == null ? "SHA" : preferredAlgForPlainText).toUpperCase();
        switch (alg) {
            case "SSHA":
                return encodeSSHA(rawOrScheme);
            case "BCRYPT":
            case "CRYPT":
            case "CRYPT-BCRYPT":
                return encodeBcrypt(rawOrScheme);
            case "SHA":
            default:
                return encodeSHA(rawOrScheme);
        }
    }

    private static byte[] encodeSHA(String raw) {

        log.info("Codificando password con SHA");
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            String value = "{SHA}" + Base64.getEncoder().encodeToString(digest);
            return value.getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error generando {SHA}", e);
        }
    }

    private static byte[] encodeSSHA(String raw) {
        log.info("Codificando password con SSHA");
        try {
            byte[] salt = new byte[8];
            new SecureRandom().nextBytes(salt);
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(raw.getBytes(StandardCharsets.UTF_8));
            md.update(salt);
            byte[] digest = md.digest();
            byte[] digestPlusSalt = new byte[digest.length + salt.length];
            System.arraycopy(digest, 0, digestPlusSalt, 0, digest.length);
            System.arraycopy(salt, 0, digestPlusSalt, digest.length, salt.length);
            String value = "{SSHA}" + Base64.getEncoder().encodeToString(digestPlusSalt);
            return value.getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error generando {SSHA}", e);
        }
    }

    private static byte[] encodeBcrypt(String raw) {

        log.info("Codificando password con bcrypt");
        String bcrypt = new BCryptPasswordEncoder(10).encode(raw); // $2a$10$...
        String value = "{CRYPT}" + bcrypt;
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static String vacioComoNulo(String s) {
        log.info("Normalizando cadena vacia a nulo");
        if (s == null)
            return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /** Si value es null => REMOVE, si no => REPLACE */
    private static void addReplaceOrRemove(List<ModificationItem> mods, String attrName, String value) {
        log.info("Agregando modificacion para atributo: {}", attrName);
        if (value == null) {
            mods.add(new ModificationItem(DirContext.REMOVE_ATTRIBUTE, new BasicAttribute(attrName)));
        } else {
            mods.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(attrName, value)));
        }
    }

    /**
     * El atributo LDAP 'mail' (rfc822Mailbox) es IA5String (ASCII).
     * Aquí normalizamos a IA5:
     * - trim
     * - lower-case
     * - si contiene caracteres no ASCII en la parte local, los "desacentuamos" y
     * reemplazamos 'ñ' -> 'n'. Para el dominio, se puede usar IDN.toASCII().
     */
    private static String normalizeEmailIA5(String mail) {
        log.info("Normalizando email a IA5String: {}", mail);
        if (mail == null)
            return null;
        String m = mail.trim().toLowerCase();
        int at = m.indexOf('@');
        if (at < 1 || at == m.length() - 1)
            return m; // formato raro, lo devolvemos y dejar que LDAP valide

        String local = m.substring(0, at);
        String domain = m.substring(at + 1);

        // desacentuar local-part (ñ -> n, acentos fuera)
        local = java.text.Normalizer.normalize(local, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "") // quita diacríticos
                .replace('ñ', 'n')
                .replace('Ñ', 'n');

        // elimina cualquier carácter no ASCII en local
        local = local.replaceAll("[^\\p{ASCII}]", "");

        // dominio a ASCII (punycode)
        try {
            domain = java.net.IDN.toASCII(domain);
        } catch (Exception ignore) {
            /* deja como venga si falla */ }

        return local + "@" + domain;
    }

    public void actualizarEstadoActivo(String uid, boolean activo) {
        log.info("Actualizando estado activo para uid: {} a {}", uid, activo);
        final String ATTR = "employeeType"; // atributo correcto
        final String VALUE = activo ? "ACTIVE" : "INACTIVE";

        try {
            // DN **relativo** al base configurado en el ContextSource
            Name dn = LdapUtils.newLdapName(String.format("uid=%s,ou=empleados", uid));

            DirContextOperations ctx = ldapTemplate.lookupContext(dn);
            ctx.setAttributeValue(ATTR, VALUE); // agrega si no existe, reemplaza si existe
            ldapTemplate.modifyAttributes(ctx);

        } catch (Exception e) {
            throw new RuntimeException("Error al actualizar estado activo del usuario " + uid, e);
        }
    }

    // REPORTE: LISTADO USUARIOS INTERNOS (desde AUDIT, no LDAP)
    // imports: usa EmpleadoReporteDto
    public List<EmpleadoReporteDto> listarUsuariosInternos(
            LocalDate desde,
            LocalDate hasta,
            String usernameActor,
            FiltroUsuariosInternosDto filtro,
            int pagina,
            int tamanio,
            String ordenarPor) {

        log.info(
                "Listando usuarios internos desde {} hasta {}, actor={}, filtro={}, página={}, tamano={}, ordenarPor={}",
                desde, hasta, usernameActor, filtro, pagina, tamanio, ordenarPor);

        if (desde == null || hasta == null) {
            throw new IllegalArgumentException("Los parámetros 'desde' y 'hasta' son obligatorios (yyyy-MM-dd).");
        }

        ZoneId zone = ZoneId.of("America/Mexico_City");
        OffsetDateTime start = desde.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime end = hasta.atTime(23, 59, 59, 999_000_000).atZone(zone).toOffsetDateTime();

        int pageNumber = 0;
        int pageSize = 2000;

        List<EmpleadoReporteDto> filas = new ArrayList<>();

        while (true) {
            Page<Object[]> page = empleadoAuditRepository.buscarAuditoriaCruda(
                    start, end, org.springframework.data.domain.PageRequest.of(pageNumber, pageSize));

            if (page == null || page.isEmpty())
                break;

            for (Object[] row : page.getContent()) {
                // SELECT uid, accion, actor, ip, user_agent, payload_before, payload_after,
                // created_at, comentario
                int i = 0;
                String uid = (String) row[i++];
                String accion = (String) row[i++];
                String actor = (String) row[i++];
                String ip = (String) row[i++];
                String userAgent = (String) row[i++];
                String beforeJs = (String) row[i++];
                String afterJs = (String) row[i++];
                Object created = row[i++];
                String comentario = (String) row[i];

                if (uid == null)
                    continue;

                OffsetDateTime createdAt;
                if (created instanceof java.sql.Timestamp ts) {
                    createdAt = ts.toInstant().atOffset(java.time.ZoneOffset.UTC);
                } else if (created instanceof java.time.LocalDateTime ldt) {
                    createdAt = ldt.atOffset(java.time.ZoneOffset.UTC);
                } else if (created instanceof OffsetDateTime odt) {
                    createdAt = odt;
                } else {
                    createdAt = null;
                }

                EmpleadoReporteDto dto = new EmpleadoReporteDto();
                dto.setUid(uid);
                dto.setAccion(accion);
                dto.setActor(actor);
                dto.setIp(ip);
                dto.setUserAgent(userAgent);
                dto.setPayloadBefore(beforeJs);
                dto.setPayloadAfter(afterJs);
                dto.setCreatedAt(createdAt);
                dto.setComentario(comentario);

                filas.add(dto);
            }

            if (!page.hasNext())
                break;
            pageNumber++;
        }

        if (filas.isEmpty())
            return Collections.emptyList();

        // Orden (default uid)
        Comparator<String> CI = Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER);
        Comparator<EmpleadoReporteDto> cmp;
        String ord = (ordenarPor == null || ordenarPor.isBlank()) ? "uid" : ordenarPor;
        switch (ord) {
            case "accion" -> cmp = Comparator.comparing(EmpleadoReporteDto::getAccion, CI);
            case "actor" -> cmp = Comparator.comparing(EmpleadoReporteDto::getActor, CI);
            case "ip" -> cmp = Comparator.comparing(EmpleadoReporteDto::getIp, CI);
            case "userAgent" -> cmp = Comparator.comparing(EmpleadoReporteDto::getUserAgent, CI);
            case "createdAt" -> cmp = Comparator.comparing(EmpleadoReporteDto::getCreatedAt,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            case "comentario" -> cmp = Comparator.comparing(EmpleadoReporteDto::getComentario, CI);
            default -> cmp = Comparator.comparing(EmpleadoReporteDto::getUid, CI);
        }

        List<EmpleadoReporteDto> ordenados = new ArrayList<>(filas);
        ordenados.sort(cmp);

        // Paginación
        if (pagina < 0)
            pagina = 0;
        if (tamanio <= 0)
            tamanio = 15;
        int from = Math.min(pagina * tamanio, ordenados.size());
        int to = Math.min(from + tamanio, ordenados.size());

        return ordenados.subList(from, to);
    }

    public boolean isFirstLogin(String uid) {
        Name dn = buildEmpleadoDn(uid);
        DirContextOperations ctx = ldapTemplate.lookupContext(dn);
        Object v = ctx.getObjectAttribute("employeeType");
        String et = (v == null ? null : v.toString());
        return "FIRST_LOGIN".equalsIgnoreCase(et);
    }

    // (Cuando el usuario cambie la contraseña con éxito, llama a este)
    public void marcarPrimerLoginCompletado(String uid) {
        Name dn = buildEmpleadoDn(uid);
        ModificationItem[] mods = new ModificationItem[] {
                new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
                        new BasicAttribute("employeeType", "ACTIVE"))
        };
        ldapTemplate.modifyAttributes(dn, mods);
    }
}