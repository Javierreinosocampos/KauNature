package com.example.kaunatureapplication;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface SupabaseApi {

    // ══════════════════════════════════════════════════════════════════
    //  CLIENTES
    // ══════════════════════════════════════════════════════════════════

    @GET("clientes")
    Call<List<ClienteModel>> getClientes(
            @Query("order")  String order,
            @Query("estado") String estado   // "eq.activo" | null
    );

    @POST("clientes")
    Call<List<ClienteModel>> crearCliente(@Body ClienteModel cliente);

    // Toggle estado (activo/inactivo) — solo actualiza el campo estado
    @PATCH("clientes")
    Call<List<ClienteModel>> actualizarCliente(
            @Query("id") String idFiltro,    // "eq.UUID"
            @Body Map<String, Object> campos
    );

    // Edición completa de campos (nombre, teléfono, email, notas…)
    @PATCH("clientes")
    Call<List<ClienteModel>> actualizarClienteMap(
            @Query("id") String idFiltro,    // "eq.UUID"
            @Body Map<String, Object> campos
    );

    @DELETE("clientes")
    Call<Void> eliminarCliente(@Query("id") String idFiltro);

    // ══════════════════════════════════════════════════════════════════
    //  CITAS
    // ══════════════════════════════════════════════════════════════════

    @GET("v_citas")
    Call<List<CitaModel>> getCitasPorFecha(
            @Query("fecha") String fecha,    // "eq.yyyy-MM-dd"
            @Query("order") String order
    );

    @GET("v_citas")
    Call<List<CitaModel>> getCitasRangoFecha(
            @Query("fecha") String desde,    // "gte.yyyy-MM-dd"
            @Query("fecha") String hasta,    // "lte.yyyy-MM-dd"
            @Query("order") String order
    );

    @POST("citas")
    Call<List<CitaModel>> crearCita(@Body Map<String, Object> cita);

    @PATCH("citas")
    Call<List<CitaModel>> actualizarCita(
            @Query("id") String idFiltro,
            @Body Map<String, Object> campos
    );

    @DELETE("citas")
    Call<Void> eliminarCita(@Query("id") String idFiltro);

    // ══════════════════════════════════════════════════════════════════
    //  COBROS
    // ══════════════════════════════════════════════════════════════════

    @GET("cobros")
    Call<List<CobroModel>> getCobros(
            @Query("order")  String order,
            @Query("estado") String estadoFiltro  // "eq.pendiente" | null
    );

    @GET("cobros")
    Call<List<CobroModel>> getCobrosPorCita(
            @Query("cita_id") String citaIdFiltro  // "eq.UUID"
    );

    @GET("v_kpis_cobros_mes")
    Call<List<KpiCobrosMes>> getKpisCobrosMes();

    @POST("cobros")
    Call<List<CobroModel>> crearCobro(@Body Map<String, Object> cobro);

    @PATCH("cobros")
    Call<List<CobroModel>> actualizarCobro(
            @Query("id") String idFiltro,
            @Body Map<String, Object> campos
    );

    @DELETE("cobros")
    Call<Void> eliminarCobro(@Query("id") String idFiltro);

    // ══════════════════════════════════════════════════════════════════
    //  SERVICIOS
    // ══════════════════════════════════════════════════════════════════

    @GET("servicios")
    Call<List<ServicioModel>> getServicios(@Query("activo") String activo);

    // ══════════════════════════════════════════════════════════════════
    //  HORARIO GIMNASIO — FRANJAS SEMANALES
    // ══════════════════════════════════════════════════════════════════

    @GET("horario_semanal")
    Call<List<FranjaModel>> getFranjas(
            @Query("activo") String activo,  // "eq.true"
            @Query("order")  String order
    );

    @POST("horario_semanal")
    Call<List<FranjaModel>> crearFranja(@Body Map<String, Object> franja);

    @PATCH("horario_semanal")
    Call<List<FranjaModel>> actualizarFranja(
            @Query("id") String idFiltro,
            @Body Map<String, Object> campos
    );

    @DELETE("horario_semanal")
    Call<Void> eliminarFranja(@Query("id") String idFiltro);

    // ══════════════════════════════════════════════════════════════════
    //  ASISTENCIA GIMNASIO
    // ══════════════════════════════════════════════════════════════════

    @GET("asistencia_gimnasio")
    Call<List<AsistenciaModel>> getAsistencia(
            @Query("fecha")              String fecha,    // "eq.yyyy-MM-dd"
            @Query("horario_semanal_id") String franjaId  // "eq.UUID"
    );

    @POST("asistencia_gimnasio")
    Call<List<AsistenciaModel>> apuntarPersona(@Body Map<String, Object> asistencia);

    @DELETE("asistencia_gimnasio")
    Call<Void> quitarPersona(
            @Query("fecha")              String fecha,
            @Query("horario_semanal_id") String franjaId,
            @Query("cliente_nombre")     String nombre
    );

    // ══════════════════════════════════════════════════════════════════
    //  MEMBRESÍAS
    // ══════════════════════════════════════════════════════════════════

    @GET("membresias")
    Call<List<MembresiaModel>> getMembresias(
            @Query("cliente_id") String clienteIdFiltro,
            @Query("activa")     String activaFiltro,
            @Query("order")      String order
    );

    @POST("membresias")
    Call<List<MembresiaModel>> crearMembresia(@Body java.util.Map<String, Object> body);

    @PATCH("membresias")
    Call<List<MembresiaModel>> actualizarMembresia(
            @Query("id") String idFiltro,
            @Body java.util.Map<String, Object> campos
    );

    @DELETE("membresias")
    Call<Void> eliminarMembresia(@Query("id") String idFiltro);

    @GET("cobros")
    Call<List<CobroModel>> getCobrosPorClienteId(
            @Query("cliente_id") String clienteIdFiltro,  // "eq.UUID"
            @Query("order") String order
    );
}
