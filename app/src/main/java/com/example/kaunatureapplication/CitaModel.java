package com.example.kaunatureapplication;

import com.google.gson.annotations.SerializedName;

public class CitaModel {
    @SerializedName("id")               public String id;
    @SerializedName("cliente_id")       public String clienteId;
    @SerializedName("cliente_nombre")   public String clienteNombre;
    @SerializedName("servicio_id")      public String servicioId;
    @SerializedName("servicio_nombre")  public String servicioNombre;
    @SerializedName("fecha")            public String fecha;   // "yyyy-MM-dd"
    @SerializedName("hora")             public String hora;    // "HH:mm:ss"
    @SerializedName("precio")           public double precio;
    @SerializedName("notas")            public String notas;
    @SerializedName("estado")           public String estado;  // pendiente|confirmada|cancelada|cobrada
    @SerializedName("created_at")       public String createdAt;

    public String inicial() {
        return (clienteNombre != null && !clienteNombre.isEmpty())
                ? String.valueOf(clienteNombre.charAt(0)).toUpperCase() : "?";
    }
    // La UI usa "dd/MM/yyyy" → convierte al leer
    public String fechaDisplay() {
        if (fecha == null || fecha.length() < 10) return "";
        return fecha.substring(8,10) + "/" + fecha.substring(5,7) + "/" + fecha.substring(0,4);
    }
    // La UI usa "HH:mm" → quita los segundos
    public String horaDisplay() {
        if (hora == null) return "";
        return hora.length() >= 5 ? hora.substring(0,5) : hora;
    }
    public String precioDisplay() {
        return String.format("%.0f€", precio);
    }
}