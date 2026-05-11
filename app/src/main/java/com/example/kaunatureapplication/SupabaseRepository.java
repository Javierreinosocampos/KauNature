package com.example.kaunatureapplication;

import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Capa de acceso a datos. Todas las Activities llaman desde aquí.
 * Nunca hagas llamadas de red directamente en una Activity.
 */
public class SupabaseRepository {

    private static final String TAG = "SupabaseRepo";
    private final SupabaseApi api;

    // ── Singleton ────────────────────────────────────────────────────
    private static SupabaseRepository instance;
    public static SupabaseRepository get() {
        if (instance == null) instance = new SupabaseRepository();
        return instance;
    }
    private SupabaseRepository() {
        api = SupabaseClient.get().create(SupabaseApi.class);
    }

    // ── Callback genérico ────────────────────────────────────────────
    public interface Callback<T> {
        void onSuccess(T data);
        void onError(String mensaje);
    }

    // ════════════════════════════════════════════════════════════════
    //  CLIENTES
    // ════════════════════════════════════════════════════════════════

    /** filtroEstado: "eq.activo" | "eq.inactivo" | null (todos) */
    public void getClientes(String filtroEstado, Callback<List<ClienteModel>> cb) {
        api.getClientes("nombre.asc", filtroEstado)
                .enqueue(new retrofit2.Callback<List<ClienteModel>>() {
                    public void onResponse(Call<List<ClienteModel>> call, Response<List<ClienteModel>> r) {
                        if (r.isSuccessful() && r.body() != null) cb.onSuccess(r.body());
                        else cb.onError("Error " + r.code());
                    }
                    public void onFailure(Call<List<ClienteModel>> call, Throwable t) {
                        cb.onError(t.getMessage());
                    }
                });
    }

    public void crearCliente(ClienteModel c, Callback<ClienteModel> cb) {
        api.crearCliente(c).enqueue(new retrofit2.Callback<List<ClienteModel>>() {
            public void onResponse(Call<List<ClienteModel>> call, Response<List<ClienteModel>> r) {
                if (r.isSuccessful() && r.body() != null && !r.body().isEmpty())
                    cb.onSuccess(r.body().get(0));
                else cb.onError("Error " + r.code());
            }
            public void onFailure(Call<List<ClienteModel>> call, Throwable t) {
                cb.onError(t.getMessage());
            }
        });
    }

    public void toggleEstadoCliente(String id, String nuevoEstado, Callback<Void> cb) {
        Map<String, Object> body = new HashMap<>();
        body.put("estado", nuevoEstado);
        api.actualizarCliente("eq." + id, body)
                .enqueue(new retrofit2.Callback<List<ClienteModel>>() {
                    public void onResponse(Call<List<ClienteModel>> call, Response<List<ClienteModel>> r) {
                        if (r.isSuccessful()) cb.onSuccess(null);
                        else cb.onError("Error " + r.code());
                    }
                    public void onFailure(Call<List<ClienteModel>> call, Throwable t) {
                        cb.onError(t.getMessage());
                    }
                });
    }

    // ════════════════════════════════════════════════════════════════
    //  CITAS
    // ════════════════════════════════════════════════════════════════

    /** fecha: "yyyy-MM-dd" */
    public void getCitasPorFecha(String fecha, Callback<List<CitaModel>> cb) {
        api.getCitasPorFecha("eq." + fecha, "hora.asc")
                .enqueue(new retrofit2.Callback<List<CitaModel>>() {
                    public void onResponse(Call<List<CitaModel>> call, Response<List<CitaModel>> r) {
                        if (r.isSuccessful() && r.body() != null) cb.onSuccess(r.body());
                        else cb.onError("Error " + r.code());
                    }
                    public void onFailure(Call<List<CitaModel>> call, Throwable t) {
                        cb.onError(t.getMessage());
                    }
                });
    }

    /**
     * Citas entre dos fechas "yyyy-MM-dd" inclusive.
     * Usa dos parámetros @Query("fecha") con distintos operadores
     * para que Supabase filtre gte + lte correctamente.
     */
    public void getCitasRango(String desde, String hasta, Callback<List<CitaModel>> cb) {
        api.getCitasRangoFecha("gte." + desde, "lte." + hasta, "fecha.asc,hora.asc")
                .enqueue(new retrofit2.Callback<List<CitaModel>>() {
                    public void onResponse(Call<List<CitaModel>> call, Response<List<CitaModel>> r) {
                        if (r.isSuccessful() && r.body() != null) cb.onSuccess(r.body());
                        else cb.onError("Error " + r.code());
                    }
                    public void onFailure(Call<List<CitaModel>> call, Throwable t) {
                        cb.onError(t.getMessage());
                    }
                });
    }

    public void crearCita(String clienteId, String clienteNombre,
                          String servicioId, String servicioNombre,
                          String fecha, String hora,
                          double precio, String notas,
                          Callback<CitaModel> cb) {
        Map<String, Object> body = new HashMap<>();
        if (clienteId  != null) body.put("cliente_id",      clienteId);
        if (servicioId != null) body.put("servicio_id",     servicioId);
        body.put("cliente_nombre",  clienteNombre);
        body.put("servicio_nombre", servicioNombre);
        body.put("fecha",           fecha);   // "yyyy-MM-dd"
        body.put("hora",            hora);    // "HH:mm"
        body.put("precio",          precio);
        body.put("notas",           notas != null ? notas : "");
        body.put("estado",          "pendiente");

        api.crearCita(body).enqueue(new retrofit2.Callback<List<CitaModel>>() {
            public void onResponse(Call<List<CitaModel>> call, Response<List<CitaModel>> r) {
                if (r.isSuccessful() && r.body() != null && !r.body().isEmpty())
                    cb.onSuccess(r.body().get(0));
                else cb.onError("Error " + r.code());
            }
            public void onFailure(Call<List<CitaModel>> call, Throwable t) {
                cb.onError(t.getMessage());
            }
        });
    }

    public void cambiarEstadoCita(String id, String nuevoEstado, Callback<Void> cb) {
        Map<String, Object> body = new HashMap<>();
        body.put("estado", nuevoEstado);
        api.actualizarCita("eq." + id, body)
                .enqueue(new retrofit2.Callback<List<CitaModel>>() {
                    public void onResponse(Call<List<CitaModel>> call, Response<List<CitaModel>> r) {
                        if (r.isSuccessful()) cb.onSuccess(null);
                        else cb.onError("Error " + r.code());
                    }
                    public void onFailure(Call<List<CitaModel>> call, Throwable t) {
                        cb.onError(t.getMessage());
                    }
                });
    }

    /** Actualiza campos arbitrarios de una cita por su UUID. */
    public void actualizarCita(String id, Map<String, Object> campos, Callback<Void> cb) {
        api.actualizarCita("eq." + id, campos)
                .enqueue(new retrofit2.Callback<List<CitaModel>>() {
                    public void onResponse(Call<List<CitaModel>> call, Response<List<CitaModel>> r) {
                        if (r.isSuccessful()) cb.onSuccess(null);
                        else cb.onError("Error " + r.code());
                    }
                    public void onFailure(Call<List<CitaModel>> call, Throwable t) {
                        cb.onError(t.getMessage());
                    }
                });
    }

    public void eliminarCita(String id, Callback<Void> cb) {
        api.eliminarCita("eq." + id).enqueue(new retrofit2.Callback<Void>() {
            public void onResponse(Call<Void> call, Response<Void> r) {
                if (r.isSuccessful()) cb.onSuccess(null);
                else cb.onError("Error " + r.code());
            }
            public void onFailure(Call<Void> call, Throwable t) {
                cb.onError(t.getMessage());
            }
        });
    }

    // ════════════════════════════════════════════════════════════════
    //  COBROS
    // ════════════════════════════════════════════════════════════════

    /** filtroEstado: "eq.pendiente" | "eq.cobrado" | null (todos) */
    public void getCobros(String filtroEstado, Callback<List<CobroModel>> cb) {
        api.getCobros("fecha.desc", filtroEstado)
                .enqueue(new retrofit2.Callback<List<CobroModel>>() {
                    public void onResponse(Call<List<CobroModel>> call, Response<List<CobroModel>> r) {
                        if (r.isSuccessful() && r.body() != null) cb.onSuccess(r.body());
                        else cb.onError("Error " + r.code());
                    }
                    public void onFailure(Call<List<CobroModel>> call, Throwable t) {
                        cb.onError(t.getMessage());
                    }
                });
    }

    public void crearCobro(String clienteId, String clienteNombre,
                           String concepto, double importe,
                           String metodo, String estado,
                           String notas, Callback<CobroModel> cb) {
        Map<String, Object> body = new HashMap<>();
        if (clienteId != null) body.put("cliente_id", clienteId);
        body.put("cliente_nombre", clienteNombre);
        body.put("concepto",       concepto);
        body.put("importe",        importe);
        body.put("metodo",         metodo);
        body.put("estado",         estado);
        body.put("notas",          notas != null ? notas : "");

        api.crearCobro(body).enqueue(new retrofit2.Callback<List<CobroModel>>() {
            public void onResponse(Call<List<CobroModel>> call, Response<List<CobroModel>> r) {
                if (r.isSuccessful() && r.body() != null && !r.body().isEmpty())
                    cb.onSuccess(r.body().get(0));
                else cb.onError("Error " + r.code());
            }
            public void onFailure(Call<List<CobroModel>> call, Throwable t) {
                cb.onError(t.getMessage());
            }
        });
    }

    public void actualizarCobro(String id, Map<String, Object> campos, Callback<Void> cb) {
        api.actualizarCobro("eq." + id, campos)
                .enqueue(new retrofit2.Callback<List<CobroModel>>() {
                    public void onResponse(Call<List<CobroModel>> call, Response<List<CobroModel>> r) {
                        if (r.isSuccessful()) cb.onSuccess(null);
                        else cb.onError("Error " + r.code());
                    }
                    public void onFailure(Call<List<CobroModel>> call, Throwable t) {
                        cb.onError(t.getMessage());
                    }
                });
    }

    public void marcarCobrado(String id, Callback<Void> cb) {
        Map<String, Object> body = new HashMap<>();
        body.put("estado", "cobrado");
        actualizarCobro(id, body, cb);
    }

    public void eliminarCobro(String id, Callback<Void> cb) {
        api.eliminarCobro("eq." + id).enqueue(new retrofit2.Callback<Void>() {
            public void onResponse(Call<Void> call, Response<Void> r) {
                if (r.isSuccessful()) cb.onSuccess(null);
                else cb.onError("Error " + r.code());
            }
            public void onFailure(Call<Void> call, Throwable t) {
                cb.onError(t.getMessage());
            }
        });
    }

    // ════════════════════════════════════════════════════════════════
    //  SERVICIOS
    // ════════════════════════════════════════════════════════════════

    public void getServicios(Callback<List<ServicioModel>> cb) {
        api.getServicios("eq.true")
                .enqueue(new retrofit2.Callback<List<ServicioModel>>() {
                    public void onResponse(Call<List<ServicioModel>> call, Response<List<ServicioModel>> r) {
                        if (r.isSuccessful() && r.body() != null) cb.onSuccess(r.body());
                        else cb.onError("Error " + r.code());
                    }
                    public void onFailure(Call<List<ServicioModel>> call, Throwable t) {
                        cb.onError(t.getMessage());
                    }
                });
    }

    // ════════════════════════════════════════════════════════════════
    //  GIMNASIO — FRANJAS SEMANALES
    // ════════════════════════════════════════════════════════════════

    /** Todas las franjas activas ordenadas por día y hora */
    public void getFranjas(Callback<List<FranjaModel>> cb) {
        api.getFranjas("eq.true", "dia_semana.asc,hora_inicio.asc")
                .enqueue(new retrofit2.Callback<List<FranjaModel>>() {
                    public void onResponse(Call<List<FranjaModel>> call, Response<List<FranjaModel>> r) {
                        if (r.isSuccessful() && r.body() != null) cb.onSuccess(r.body());
                        else cb.onError("Error " + r.code());
                    }
                    public void onFailure(Call<List<FranjaModel>> call, Throwable t) {
                        cb.onError(t.getMessage());
                    }
                });
    }

    public void crearFranja(int diaSemana, String horaInicio, String horaFin,
                            int aforoMax, Callback<FranjaModel> cb) {
        Map<String, Object> body = new HashMap<>();
        body.put("dia_semana",  diaSemana);
        body.put("hora_inicio", horaInicio);
        body.put("hora_fin",    horaFin);
        body.put("aforo_max",   aforoMax);
        body.put("activo",      true);

        api.crearFranja(body).enqueue(new retrofit2.Callback<List<FranjaModel>>() {
            public void onResponse(Call<List<FranjaModel>> call, Response<List<FranjaModel>> r) {
                if (r.isSuccessful() && r.body() != null && !r.body().isEmpty())
                    cb.onSuccess(r.body().get(0));
                else cb.onError("Error " + r.code());
            }
            public void onFailure(Call<List<FranjaModel>> call, Throwable t) {
                cb.onError(t.getMessage());
            }
        });
    }

    public void actualizarFranja(String id, Map<String, Object> campos, Callback<Void> cb) {
        api.actualizarFranja("eq." + id, campos)
                .enqueue(new retrofit2.Callback<List<FranjaModel>>() {
                    public void onResponse(Call<List<FranjaModel>> call, Response<List<FranjaModel>> r) {
                        if (r.isSuccessful()) cb.onSuccess(null);
                        else cb.onError("Error " + r.code());
                    }
                    public void onFailure(Call<List<FranjaModel>> call, Throwable t) {
                        cb.onError(t.getMessage());
                    }
                });
    }

    /** Eliminar franja → desaparece de TODOS los días de esa semana */
    public void eliminarFranja(String franjaId, Callback<Void> cb) {
        api.eliminarFranja("eq." + franjaId).enqueue(new retrofit2.Callback<Void>() {
            public void onResponse(Call<Void> call, Response<Void> r) {
                if (r.isSuccessful()) cb.onSuccess(null);
                else cb.onError("Error " + r.code());
            }
            public void onFailure(Call<Void> call, Throwable t) {
                cb.onError(t.getMessage());
            }
        });
    }

    // ════════════════════════════════════════════════════════════════
    //  ASISTENCIA GIMNASIO
    // ════════════════════════════════════════════════════════════════

    /** fecha: "yyyy-MM-dd", franjaId: UUID de horario_semanal */
    public void getAsistencia(String fecha, String franjaId, Callback<List<AsistenciaModel>> cb) {
        api.getAsistencia("eq." + fecha, "eq." + franjaId)
                .enqueue(new retrofit2.Callback<List<AsistenciaModel>>() {
                    public void onResponse(Call<List<AsistenciaModel>> call, Response<List<AsistenciaModel>> r) {
                        if (r.isSuccessful() && r.body() != null) cb.onSuccess(r.body());
                        else cb.onError("Error " + r.code());
                    }
                    public void onFailure(Call<List<AsistenciaModel>> call, Throwable t) {
                        cb.onError(t.getMessage());
                    }
                });
    }

    /** clienteId puede ser null para nombres libres */
    public void apuntarPersona(String fecha, String franjaId,
                               String clienteId, String clienteNombre,
                               Callback<Void> cb) {
        Map<String, Object> body = new HashMap<>();
        body.put("fecha",              fecha);
        body.put("horario_semanal_id", franjaId);
        body.put("cliente_nombre",     clienteNombre);
        if (clienteId != null) body.put("cliente_id", clienteId);

        api.apuntarPersona(body).enqueue(new retrofit2.Callback<List<AsistenciaModel>>() {
            public void onResponse(Call<List<AsistenciaModel>> call, Response<List<AsistenciaModel>> r) {
                if (r.isSuccessful()) cb.onSuccess(null);
                else cb.onError("Error " + r.code() + " — ya apuntado o franja llena");
            }
            public void onFailure(Call<List<AsistenciaModel>> call, Throwable t) {
                cb.onError(t.getMessage());
            }
        });
    }

    public void quitarPersona(String fecha, String franjaId,
                              String clienteNombre, Callback<Void> cb) {
        api.quitarPersona("eq." + fecha, "eq." + franjaId, "eq." + clienteNombre)
                .enqueue(new retrofit2.Callback<Void>() {
                    public void onResponse(Call<Void> call, Response<Void> r) {
                        if (r.isSuccessful()) cb.onSuccess(null);
                        else cb.onError("Error " + r.code());
                    }
                    public void onFailure(Call<Void> call, Throwable t) {
                        cb.onError(t.getMessage());
                    }
                });
    }
}