package com.bim.seif.models.mappers;

import com.bim.seif.models.*;
import com.bim.seif.models.CuentaAbono;
import com.bim.seif.models.dto.SolicitudTipoCambioDto;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.regex.Pattern;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SolicitudTipoCambioMapper {

    static final Pattern NON_NUMERIC = Pattern.compile("[^\\d.-]");

    static final DecimalFormat MONEY_FMT = new DecimalFormat("#,##0.00");

    @Named("toDto")
    @Mappings({
            @Mapping(target = "id", source = "entity", qualifiedByName = "mapId"),
            @Mapping(target = "cuentaCargo", source = "operacion.campos", qualifiedByName = "mapCuentaCargo"),
            @Mapping(target = "cuentaAbono", source = "operacion.campos", qualifiedByName = "mapCuentaAbono"),
            @Mapping(target = "monto", source = "operacion.campos", qualifiedByName = "mapMonto"),
            @Mapping(target = "referencia", source = "operacion.campos", qualifiedByName = "mapReferencia"),
            @Mapping(target = "comentarios", source = "operacion.comentario"),
            @Mapping(target = "concepto", source = "operacion.concepto"),
            @Mapping(target = "divisa", source = "entity", qualifiedByName = "mapDivisa"),
            @Mapping(target = "banco", source = "operacion.campos", qualifiedByName = "mapBanco"),
            @Mapping(target = "divisaCompra", source = "entity", qualifiedByName = "mapDivisaCompra"),
            @Mapping(target = "divisaPago", source = "entity", qualifiedByName = "mapPagoCompra"),
            @Mapping(target = "contacto", source = "entity.contacto"),
            @Mapping(target = "claveLlamada", source = "entity.claveLlamada"),
            @Mapping(source = "entity", target = "folio", qualifiedByName = "mapFolioFromOperacion"),

            @Mapping(target = "divisaPagoDescripcion", source = "divisaPago.descripcion"),
            @Mapping(target = "divisaCompraDescripcion", source = "divisaCompra.descripcion"),
            @Mapping(target = "tipoCambioPagoNacional", source = "divisaPago.tipoCambioMonedaNacional"),
            @Mapping(target = "tipoCambioCompraNacional", source = "divisaCompra.tipoCambioMonedaNacional"),

            // se calcula en afterMapping
            @Mapping(target = "tipoCambio", ignore = true)
    })
    SolicitudTipoCambioDto toDto(SolicitudTipoCambio entity);

    @IterableMapping(qualifiedByName = "toDto")
    List<SolicitudTipoCambioDto> toDto(List<SolicitudTipoCambio> list);

    // ===== Métodos auxiliares =====

    @Named("mapId")
    default Long mapId(SolicitudTipoCambio entity) {
        return entity.getId();
    }

    @Named("mapFolioFromOperacion")
    default String mapFolioFromOperacion(SolicitudTipoCambio entity) {
        return entity.getOperacion() != null &&
                entity.getOperacion().getInstruccion() != null
                        ? entity.getOperacion().getInstruccion().getFolio()
                        : null;
    }

    @Named("mapCuentaCargo")
    default String mapCuentaCargo(List<Campo> campos) {
        return campos.stream()
                .filter(CampoCuentaCargo.class::isInstance)
                .map(CampoCuentaCargo.class::cast)
                .findFirst()
                .map(c -> c.getCuenta() + (c.getBanco() != null ? " - " + c.getBanco() : ""))
                .orElse(null);
    }

    @Named("mapCuentaAbono")
    default String mapCuentaAbono(List<Campo> campos) {
        return campos.stream()
                .filter(CampoCuentaAbono.class::isInstance)
                .map(CampoCuentaAbono.class::cast)
                .map(campo -> {
                    CuentaAbono cuentaAbono = campo.getCuenta();
                    if (cuentaAbono == null)
                        return null;

                    String cuenta = cuentaAbono.getCuenta() != null ? String.valueOf(cuentaAbono.getCuenta()) : "";
                    String banco = cuentaAbono.getBanco() != null ? " - " + cuentaAbono.getBanco() : "";

                    return cuenta + banco;
                })
                .findFirst()
                .orElse(null);
    }

    @Named("mapMonto")
    default String mapMonto(List<Campo> campos) {
        return campos.stream()
                .filter(CampoMonto.class::isInstance)
                .map(CampoMonto.class::cast)
                .findFirst()
                .map(campo -> {
                    Object raw = campo.getMonto(); // BigDecimal/Long/String según tu modelo
                    String rawStr = (raw == null) ? null : raw.toString();
                    BigDecimal normalized = normalizeMoney(rawStr);
                    return toMoneyString(normalized);
                })
                .orElse(null);
    }

    @Named("mapReferencia")
    default String mapReferencia(List<Campo> campos) {
        return campos.stream()
                .filter(CampoReferencia.class::isInstance)
                .map(CampoReferencia.class::cast)
                .findFirst()
                .map(campo -> campo.getReferencia() != null ? campo.getReferencia() : "")
                .orElse(null);
    }

    @Named("mapDivisa")
    default String mapDivisa(SolicitudTipoCambio entity) {
        return (entity.getDivisaCompra() != null && entity.getDivisaCompra().getCve() != null)
                ? entity.getDivisaCompra().getCve()
                : null;
    }

    @Named("mapBanco")
    default String mapBanco(List<Campo> campos) {
        return campos.stream()
                .filter(CampoCuentaAbono.class::isInstance)
                .map(CampoCuentaAbono.class::cast)
                .map(campo -> {
                    CuentaAbono cuenta = campo.getCuenta();
                    return (cuenta != null && cuenta.getBanco() != null)
                            ? cuenta.getBanco()
                            : null;
                })
                .findFirst()
                .orElse(null);
    }

    @Named("mapDivisaCompra")
    default String mapDivisaCompra(SolicitudTipoCambio entity) {
        return entity != null && entity.getDivisaCompra() != null
                ? entity.getDivisaCompra().getCve()
                : null;
    }

    @Named("mapPagoCompra")
    default String mapPagoCompra(SolicitudTipoCambio entity) {
        return entity != null && entity.getDivisaPago() != null
                ? entity.getDivisaPago().getCve()
                : null;
    }

    // ===== AfterMapping para calcular tipoCambio =====
    @AfterMapping
    default void afterMapping(@MappingTarget SolicitudTipoCambioDto dto, SolicitudTipoCambio entity) {
        String montoStr = mapMonto(entity.getOperacion().getCampos());
        try {
            BigDecimal monto = normalizeMoney(montoStr);
            if (monto != null && entity.getDivisaPago() != null && entity.getDivisaCompra() != null) {
                double tcPagoD = entity.getDivisaPago().getTipoCambioMonedaNacional();
                double tcCompraD = entity.getDivisaCompra().getTipoCambioMonedaNacional();

                if (tcPagoD > 0 && tcCompraD > 0) {
                    BigDecimal tcPago = BigDecimal.valueOf(tcPagoD);
                    BigDecimal tcCompra = BigDecimal.valueOf(tcCompraD);

                    BigDecimal factor = tcPago.divide(tcCompra, 10, RoundingMode.HALF_UP);
                    BigDecimal montoConvertido = monto.multiply(factor);

                    // <<< Aquí aplicamos el mismo formato que "Montos"
                    dto.setTipoCambio(
                            toMoneyDisplay(montoConvertido) + " " + entity.getDivisaCompra().getCve());
                    return;
                }
            }
            dto.setTipoCambio(
                    toMoneyDisplay(BigDecimal.ZERO)
                            + (entity.getDivisaCompra() != null ? " " + entity.getDivisaCompra().getCve() : ""));
        } catch (Exception e) {
            dto.setTipoCambio(
                    toMoneyDisplay(BigDecimal.ZERO)
                            + (entity.getDivisaCompra() != null ? " " + entity.getDivisaCompra().getCve() : ""));
        }
    }

    static String toMoneyDisplay(BigDecimal bd) {
        if (bd == null)
            return "$0.00";
        return "$" + MONEY_FMT.format(bd.setScale(2, RoundingMode.HALF_UP));
    }

    /**
     * Limpia y normaliza montos:
     * - Quita $, comas, espacios, etc.
     * - Si no hay punto decimal, asume centavos y mueve 2 lugares a la izquierda.
     */
    static BigDecimal normalizeMoney(String raw) {
        if (raw == null)
            return null;
        String cleaned = NON_NUMERIC.matcher(raw).replaceAll("");
        if (cleaned.isEmpty() || cleaned.equals("-") || cleaned.equals("."))
            return null;

        if (cleaned.contains(".")) {
            return new BigDecimal(cleaned);
        }
        // Sin punto decimal: asume centavos → mueve 2 lugares a la izquierda
        return new BigDecimal(cleaned).movePointLeft(2);
    }

    /** Formatea a string con 2 decimales sin notación científica */
    static String toMoneyString(BigDecimal bd) {
        if (bd == null)
            return null;
        return bd.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

}