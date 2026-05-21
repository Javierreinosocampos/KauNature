package com.example.kaunatureapplication;

import com.google.gson.annotations.SerializedName;

public class CitaModel {
    @SerializedName("id")           public String id;
    @SerializedName("cliente_id")   public String clienteId;
    @SerializedName("servicio_id")  public String servicioId;
    @SerializedName("created_at")   public String createdAt;
    @SerializedName("fecha")        public String fecha;    // "yyyy-MM-dd"
    @SerializedName("hora")         public String hora;     // "HH:mm:ss"
    @SerializedName("estado")       public String estado;   // pendiente|confirmada|cancelada|cobrada
    @SerializedName("notas")        public String notas;

    // ── La vista v_citas usa "cliente" y "servicio", NO "cliente_nombre"/"servicio_nombre"
    @SerializedName("cliente")      public String clienteNombre;
    @SerializedName("servicio")     public String servicioNombre;

    // precio viene como String en la vista ("5.00") — lo parseamos
    @SerializedName("precio")       public String precioRaw;

    public double getPrecio() {
        if (precioRaw == null) return 0;
        try { return Double.parseDouble(precioRaw); }
        catch (Exception e) { return 0; }
    }

    public String inicial() {
        return (clienteNombre != null && !clienteNombre.isEmpty())
                ? String.valueOf(clienteNombre.trim().charAt(0)).toUpperCase() : "?";
    }

    public String fechaDisplay() {
        if (fecha == null || fecha.length() < 10) return "";
        return fecha.substring(8, 10) + "/" + fecha.substring(5, 7) + "/" + fecha.substring(0, 4);
    }

    public String horaDisplay() {
        if (hora == null) return "";
        return hora.length() >= 5 ? hora.substring(0, 5) : hora;
    }

    public String precioDisplay() {
        return String.format(java.util.Locale.US, "%.2f", getPrecio()).replace(".", ",") + "€";
    }
}