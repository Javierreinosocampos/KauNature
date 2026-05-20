package com.example.kaunatureapplication;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;

public class SupabaseRepository {

    private final SupabaseApi api;

    private static SupabaseRepository instance;
    public static SupabaseRepository get() {
        if (instance == null) instance = new SupabaseRepository();
        return instance;
    }
    private SupabaseRepository() {
        api = SupabaseClient.get().create(SupabaseApi.class);
    }

    public interface Callback<T> {
        void onSuccess(T data);
        void onError(String mensaje);
    }

    // ════════════════════════════════════════════════════════════════
    //  CLIENTES
    // ════════════════════════════════════════════════════════════════

    public void getClientes(String filtroEstado, Callback<List<ClienteModel>> cb) {
        api.getClientes("nombre.asc", filtroEstado)
                .enqueue(new retrofit2.Callback<List<ClienteModel>>() {
                    public void onResponse(Call<List<ClienteModel>> call, Response<List<ClienteModel>> r) {
                        if (r.isSuccessful() && r.body() != null) cb.onSuccess(r.body());
                        else cb.onError("Error " + r.code());
                    }
                    public void onFailure(Call<List<ClienteModel>> call, Throwable t) { cb.onError(t.getMessage()); }
                });
    }

    public void crearCliente(ClienteModel c, Callback<ClienteModel> cb) {
        api.crearCliente(c).enqueue(new retrofit2.Callback<List<ClienteModel>>() {
            public void onResponse(Call<List<ClienteModel>> call, Response<List<ClienteModel>> r) {
                if (r.isSuccessful()) {
                    if (r.body() != null && !r.body().isEmpty()) cb.onSuccess(r.body().get(0));
                    else cb.onSuccess(c);
                } else cb.onError("Error " + r.code());
            }
            public void onFailure(Call<List<ClienteModel>> call, Throwable t) { cb.onError(t.getMessage()); }
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
                    public void onFailure(Call<List<ClienteModel>> call, Throwable t) { cb.onError(t.getMessage()); }
                });
    }

    public void actualizarCliente(String id, Map<String, Object> campos, Callback<Void> cb) {
        api.actualizarClienteMap("eq." + id, campos)
                .enqueue(new retrofit2.Callback<List<ClienteModel>>() {
                    public void onResponse(Call<List<ClienteModel>> call, Response<List<ClienteModel>> r) {
                        if (r.isSuccessful()) cb.onSuccess(null);
                        else cb.onError("Error " + r.code());
                    }
                    public void onFailure(Call<List<ClienteModel>> call, Throwable t) { cb.onError(t.getMessage()); }
                });
    }

    public void eliminarCliente(String id, Callback<Void> cb) {
        api.eliminarCliente("eq." + id).enqueue(new retrofit2.Callback<Void>() {
            public void onResponse(Call<Void> call, Response<Void> r) {
                if (r.isSuccessful()) cb.onSuccess(null);
                else cb.onError("Error " + r.code());
            }
            public void onFailure(Call<Void> call, Throwable t) { cb.onError(t.getMessage()); }
        });
    }

    // ════════════════════════════════════════════════════════════════
    //  CITAS
    // ════════════════════════════════════════════════════════════════

    public void getCitasPorFecha(String fecha, Callback<List<CitaModel>> cb) {
        api.getCitasPorFecha("eq." + fecha, "hora.asc")
                .enqueue(new retrofit2.Callback<List<CitaModel>>() {
                    public void onResponse(Call<List<CitaModel>> call, Response<List<CitaModel>> r) {
                        if (r.isSuccessful() && r.body() != null) cb.onSuccess(r.body());
                        else cb.onError("Error " + r.code());
                    }
                    public void onFailure(Call<List<CitaModel>> call, Throwable t) { cb.onError(t.getMessage()); }
                });
    }

    public void getCitasRango(String desde, String hasta, Callback<List<CitaModel>> cb) {
        api.getCitasRangoFecha("gte." + desde, "lte." + hasta, "fecha.asc,hora.asc")
                .enqueue(new retrofit2.Callback<List<CitaModel>>() {
                    public void onResponse(Call<List<CitaModel>> call, Response<List<CitaModel>> r) {
                        if (r.isSuccessful() && r.body() != null) cb.onSuccess(r.body());
                        else cb.onError("Error " + r.code());
                    }
                    public void onFailure(Call<List<CitaModel>> call, Throwable t) { cb.onError(t.getMessage()); }
                });
    }

    public void crearCita(String clienteId, String clienteNombre,
                          String servicioId, String servicioNombre,
                          String fecha, String hora,
                          double precio, String notas, Callback<CitaModel> cb) {
        Map<String, Object> body = new HashMap<>();
        if (clienteId  != null) body.put("cliente_id",  clienteId);
        if (servicioId != null) body.put("servicio_id", servicioId);
        body.put("cliente_nombre",  clienteNombre);
        body.put("servicio_nombre", servicioNombre);
        body.put("fecha",  fecha);
        body.put("hora",   hora);
        body.put("precio", precio);
        body.put("notas",  notas != null ? notas : "");
        body.put("estado", "pendiente");

        api.crearCita(body).enqueue(new retrofit2.Callback<List<CitaModel>>() {
            public void onResponse(Call<List<CitaModel>> call, Response<List<CitaModel>> r) {
                if (r.isSuccessful()) {
                    if (r.body() != null && !r.body().isEmpty()) cb.onSuccess(r.body().get(0));
                    else cb.onSuccess(new CitaModel());
                } else cb.onError("Error " + r.code());
            }
            public void onFailure(Call<List<CitaModel>> call, Throwable t) { cb.onError(t.getMessage()); }
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
                    public void onFailure(Call<List<CitaModel>> call, Throwable t) { cb.onError(t.getMessage()); }
                });
    }

    public void actualizarCita(String id, Map<String, Object> campos, Callback<Void> cb) {
        api.actualizarCita("eq." + id, campos)
                .enqueue(new retrofit2.Callback<List<CitaModel>>() {
                    public void onResponse(Call<List<CitaModel>> call, Response<List<CitaModel>> r) {
                        if (r.isSuccessful()) cb.onSuccess(null);
                        else cb.onError("Error " + r.code());
                    }
                    public void onFailure(Call<List<CitaModel>> call, Throwable t) { cb.onError(t.getMessage()); }
                });
    }

    public void eliminarCita(String id, Callback<Void> cb) {
        api.eliminarCita("eq." + id).enqueue(new retrofit2.Callback<Void>() {
            public void onResponse(Call<Void> call, Response<Void> r) {
                if (r.isSuccessful()) cb.onSuccess(null);
                else cb.onError("Error " + r.code());
            }
            public void onFailure(Call<Void> call, Throwable t) { cb.onError(t.getMessage()); }
        });
    }

    // ════════════════════════════════════════════════════════════════
    //  COBROS
    // ════════════════════════════════════════════════════════════════

    public void getCobros(String filtroEstado, Callback<List<CobroModel>> cb) {
        api.getCobros("fecha.desc", filtroEstado)
                .enqueue(new retrofit2.Callback<List<CobroModel>>() {
                    public void onResponse(Call<List<CobroModel>> call, Response<List<CobroModel>> r) {
                        if (r.isSuccessful() && r.body() != null) cb.onSuccess(r.body());
                        else cb.onError("Error " + r.code());
                    }
                    public void onFailure(Call<List<CobroModel>> call, Throwable t) { cb.onError(t.getMessage()); }
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
                if (r.isSuccessful()) {
                    if (r.body() != null && !r.body().isEmpty()) cb.onSuccess(r.body().get(0));
                    else cb.onSuccess(new CobroModel());
                } else cb.onError("Error " + r.code());
            }
            public void onFailure(Call<List<CobroModel>> call, Throwable t) { cb.onError(t.getMessage()); }
        });
    }

    public void actualizarCobro(String id, Map<String, Object> campos, Callback<Void> cb) {
        api.actualizarCobro("eq." + id, campos)
                .enqueue(new retrofit2.Callback<List<CobroModel>>() {
                    public void onResponse(Call<List<CobroModel>> call, Response<List<CobroModel>> r) {
                        if (r.isSuccessful()) cb.onSuccess(null);
                        else cb.onError("Error " + r.code());
                    }
                    public void onFailure(Call<List<CobroModel>> call, Throwable t) { cb.onError(t.getMessage()); }
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
            public void onFailure(Call<Void> call, Throwable t) { cb.onError(t.getMessage()); }
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
                    public void onFailure(Call<List<ServicioModel>> call, Throwable t) { cb.onError(t.getMessage()); }
                });
    }

    // ════════════════════════════════════════════════════════════════
    //  GIMNASIO — FRANJAS
    // ════════════════════════════════════════════════════════════════

    public void getFranjas(Callback<List<FranjaModel>> cb) {
        api.getFranjas("eq.true", "dia_semana.asc,hora_inicio.asc")
                .enqueue(new retrofit2.Callback<List<FranjaModel>>() {
                    public void onResponse(Call<List<FranjaModel>> call, Response<List<FranjaModel>> r) {
                        if (r.isSuccessful() && r.body() != null) cb.onSuccess(r.body());
                        else cb.onError("Error " + r.code());
                    }
                    public void onFailure(Call<List<FranjaModel>> call, Throwable t) { cb.onError(t.getMessage()); }
                });
    }

    public void crearFranja(int diaSemana, String horaInicio, String horaFin,
                            int aforoMax, Callback<FranjaModel> cb) {
        Map<String, Object> body = new HashMap<>();
        body.put("dia_semana",  diaSemana);
        body.put("hora_inicio", horaInicio);
        body.put("hora_fin",    horaFin);
        body.put("aforo_max",   aforoMax);
        body.put("activo",      Boolean.TRUE);

        api.crearFranja(body).enqueue(new retrofit2.Callback<List<FranjaModel>>() {
            public void onResponse(Call<List<FranjaModel>> call, Response<List<FranjaModel>> r) {
                if (r.isSuccessful() && r.body() != null && !r.body().isEmpty())
                    cb.onSuccess(r.body().get(0));
                else cb.onError("Error " + r.code());
            }
            public void onFailure(Call<List<FranjaModel>> call, Throwable t) { cb.onError(t.getMessage()); }
        });
    }

    public void actualizarFranja(String id, Map<String, Object> campos, Callback<Void> cb) {
        api.actualizarFranja("eq." + id, campos)
                .enqueue(new retrofit2.Callback<List<FranjaModel>>() {
                    public void onResponse(Call<List<FranjaModel>> call, Response<List<FranjaModel>> r) {
                        if (r.isSuccessful()) cb.onSuccess(null);
                        else cb.onError("Error " + r.code());
                    }
                    public void onFailure(Call<List<FranjaModel>> call, Throwable t) { cb.onError(t.getMessage()); }
                });
    }

    public void eliminarFranja(String franjaId, Callback<Void> cb) {
        api.eliminarFranja("eq." + franjaId).enqueue(new retrofit2.Callback<Void>() {
            public void onResponse(Call<Void> call, Response<Void> r) {
                if (r.isSuccessful()) cb.onSuccess(null);
                else cb.onError("Error " + r.code());
            }
            public void onFailure(Call<Void> call, Throwable t) { cb.onError(t.getMessage()); }
        });
    }

    // ════════════════════════════════════════════════════════════════
    //  ASISTENCIA
    // ════════════════════════════════════════════════════════════════

    public void getAsistencia(String fecha, String franjaId, Callback<List<AsistenciaModel>> cb) {
        api.getAsistencia("eq." + fecha, "eq." + franjaId)
                .enqueue(new retrofit2.Callback<List<AsistenciaModel>>() {
                    public void onResponse(Call<List<AsistenciaModel>> call, Response<List<AsistenciaModel>> r) {
                        if (r.isSuccessful() && r.body() != null) cb.onSuccess(r.body());
                        else cb.onError("Error " + r.code());
                    }
                    public void onFailure(Call<List<AsistenciaModel>> call, Throwable t) { cb.onError(t.getMessage()); }
                });
    }

    public void apuntarPersona(String fecha, String franjaId,
                               String clienteId, String clienteNombre, Callback<Void> cb) {
        Map<String, Object> body = new HashMap<>();
        body.put("fecha",              fecha);
        body.put("horario_semanal_id", franjaId);
        body.put("cliente_nombre",     clienteNombre);
        if (clienteId != null) body.put("cliente_id", clienteId);

        api.apuntarPersona(body).enqueue(new retrofit2.Callback<List<AsistenciaModel>>() {
            public void onResponse(Call<List<AsistenciaModel>> call, Response<List<AsistenciaModel>> r) {
                if (r.isSuccessful()) cb.onSuccess(null);
                else cb.onError("Error " + r.code());
            }
            public void onFailure(Call<List<AsistenciaModel>> call, Throwable t) { cb.onError(t.getMessage()); }
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
                    public void onFailure(Call<Void> call, Throwable t) { cb.onError(t.getMessage()); }
                });
    }

    // ════════════════════════════════════════════════════════════════
    //  MEMBRESÍAS
    //  FIX 400: Boolean.TRUE/FALSE explícito, no mandar campos null,
    //  precio como double nativo de Java.
    // ════════════════════════════════════════════════════════════════

    public void getMembresias(String clienteId, Boolean soloActivas,
                              Callback<List<MembresiaModel>> cb) {
        String filtCliente = clienteId != null ? "eq." + clienteId : null;
        String filtActiva  = (soloActivas != null && soloActivas) ? "eq.true" : null;
        api.getMembresias(filtCliente, filtActiva, "created_at.desc")
                .enqueue(new retrofit2.Callback<List<MembresiaModel>>() {
                    public void onResponse(Call<List<MembresiaModel>> call,
                                           Response<List<MembresiaModel>> r) {
                        if (r.isSuccessful() && r.body() != null) cb.onSuccess(r.body());
                        else cb.onError("Error " + r.code());
                    }
                    public void onFailure(Call<List<MembresiaModel>> call, Throwable t) { cb.onError(t.getMessage()); }
                });
    }

    public void crearMembresia(String clienteId, String tipo, double precio,
                               String fechaInicio, String notas,
                               Callback<MembresiaModel> cb) {
        Map<String, Object> body = new HashMap<>();
        body.put("cliente_id",   clienteId);
        // Usar valor seguro — si el constraint tiene valores concretos en BD usamos el primero
        String tipoSeguro = tipo != null ? tipo : "mensual";
        body.put("tipo",         tipoSeguro);
        // precio mínimo 0.01 para evitar constraint precio > 0
        body.put("precio",       Math.max(precio, 0.01));
        body.put("fecha_inicio", fechaInicio);
        body.put("activa",       Boolean.TRUE);
        body.put("notas",        notas != null ? notas.trim() : "");

        android.util.Log.d("MEMBRESIA", "POST body: " + body.toString());

        api.crearMembresia(body).enqueue(new retrofit2.Callback<List<MembresiaModel>>() {
            public void onResponse(Call<List<MembresiaModel>> call,
                                   Response<List<MembresiaModel>> r) {
                if (r.isSuccessful()) {
                    if (r.body() != null && !r.body().isEmpty()) {
                        cb.onSuccess(r.body().get(0));
                    } else {
                        MembresiaModel d = new MembresiaModel();
                        d.clienteId   = clienteId;
                        d.tipo        = tipo;
                        d.precio      = precio;
                        d.fechaInicio = fechaInicio;
                        d.activa      = true;
                        cb.onSuccess(d);
                    }
                } else {
                    // Leer el body del error para saber qué rechaza Supabase
                    String errorBody = "";
                    try {
                        if (r.errorBody() != null) errorBody = r.errorBody().string();
                    } catch (Exception ignored) {}
                    android.util.Log.e("MEMBRESIA", "Error " + r.code() + ": " + errorBody);
                    cb.onError("Error " + r.code() + " — " + errorBody);
                }
            }
            public void onFailure(Call<List<MembresiaModel>> call, Throwable t) {
                android.util.Log.e("MEMBRESIA", "Failure: " + t.getMessage());
                cb.onError(t.getMessage());
            }
        });
    }

    public void cancelarMembresia(String id, String fechaFin, Callback<Void> cb) {
        Map<String, Object> body = new HashMap<>();
        body.put("activa",    Boolean.FALSE);
        body.put("fecha_fin", fechaFin);
        api.actualizarMembresia("eq." + id, body)
                .enqueue(new retrofit2.Callback<List<MembresiaModel>>() {
                    public void onResponse(Call<List<MembresiaModel>> call,
                                           Response<List<MembresiaModel>> r) {
                        if (r.isSuccessful()) cb.onSuccess(null);
                        else cb.onError("Error " + r.code());
                    }
                    public void onFailure(Call<List<MembresiaModel>> call, Throwable t) { cb.onError(t.getMessage()); }
                });
    }

    public void renovarMembresia(String id, String nuevaFechaInicio, Callback<Void> cb) {
        Map<String, Object> body = new HashMap<>();
        body.put("fecha_inicio", nuevaFechaInicio);
        body.put("activa",       Boolean.TRUE);
        api.actualizarMembresia("eq." + id, body)
                .enqueue(new retrofit2.Callback<List<MembresiaModel>>() {
                    public void onResponse(Call<List<MembresiaModel>> call,
                                           Response<List<MembresiaModel>> r) {
                        if (r.isSuccessful()) cb.onSuccess(null);
                        else cb.onError("Error " + r.code());
                    }
                    public void onFailure(Call<List<MembresiaModel>> call, Throwable t) { cb.onError(t.getMessage()); }
                });
    }

    public void actualizarPrecioMembresia(String id, double nuevoPrecio, Callback<Void> cb) {
        Map<String, Object> body = new HashMap<>();
        body.put("precio", nuevoPrecio);
        api.actualizarMembresia("eq." + id, body)
                .enqueue(new retrofit2.Callback<List<MembresiaModel>>() {
                    public void onResponse(Call<List<MembresiaModel>> call,
                                           Response<List<MembresiaModel>> r) {
                        if (r.isSuccessful()) cb.onSuccess(null);
                        else cb.onError("Error " + r.code());
                    }
                    public void onFailure(Call<List<MembresiaModel>> call, Throwable t) { cb.onError(t.getMessage()); }
                });
    }
}