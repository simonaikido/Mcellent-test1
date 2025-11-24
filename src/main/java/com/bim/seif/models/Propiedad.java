package com.bim.seif.models;

public enum Propiedad {

    fideicomiso_folio(TipoPropiedad.fideicomiso,"folio del fideicomiso")
    ,fideicomiso_alias(TipoPropiedad.fideicomiso,"nombre del fideicomiso")
    ,cuenta_beneficiario(TipoPropiedad.cuenta, "beneficiario")
    ,cuenta_direccion(TipoPropiedad.cuenta, "direccion")
    ,cuenta_numero(TipoPropiedad.cuenta,"numero de cuenta")
    ,cuenta_banco(TipoPropiedad.cuenta,"banco")
    ,cuenta_rfc(TipoPropiedad.cuenta,"rfc")
    ,cuenta_divisa(TipoPropiedad.cuenta,"divisa")
    ,divisa_tipo_cambio(TipoPropiedad.divisa,"tipo de cambio")
    ,divisa_moneda(TipoPropiedad.divisa,"moneda")
    ,divisa_llamada(TipoPropiedad.divisa,"llamada")
    ,divisa_banco(TipoPropiedad.divisa,"banco")
    ,empleado_email(TipoPropiedad.empleado,"email del empleado")
    ,empleado_rol(TipoPropiedad.empleado,"rol del empleado")
    ,empleado_regiones(TipoPropiedad.empleado,"regiones del empleado")
    ,empleado_nombre(TipoPropiedad.empleado,"nombre del empleado")
    ,cliente_nombre(TipoPropiedad.cliente,"nombre del cliente")
    ,cliente_email(TipoPropiedad.cliente,"email del cliente")
    ,instruccion_folio(TipoPropiedad.instruccion,"folio de la instruccion")
    ,instruccion_tipo(TipoPropiedad.instruccion,"clasificacion de la instruccion")
    ,instruccion_cliente_email(TipoPropiedad.instruccion,"correo del cliente instructor")
    ,instruccion_fecha_recepcion(TipoPropiedad.instruccion,"fecha de recepcion de la instruccion")
    ,otp(TipoPropiedad.seguridad,"One Time Password")
    ,password(TipoPropiedad.pass, "password")
    ,url(TipoPropiedad.link, "Enlace");

    private final TipoPropiedad tipoPropiedad;
    private final String descripcion;

    Propiedad(TipoPropiedad tipoPropiedad, String descripcion){
        this.tipoPropiedad = tipoPropiedad;
        this.descripcion = descripcion;
    }

    public TipoPropiedad getTipoPropiedad() {
        return tipoPropiedad;
    }

    public String getDescripcion() {
        return descripcion;
    }

    @Override
    public String toString() {
        return "{{" + name() + "}}";
    }
}
