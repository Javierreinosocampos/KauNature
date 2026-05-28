package com.example.kaunatureapplication;

import java.util.Locale;

/**
 * Utilidades de fecha/hora para GimnasioActivity y GimnasioUI.
 * Separadas para que puedan usarse desde cualquier hilo sin depender del contexto Android.
 */
public class GimnasioDateUtils {

    static String hoy() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        return String.format(Locale.getDefault(), "%04d-%02d-%02d",
                cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH) + 1,
                cal.get(java.util.Calendar.DAY_OF_MONTH));
    }

    static String lunesDeSemana(int offsetSemanas) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int dow = cal.get(java.util.Calendar.DAY_OF_WEEK);
        int diasHastaLunes = (dow == java.util.Calendar.SUNDAY) ? -6 : -(dow - java.util.Calendar.MONDAY);
        cal.add(java.util.Calendar.DAY_OF_YEAR, diasHastaLunes + offsetSemanas * 7);
        return String.format(Locale.getDefault(), "%04d-%02d-%02d",
                cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH) + 1,
                cal.get(java.util.Calendar.DAY_OF_MONTH));
    }

    static String sumarDias(String fecha, int dias) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(sdf.parse(fecha));
            cal.add(java.util.Calendar.DAY_OF_YEAR, dias);
            return sdf.format(cal.getTime());
        } catch (Exception e) { return fecha; }
    }

    static int diaSemanaIdx(String fecha) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(sdf.parse(fecha));
            int dow = cal.get(java.util.Calendar.DAY_OF_WEEK);
            return (dow == java.util.Calendar.SUNDAY) ? 6 : dow - java.util.Calendar.MONDAY;
        } catch (Exception e) { return 0; }
    }

    static String lunesBase(String fecha) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(sdf.parse(fecha));
            int dow = cal.get(java.util.Calendar.DAY_OF_WEEK);
            int diff = (dow == java.util.Calendar.SUNDAY) ? -6 : -(dow - java.util.Calendar.MONDAY);
            cal.add(java.util.Calendar.DAY_OF_YEAR, diff);
            return sdf.format(cal.getTime());
        } catch (Exception e) { return fecha; }
    }

    static String fmtLargo(String fecha, String[] diasCorto) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(sdf.parse(fecha));
            int dw = cal.get(java.util.Calendar.DAY_OF_WEEK);
            int di = (dw == java.util.Calendar.SUNDAY) ? 6 : dw - java.util.Calendar.MONDAY;
            int dia  = cal.get(java.util.Calendar.DAY_OF_MONTH);
            int mes  = cal.get(java.util.Calendar.MONTH);
            int anio = cal.get(java.util.Calendar.YEAR);
            String[] mc = {"Ene","Feb","Mar","Abr","May","Jun","Jul","Ago","Sep","Oct","Nov","Dic"};
            return diasCorto[di] + ", " + dia + " " + mc[mes] + " " + anio;
        } catch (Exception e) { return fecha; }
    }

    static String fmtCorto(String fecha) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(sdf.parse(fecha));
            String[] mc = {"Ene","Feb","Mar","Abr","May","Jun","Jul","Ago","Sep","Oct","Nov","Dic"};
            return cal.get(java.util.Calendar.DAY_OF_MONTH) + " " + mc[cal.get(java.util.Calendar.MONTH)];
        } catch (Exception e) { return fecha; }
    }
}