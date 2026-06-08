package com.example.kaunatureapplication;

import com.google.gson.annotations.SerializedName;

public class FranjaModel {
    @SerializedName("id")           public String id;
    @SerializedName("dia_semana")   public int    diaSemana;
    @SerializedName("hora_inicio")  public String horaInicio; // "HH:mm:ss"
    @SerializedName("hora_fin")     public String horaFin;
    @SerializedName("aforo_max")    public int    aforoMax;
    @SerializedName("activo")       public boolean activo;

    public String horaDisplay() {
        if (horaInicio == null) return "";
        return horaInicio.length() >= 5 ? horaInicio.substring(0, 5) : horaInicio;
    }
}