package com.example.kaunatureapplication;


import com.google.gson.annotations.SerializedName;

public class ClienteModel {
    @SerializedName("id")           public String id;
    @SerializedName("nombre")       public String nombre;
    @SerializedName("apellidos")    public String apellidos;
    @SerializedName("telefono")     public String telefono;
    @SerializedName("email")        public String email;
    @SerializedName("notas")        public String notas;
    @SerializedName("estado")       public String estado;   // "activo" | "inactivo"
    @SerializedName("saldo")        public double saldo;
    @SerializedName("citas_total")  public int    citasTotal;
    @SerializedName("created_at")   public String createdAt;

    // Helper para la UI
    public String inicial() {
        return (nombre != null && !nombre.isEmpty())
                ? String.valueOf(nombre.charAt(0)).toUpperCase() : "?";
    }
    public boolean tieneDeuda() { return saldo < 0; }
}