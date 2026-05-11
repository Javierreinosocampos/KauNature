package com.example.kaunatureapplication;

import com.google.gson.annotations.SerializedName;

public class KpiCobrosMes {
    @SerializedName("total_cobrado")   public double totalCobrado;
    @SerializedName("efectivo")        public double efectivo;
    @SerializedName("tarjeta")         public double tarjeta;
    @SerializedName("bizum_transfer")  public double bizumTransfer;
    @SerializedName("pendientes")      public int    pendientes;
}