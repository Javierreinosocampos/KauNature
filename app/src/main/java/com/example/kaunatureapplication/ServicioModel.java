package com.example.kaunatureapplication;

import com.google.gson.annotations.SerializedName;

public class ServicioModel {
    @SerializedName("id")            public String id;
    @SerializedName("nombre")        public String nombre;
    @SerializedName("precio")        public double precio;
    @SerializedName("duracion_min")  public int    duracionMin;
    @SerializedName("activo")        public boolean activo;
}