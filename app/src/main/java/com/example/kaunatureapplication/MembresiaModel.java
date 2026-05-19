package com.example.kaunatureapplication;

import com.google.gson.annotations.SerializedName;

public class MembresiaModel {
    @SerializedName("id")            public String  id;
    @SerializedName("cliente_id")    public String  clienteId;
    @SerializedName("tipo")          public String  tipo;       // "Mensual", "Trimestral"…
    @SerializedName("precio")        public double  precio;
    @SerializedName("fecha_inicio")  public String  fechaInicio; // yyyy-MM-dd
    @SerializedName("fecha_fin")     public String  fechaFin;    // yyyy-MM-dd (puede ser null)
    @SerializedName("activa")        public boolean activa;
    @SerializedName("notas")         public String  notas;
    @SerializedName("created_at")    public String  createdAt;
}