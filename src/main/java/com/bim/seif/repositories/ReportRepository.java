package com.bim.seif.repositories;

import com.bim.seif.models.OperacionJuridica;
import com.bim.seif.models.OperacionMonetaria;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class ReportRepository {

    @PersistenceContext
    private EntityManager em;

    public List<OperacionMonetaria> buscarOperacionesMonetariasPorMuestra(
            OperacionMonetaria filtro,
            LocalDate desde,
            LocalDate hasta,
            Set<String> regiones,
            Pageable pageable
    ) {

        StringBuilder jpql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();

        // SELECT + JOINS (equivalentes a los fetchJoin que tenías)
        jpql.append("select om ")
            .append("from OperacionMonetaria om ")
            .append("join fetch om.instruccion im ")
            .append("join fetch im.instruccion i ")
            .append("join fetch i.fideicomiso f ")
            .append("join fetch f.region r ")
            .append("where 1=1 ");

        // Filtro por regiones (si viene no vacío)
        if (regiones != null && !regiones.isEmpty()) {
            jpql.append("and r.cve in :regiones ");
            params.put("regiones", regiones);
        }

        // Rango de fechas en om.fechaRegistro (si ambos vienen)
        if (desde != null && hasta != null) {
            jpql.append("and om.fechaRegistro between :desde and :hasta ");
            params.put("desde", desde.atStartOfDay());
            params.put("hasta", hasta.atTime(23, 59, 59));
        }

        // Filtro por tipo de operación (si viene en el filtro)
        if (filtro != null
                && filtro.getTipoOperacion() != null
                && filtro.getTipoOperacion().getCve() != null
                && !filtro.getTipoOperacion().getCve().trim().isEmpty()) {
            jpql.append("and om.tipoOperacion.cve = :tipoCve ");
            params.put("tipoCve", filtro.getTipoOperacion().getCve());
        }

        // ORDER BY usando el Sort del Pageable (si existe), sino por id asc
        jpql.append(buildOrderBy("om", pageable));

        TypedQuery<OperacionMonetaria> query = em.createQuery(jpql.toString(), OperacionMonetaria.class);
        params.forEach(query::setParameter);

        // Paginación
        if (pageable != null) {
            query.setFirstResult((int) pageable.getOffset());
            query.setMaxResults(pageable.getPageSize());
        }

        return query.getResultList();
    }

    public List<OperacionJuridica> buscarOperacionesJuridicasPorMuestra(
            OperacionJuridica filtro,
            LocalDate desde,
            LocalDate hasta,
            Set<String> regiones,
            Pageable pageable
    ) {

        StringBuilder jpql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();

        // SELECT + JOINS (equivalentes a los fetchJoin que tenías)
        jpql.append("select oj ")
            .append("from OperacionJuridica oj ")
            .append("join fetch oj.instruccion ij ")
            .append("join fetch ij.instruccion i ")
            .append("join fetch i.fideicomiso f ")
            .append("join fetch f.region r ")
            .append("where 1=1 ");

        // Filtro por regiones (si viene no vacío)
        if (regiones != null && !regiones.isEmpty()) {
            jpql.append("and r.cve in :regiones ");
            params.put("regiones", regiones);
        }

        // Rango de fechas en oj.fechaRegistro (si ambos vienen)
        if (desde != null && hasta != null) {
            jpql.append("and oj.fechaRegistro between :desde and :hasta ");
            params.put("desde", desde.atStartOfDay());
            params.put("hasta", hasta.atTime(23, 59, 59));
        }

        // Filtro por tipo de operación jurídica (si viene en el filtro)
        if (filtro != null
                && filtro.getTipoOperacionJuridica() != null
                && filtro.getTipoOperacionJuridica().getCve() != null
                && !filtro.getTipoOperacionJuridica().getCve().trim().isEmpty()) {
            jpql.append("and oj.tipoOperacionJuridica.cve = :tipoCve ");
            params.put("tipoCve", filtro.getTipoOperacionJuridica().getCve());
        }

        // ORDER BY usando el Sort del Pageable (si existe), sino por id asc
        jpql.append(buildOrderBy("oj", pageable));

        TypedQuery<OperacionJuridica> query = em.createQuery(jpql.toString(), OperacionJuridica.class);
        params.forEach(query::setParameter);

        // Paginación
        if (pageable != null) {
            query.setFirstResult((int) pageable.getOffset());
            query.setMaxResults(pageable.getPageSize());
        }

        return query.getResultList();
    }

    // ---- Helpers ----

    private String buildOrderBy(String alias, Pageable pageable) {
        if (pageable == null || pageable.getSort() == null || pageable.getSort().isUnsorted()) {
            return " order by " + alias + ".id asc";
        }
        Sort sort = pageable.getSort();
        String orderBy = sort.stream()
                .map(o -> alias + "." + sanitizeProperty(o.getProperty()) + " " + (o.isAscending() ? "asc" : "desc"))
                .collect(Collectors.joining(", "));
        return " order by " + (orderBy.isEmpty() ? (alias + ".id asc") : orderBy);
    }

    // Limpia el nombre de propiedad para evitar inyección en ORDER BY
    private String sanitizeProperty(String prop) {
        // Permite letras, números y puntos (para nested props), quita el resto.
        return prop == null ? "id" : prop.replaceAll("[^A-Za-z0-9_.]", "");
    }
}