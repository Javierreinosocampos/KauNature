package com.example.kaunatureapplication;

public class MembresiaModel {
    @com.google.gson.annotations.SerializedName("id")           public String  id;
    @com.google.gson.annotations.SerializedName("cliente_id")   public String  clienteId;
    @com.google.gson.annotations.SerializedName("tipo")         public String  tipo;
    @com.google.gson.annotations.SerializedName("precio")       public double  precio;
    @com.google.gson.annotations.SerializedName("fecha_inicio") public String  fechaInicio;
    @com.google.gson.annotations.SerializedName("fecha_fin")    public String  fechaFin;
    @com.google.gson.annotations.SerializedName("activa")       public boolean activa;
    @com.google.gson.annotations.SerializedName("notas")        public String  notas;
    @com.google.gson.annotations.SerializedName("created_at")   public String  createdAt;

    public boolean canceladaEnPeriodoEspera() {
        if (activa || fechaFin == null || fechaFin.isEmpty()) return false;
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            java.util.Date dFin = sdf.parse(fechaFin);
            if (dFin == null) return false;
            java.util.Calendar hoy = java.util.Calendar.getInstance();
            java.util.Calendar calFin = java.util.Calendar.getInstance();
            calFin.setTime(dFin);
            return hoy.before(calFin) || hoy.equals(calFin);
        } catch (Exception e) { return false; }
    }

    public int diasParaReinscripcion() {
        if (activa || fechaFin == null || fechaFin.isEmpty()) return 0;
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            java.util.Date dFin = sdf.parse(fechaFin);
            if (dFin == null) return 0;
            java.util.Calendar calFin = java.util.Calendar.getInstance();
            calFin.setTime(dFin);
            java.util.Calendar hoy = java.util.Calendar.getInstance();
            long diffMs = calFin.getTimeInMillis() - hoy.getTimeInMillis();
            return Math.max(0, (int) (diffMs / (1000 * 60 * 60 * 24)));
        } catch (Exception e) { return 0; }
    }

    public String calcularFechaFin() {
        if (fechaInicio == null || fechaInicio.isEmpty()) return "";
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            java.util.Date dInicio = sdf.parse(fechaInicio);
            if (dInicio == null) return "";
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(dInicio);
            switch (tipo.toLowerCase()) {
                case "mensual": cal.add(java.util.Calendar.MONTH, 1); break;
                case "trimestral": cal.add(java.util.Calendar.MONTH, 3); break;
                case "semestral": cal.add(java.util.Calendar.MONTH, 6); break;
                case "anual": cal.add(java.util.Calendar.YEAR, 1); break;
                default: cal.add(java.util.Calendar.MONTH, 1);
            }
            return sdf.format(cal.getTime());
        } catch (Exception e) { return ""; }
    }

    public int diaMesInicio() {
        if (fechaInicio == null || fechaInicio.length() < 10) return 0;
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            java.util.Date d = sdf.parse(fechaInicio);
            if (d == null) return 0;
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(d);
            return cal.get(java.util.Calendar.DAY_OF_MONTH);
        } catch (Exception e) { return 0; }
    }

    public String proximoCobro() {
        if (fechaInicio == null || !activa) return "";
        if (!"mensual".equalsIgnoreCase(tipo)) return "";
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            java.util.Date dInicio = sdf.parse(fechaInicio);
            if (dInicio == null) return "";
            java.util.Calendar calInicio = java.util.Calendar.getInstance();
            calInicio.setTime(dInicio);
            int diaInicio = calInicio.get(java.util.Calendar.DAY_OF_MONTH);
            java.util.Calendar hoy = java.util.Calendar.getInstance();
            java.util.Calendar proxCobro = java.util.Calendar.getInstance();
            proxCobro.set(java.util.Calendar.DAY_OF_MONTH, diaInicio);
            if (proxCobro.before(hoy) || proxCobro.equals(hoy)) proxCobro.add(java.util.Calendar.MONTH, 1);
            int maxDiaDelMes = proxCobro.getActualMaximum(java.util.Calendar.DAY_OF_MONTH);
            if (diaInicio > maxDiaDelMes) proxCobro.set(java.util.Calendar.DAY_OF_MONTH, maxDiaDelMes);
            return sdf.format(proxCobro.getTime());
        } catch (Exception e) { return ""; }
    }

    public boolean debeGenerarCobroMesActual() {
        return activa && "mensual".equalsIgnoreCase(tipo);
    }


    public String calcularFechaInicioRenovacion() {
        if (fechaInicio == null || fechaInicio.isEmpty()) return "";
        try {
            java.text.SimpleDateFormat sdf =
                    new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            java.util.Date dInicio = sdf.parse(fechaInicio);
            if (dInicio == null) return fechaInicio;

            java.util.Calendar inicio = java.util.Calendar.getInstance();
            inicio.setTime(dInicio);

            final int diaAncla = inicio.get(java.util.Calendar.DAY_OF_MONTH);

            java.util.Calendar hoy = java.util.Calendar.getInstance();
            hoy.set(java.util.Calendar.HOUR_OF_DAY, 0);
            hoy.set(java.util.Calendar.MINUTE, 0);
            hoy.set(java.util.Calendar.SECOND, 0);
            hoy.set(java.util.Calendar.MILLISECOND, 0);

            int campo;
            int cantidad;
            switch (tipo != null ? tipo.toLowerCase() : "mensual") {
                case "trimestral": campo = java.util.Calendar.MONTH; cantidad = 3; break;
                case "semestral":  campo = java.util.Calendar.MONTH; cantidad = 6; break;
                case "anual":      campo = java.util.Calendar.YEAR;  cantidad = 1; break;
                default:           campo = java.util.Calendar.MONTH; cantidad = 1; break;
            }


            for (int i = 0; i < 1200; i++) {
                java.util.Calendar fin = (java.util.Calendar) inicio.clone();
                fin.add(campo, cantidad);
                if (fin.after(hoy)) break;          // este periodo aún cubre hoy → parar
                inicio.add(campo, cantidad);        // avanzar al siguiente periodo
                int maxDia = inicio.getActualMaximum(java.util.Calendar.DAY_OF_MONTH);
                inicio.set(java.util.Calendar.DAY_OF_MONTH, Math.min(diaAncla, maxDia));
            }

            int maxDiaFinal = inicio.getActualMaximum(java.util.Calendar.DAY_OF_MONTH);
            inicio.set(java.util.Calendar.DAY_OF_MONTH, Math.min(diaAncla, maxDiaFinal));

            return sdf.format(inicio.getTime());
        } catch (Exception e) {
            return fechaInicio;
        }
    }
}