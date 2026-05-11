package com.example.kaunatureapplication;

import com.google.gson.annotations.SerializedName;

public class CobroModel {
    @SerializedName("id")               public String id;
    @SerializedName("cliente_id")       public String clienteId;
    @SerializedName("cliente_nombre")   public String clienteNombre;
    @SerializedName("cita_id")          public String citaId;
    @SerializedName("concepto")         public String concepto;
    @SerializedName("importe")          public double importe;
    @SerializedName("metodo")           public String metodo;  // Efectivo|Tarjeta|Bizum|Transferencia
    @SerializedName("estado")           public String estado;  // pendiente|cobrado
    @SerializedName("fecha")            public String fecha;
    @SerializedName("notas")            public String notas;
    @SerializedName("created_at")       public String createdAt;

    public String inicial() {
        return (clienteNombre != null && !clienteNombre.isEmpty())
                ? String.valueOf(clienteNombre.charAt(0)).toUpperCase() : "?";
    }
    public String importeFormateado() {
        return String.format("%.2f€", importe).replace(".", ",");
    }
}