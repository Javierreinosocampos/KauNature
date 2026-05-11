package com.example.kaunatureapplication;

import com.google.gson.annotations.SerializedName;

public class AsistenciaModel {
    @SerializedName("id")                   public String id;
    @SerializedName("fecha")                public String fecha;
    @SerializedName("horario_semanal_id")   public String horarioSemanalId;
    @SerializedName("cliente_id")           public String clienteId;
    @SerializedName("cliente_nombre")       public String clienteNombre;
    @SerializedName("created_at")           public String createdAt;
}