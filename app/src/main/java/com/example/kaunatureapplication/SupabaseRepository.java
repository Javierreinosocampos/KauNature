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
    public static void reset() { instance = null; }
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

    public void getCobrosPorCita(String citaId, Callback<List<CobroModel>> cb) {
        api.getCobrosPorCita("eq." + citaId)
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
        crearCobro(null, clienteId, clienteNombre, concepto, importe, metodo, estado, notas, cb);
    }

    public void crearCobro(String citaId, String clienteId, String clienteNombre,
                           String concepto, double importe,
                           String metodo, String estado,
                           String notas, Callback<CobroModel> cb) {
        Map<String, Object> body = new HashMap<>();
        if (citaId   != null) body.put("cita_id",   citaId);
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
                    if (r.body() != null && !r.body().isEmpty()) {
                        cb.onSuccess(r.body().get(0));
                    } else {
                        CobroModel dummy = new CobroModel();
                        dummy.citaId         = citaId;
                        dummy.clienteNombre = clienteNombre;
                        dummy.concepto      = concepto;
                        dummy.importe       = importe;
                        dummy.metodo        = metodo;
                        dummy.estado        = estado;
                        dummy.clienteId     = clienteId;
                        cb.onSuccess(dummy);
                    }
                } else cb.onError("Error " + r.code());
            }
            public void onFailure(Call<List<CobroModel>> call, Throwable t) { cb.onError(t.getMessage()); }
        });
    }

    /** Igual que crearCobro pero fijando la FECHA del cobro (yyyy-MM-dd). */
    public void crearCobroConFecha(String clienteId, String clienteNombre,
                                   String concepto, double importe,
                                   String metodo, String estado, String notas,
                                   String fecha, Callback<CobroModel> cb) {
        Map<String, Object> body = new HashMap<>();
        if (clienteId != null) body.put("cliente_id", clienteId);
        body.put("cliente_nombre", clienteNombre);
        body.put("concepto",       concepto);
        body.put("importe",        importe);
        body.put("metodo",         metodo);
        body.put("estado",         estado);
        body.put("notas",          notas != null ? notas : "");
        if (fecha != null && !fecha.isEmpty()) body.put("fecha", fecha);

        api.crearCobro(body).enqueue(new retrofit2.Callback<List<CobroModel>>() {
            public void onResponse(Call<List<CobroModel>> call, Response<List<CobroModel>> r) {
                if (r.isSuccessful()) {
                    if (r.body() != null && !r.body().isEmpty()) cb.onSuccess(r.body().get(0));
                    else {
                        CobroModel dummy = new CobroModel();
                        dummy.clienteId = clienteId; dummy.clienteNombre = clienteNombre;
                        dummy.concepto = concepto; dummy.importe = importe;
                        dummy.metodo = metodo; dummy.estado = estado; dummy.fecha = fecha;
                        cb.onSuccess(dummy);
                    }
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
    //  SINCRONIZACIÓN BIDIRECCIONAL CITAS ↔ COBROS
    // ════════════════════════════════════════════════════════════════

    /**
     * Marca un cobro como COBRADO y sincroniza la cita asociada (si existe)
     * FLUJO: Usuario marca cobro como cobrado → Cita pasa a "cobrada"
     */
    public void marcarCobroCobradoConSync(String cobroId, String citaId, Callback<Void> cb) {
        android.util.Log.d("SYNC", "marcarCobroCobrado: cobro=" + cobroId + ", cita=" + citaId);

        Map<String, Object> body = new HashMap<>();
        body.put("estado", "cobrado");

        api.actualizarCobro("eq." + cobroId, body)
                .enqueue(new retrofit2.Callback<List<CobroModel>>() {
                    public void onResponse(Call<List<CobroModel>> call, Response<List<CobroModel>> r) {
                        if (r.isSuccessful()) {
                            android.util.Log.d("SYNC", "✓ Cobro actualizado a cobrado");

                            if (citaId != null && !citaId.isEmpty()) {
                                Map<String, Object> citaBody = new HashMap<>();
                                citaBody.put("estado", "cobrada");

                                api.actualizarCita("eq." + citaId, citaBody)
                                        .enqueue(new retrofit2.Callback<List<CitaModel>>() {
                                            public void onResponse(Call<List<CitaModel>> call2, Response<List<CitaModel>> r2) {
                                                if (r2.isSuccessful()) {
                                                    android.util.Log.d("SYNC", "✓ Cita sincronizada a cobrada");
                                                    cb.onSuccess(null);
                                                } else {
                                                    android.util.Log.w("SYNC", "⚠ Error al actualizar cita: " + r2.code());
                                                    cb.onError("Cobro actualizado pero error al sincronizar cita");
                                                }
                                            }
                                            public void onFailure(Call<List<CitaModel>> call2, Throwable t) {
                                                android.util.Log.w("SYNC", "⚠ Error al actualizar cita: " + t.getMessage());
                                                cb.onError("Cobro actualizado pero error al sincronizar cita");
                                            }
                                        });
                            } else {
                                android.util.Log.d("SYNC", "✓ Cobro sin cita asociada");
                                cb.onSuccess(null);
                            }
                        } else {
                            cb.onError("Error " + r.code());
                        }
                    }
                    public void onFailure(Call<List<CobroModel>> call, Throwable t) {
                        cb.onError(t.getMessage());
                    }
                });
    }

    /**
     * Marca un cobro como PENDIENTE y sincroniza la cita asociada (si existe)
     * FLUJO: Usuario desmarca cobro → Cita pasa a "confirmada"
     */
    public void marcarCobroPendienteConSync(String cobroId, String citaId, Callback<Void> cb) {
        android.util.Log.d("SYNC", "marcarCobroPendiente: cobro=" + cobroId + ", cita=" + citaId);

        Map<String, Object> body = new HashMap<>();
        body.put("estado", "pendiente");

        api.actualizarCobro("eq." + cobroId, body)
                .enqueue(new retrofit2.Callback<List<CobroModel>>() {
                    public void onResponse(Call<List<CobroModel>> call, Response<List<CobroModel>> r) {
                        if (r.isSuccessful()) {
                            android.util.Log.d("SYNC", "✓ Cobro actualizado a pendiente");

                            if (citaId != null && !citaId.isEmpty()) {
                                Map<String, Object> citaBody = new HashMap<>();
                                citaBody.put("estado", "confirmada");

                                api.actualizarCita("eq." + citaId, citaBody)
                                        .enqueue(new retrofit2.Callback<List<CitaModel>>() {
                                            public void onResponse(Call<List<CitaModel>> call2, Response<List<CitaModel>> r2) {
                                                if (r2.isSuccessful()) {
                                                    android.util.Log.d("SYNC", "✓ Cita sincronizada a confirmada");
                                                    cb.onSuccess(null);
                                                } else {
                                                    android.util.Log.w("SYNC", "⚠ Error al actualizar cita: " + r2.code());
                                                    cb.onError("Cobro actualizado pero error al sincronizar cita");
                                                }
                                            }
                                            public void onFailure(Call<List<CitaModel>> call2, Throwable t) {
                                                android.util.Log.w("SYNC", "⚠ Error al actualizar cita: " + t.getMessage());
                                                cb.onError("Cobro actualizado pero error al sincronizar cita");
                                            }
                                        });
                            } else {
                                android.util.Log.d("SYNC", "✓ Cobro sin cita asociada");
                                cb.onSuccess(null);
                            }
                        } else {
                            cb.onError("Error " + r.code());
                        }
                    }
                    public void onFailure(Call<List<CobroModel>> call, Throwable t) {
                        cb.onError(t.getMessage());
                    }
                });
    }

    /**
     * ELIMINA un cobro y CANCELA la cita asociada (si existe)
     * FLUJO: Usuario elimina cobro → Cita pasa a "cancelada"
     */
    public void eliminarCobroConSync(String cobroId, String citaId, Callback<Void> cb) {
        android.util.Log.d("SYNC", "eliminarCobro: cobro=" + cobroId + ", cita=" + citaId);

        api.eliminarCobro("eq." + cobroId).enqueue(new retrofit2.Callback<Void>() {
            public void onResponse(Call<Void> call, Response<Void> r) {
                if (r.isSuccessful()) {
                    android.util.Log.d("SYNC", "✓ Cobro eliminado");

                    if (citaId != null && !citaId.isEmpty()) {
                        Map<String, Object> citaBody = new HashMap<>();
                        citaBody.put("estado", "cancelada");

                        api.actualizarCita("eq." + citaId, citaBody)
                                .enqueue(new retrofit2.Callback<List<CitaModel>>() {
                                    public void onResponse(Call<List<CitaModel>> call2, Response<List<CitaModel>> r2) {
                                        if (r2.isSuccessful()) {
                                            android.util.Log.d("SYNC", "✓ Cita cancelada");
                                            cb.onSuccess(null);
                                        } else {
                                            android.util.Log.w("SYNC", "⚠ Error al cancelar cita: " + r2.code());
                                            cb.onError("Cobro eliminado pero error al cancelar cita");
                                        }
                                    }
                                    public void onFailure(Call<List<CitaModel>> call2, Throwable t) {
                                        android.util.Log.w("SYNC", "⚠ Error al cancelar cita: " + t.getMessage());
                                        cb.onError("Cobro eliminado pero error al cancelar cita");
                                    }
                                });
                    } else {
                        android.util.Log.d("SYNC", "✓ Cobro sin cita asociada");
                        cb.onSuccess(null);
                    }
                } else {
                    cb.onError("Error " + r.code());
                }
            }
            public void onFailure(Call<Void> call, Throwable t) {
                cb.onError(t.getMessage());
            }
        });
    }

    /**
     * Marca una cita como COBRADA y sincroniza el cobro asociado (si existe)
     */
    public void marcarCitaCobradaConSync(String citaId, Callback<Void> cb) {
        android.util.Log.d("SYNC", "marcarCitaCobrada: cita=" + citaId);

        Map<String, Object> citaBody = new HashMap<>();
        citaBody.put("estado", "cobrada");

        api.actualizarCita("eq." + citaId, citaBody)
                .enqueue(new retrofit2.Callback<List<CitaModel>>() {
                    public void onResponse(Call<List<CitaModel>> call, Response<List<CitaModel>> r) {
                        if (r.isSuccessful()) {
                            android.util.Log.d("SYNC", "✓ Cita actualizada a cobrada");

                            getCobrosPorCita(citaId, new Callback<List<CobroModel>>() {
                                @Override
                                public void onSuccess(List<CobroModel> cobros) {
                                    if (cobros != null && !cobros.isEmpty()) {
                                        CobroModel cobro = cobros.get(0);
                                        android.util.Log.d("SYNC", "✓ Cobro encontrado: " + cobro.id);

                                        Map<String, Object> cobroBody = new HashMap<>();
                                        cobroBody.put("estado", "cobrado");

                                        api.actualizarCobro("eq." + cobro.id, cobroBody)
                                                .enqueue(new retrofit2.Callback<List<CobroModel>>() {
                                                    public void onResponse(Call<List<CobroModel>> call2, Response<List<CobroModel>> r2) {
                                                        if (r2.isSuccessful()) {
                                                            android.util.Log.d("SYNC", "✓ Cobro sincronizado a cobrado");
                                                            cb.onSuccess(null);
                                                        } else {
                                                            android.util.Log.w("SYNC", "⚠ Error al actualizar cobro");
                                                            cb.onError("Cita actualizada pero error al sincronizar cobro");
                                                        }
                                                    }
                                                    public void onFailure(Call<List<CobroModel>> call2, Throwable t) {
                                                        android.util.Log.w("SYNC", "⚠ Error al actualizar cobro");
                                                        cb.onError("Cita actualizada pero error al sincronizar cobro");
                                                    }
                                                });
                                    } else {
                                        android.util.Log.d("SYNC", "✓ Cita sin cobro asociado");
                                        cb.onSuccess(null);
                                    }
                                }
                                @Override
                                public void onError(String mensaje) {
                                    android.util.Log.w("SYNC", "⚠ Error buscando cobro: " + mensaje);
                                    cb.onSuccess(null);
                                }
                            });
                        } else {
                            cb.onError("Error " + r.code());
                        }
                    }
                    public void onFailure(Call<List<CitaModel>> call, Throwable t) {
                        cb.onError(t.getMessage());
                    }
                });
    }

    /**
     * Cambia estado de cita y sincroniza cobro asociado
     */
    public void cambiarEstadoCitaConSync(String citaId, String nuevoEstado, Callback<Void> cb) {
        android.util.Log.d("SYNC", "cambiarEstadoCita: cita=" + citaId + ", estado=" + nuevoEstado);

        Map<String, Object> citaBody = new HashMap<>();
        citaBody.put("estado", nuevoEstado);

        api.actualizarCita("eq." + citaId, citaBody)
                .enqueue(new retrofit2.Callback<List<CitaModel>>() {
                    public void onResponse(Call<List<CitaModel>> call, Response<List<CitaModel>> r) {
                        if (r.isSuccessful()) {
                            android.util.Log.d("SYNC", "✓ Cita actualizada a " + nuevoEstado);

                            getCobrosPorCita(citaId, new Callback<List<CobroModel>>() {
                                @Override
                                public void onSuccess(List<CobroModel> cobros) {
                                    if (cobros != null && !cobros.isEmpty()) {
                                        CobroModel cobro = cobros.get(0);
                                        android.util.Log.d("SYNC", "✓ Cobro encontrado: " + cobro.id);

                                        String estadoCobro;
                                        switch (nuevoEstado) {
                                            case "cobrada":
                                                estadoCobro = "cobrado";
                                                break;
                                            case "confirmada":
                                            case "pendiente":
                                                estadoCobro = "pendiente";
                                                break;
                                            case "cancelada":
                                                estadoCobro = "pendiente";
                                                break;
                                            default:
                                                estadoCobro = "pendiente";
                                        }

                                        Map<String, Object> cobroBody = new HashMap<>();
                                        cobroBody.put("estado", estadoCobro);

                                        api.actualizarCobro("eq." + cobro.id, cobroBody)
                                                .enqueue(new retrofit2.Callback<List<CobroModel>>() {
                                                    public void onResponse(Call<List<CobroModel>> call2, Response<List<CobroModel>> r2) {
                                                        if (r2.isSuccessful()) {
                                                            android.util.Log.d("SYNC", "✓ Cobro sincronizado a " + estadoCobro);
                                                            cb.onSuccess(null);
                                                        } else {
                                                            android.util.Log.w("SYNC", "⚠ Error al actualizar cobro");
                                                            cb.onSuccess(null);
                                                        }
                                                    }
                                                    public void onFailure(Call<List<CobroModel>> call2, Throwable t) {
                                                        android.util.Log.w("SYNC", "⚠ Error al actualizar cobro");
                                                        cb.onSuccess(null);
                                                    }
                                                });
                                    } else {
                                        android.util.Log.d("SYNC", "✓ Cita sin cobro asociado");
                                        cb.onSuccess(null);
                                    }
                                }
                                @Override
                                public void onError(String mensaje) {
                                    android.util.Log.w("SYNC", "⚠ Error buscando cobro: " + mensaje);
                                    cb.onSuccess(null);
                                }
                            });
                        } else {
                            cb.onError("Error " + r.code());
                        }
                    }
                    public void onFailure(Call<List<CitaModel>> call, Throwable t) {
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
        String tipoSeguro = tipo != null ? tipo : "mensual";
        body.put("tipo",         tipoSeguro);
        body.put("precio",       Math.max(precio, 0.01));
        body.put("fecha_inicio", fechaInicio);
        body.put("activa",       Boolean.TRUE);
        body.put("notas",        notas != null ? notas.trim() : "");

        android.util.Log.d("MEMBRESIA", "POST body: " + body.toString());

        api.crearMembresia(body).enqueue(new retrofit2.Callback<List<MembresiaModel>>() {
            public void onResponse(Call<List<MembresiaModel>> call,
                                   Response<List<MembresiaModel>> r) {
                if (r.isSuccessful()) {
                    MembresiaModel mem;
                    if (r.body() != null && !r.body().isEmpty()) {
                        mem = r.body().get(0);
                    } else {
                        mem = new MembresiaModel();
                        mem.clienteId   = clienteId;
                        mem.tipo        = tipo;
                        mem.precio      = precio;
                        mem.fechaInicio = fechaInicio;
                        mem.activa      = true;
                    }

                    // ── Si Supabase ignoró nuestra fecha (DEFAULT now()),
                    //    hacemos un PATCH inmediato para corregirla ──────
                    if (mem.id != null && !fechaInicio.equals(mem.fechaInicio)) {
                        Map<String, Object> patch = new HashMap<>();
                        patch.put("fecha_inicio", fechaInicio);
                        api.actualizarMembresia("eq." + mem.id, patch)
                                .enqueue(new retrofit2.Callback<List<MembresiaModel>>() {
                                    public void onResponse(Call<List<MembresiaModel>> c2,
                                                           Response<List<MembresiaModel>> r2) {
                                        mem.fechaInicio = fechaInicio; // corregir en memoria
                                        cb.onSuccess(mem);
                                    }
                                    public void onFailure(Call<List<MembresiaModel>> c2, Throwable t) {
                                        // El PATCH falló pero la membresía existe — devolver igualmente
                                        android.util.Log.e("MEMBRESIA", "PATCH fecha falló: " + t.getMessage());
                                        cb.onSuccess(mem);
                                    }
                                });
                    } else {
                        cb.onSuccess(mem);
                    }

                } else {
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

    /** Devuelve true si el cliente YA tiene al menos una membresía activa. */
    public void tieneMembresiaActiva(String clienteId, Callback<Boolean> cb) {
        if (clienteId == null) { cb.onSuccess(false); return; }
        getMembresias(clienteId, true, new Callback<List<MembresiaModel>>() {
            @Override public void onSuccess(List<MembresiaModel> list) {
                cb.onSuccess(list != null && !list.isEmpty());
            }
            @Override public void onError(String e) { cb.onError(e); }
        });
    }

    public void eliminarMembresia(String id, Callback<Void> cb) {
        api.eliminarMembresia("eq." + id).enqueue(new retrofit2.Callback<Void>() {
            public void onResponse(Call<Void> call, Response<Void> r) {
                if (r.isSuccessful()) cb.onSuccess(null);
                else cb.onError("Error " + r.code());
            }
            public void onFailure(Call<Void> call, Throwable t) { cb.onError(t.getMessage()); }
        });
    }

    // ── Borrado en cascada: elimina un cliente junto a TODOS sus cobros y membresías ──
    public void eliminarClienteEnCascada(String clienteId, Callback<Void> cb) {
        if (clienteId == null) { cb.onError("clienteId nulo"); return; }
        // 1) Traer cobros del cliente
        getCobrosPorCliente(clienteId, new Callback<List<CobroModel>>() {
            @Override public void onSuccess(List<CobroModel> cobros) {
                final java.util.List<String> idsCobros = new java.util.ArrayList<>();
                if (cobros != null) {
                    for (CobroModel c : cobros) if (c.id != null) idsCobros.add(c.id);
                }
                // 2) Borrar cobros en cadena
                borrarCobrosEnCadena(idsCobros, 0, () ->
                        // 3) Traer y borrar membresías del cliente
                        getMembresias(clienteId, null, new Callback<List<MembresiaModel>>() {
                            @Override public void onSuccess(List<MembresiaModel> mems) {
                                final java.util.List<String> idsMems = new java.util.ArrayList<>();
                                if (mems != null) {
                                    for (MembresiaModel m : mems) if (m.id != null) idsMems.add(m.id);
                                }
                                borrarMembresiasEnCadena(idsMems, 0, () ->
                                        // 4) Por último, borrar el cliente
                                        eliminarCliente(clienteId, cb));
                            }
                            @Override public void onError(String e) {
                                // Si falla traer membresías, intentamos borrar el cliente igualmente
                                eliminarCliente(clienteId, cb);
                            }
                        }));
            }
            @Override public void onError(String e) {
                // Si falla traer cobros, intentamos borrar el cliente igualmente
                eliminarCliente(clienteId, cb);
            }
        });
    }

    // ── Borra un cobro y, si correspondía a una membresía ACTIVA, también la membresía ──
    public void eliminarCobroYMembresiaSiAplica(String cobroId, String citaId,
                                                String concepto, String clienteId,
                                                Callback<Void> cb) {
        if (cobroId == null) { cb.onError("cobro nulo"); return; }
        final String conceptoLow = concepto != null ? concepto.toLowerCase() : "";
        eliminarCobroConSync(cobroId, citaId, new Callback<Void>() {
            @Override public void onSuccess(Void v) {
                boolean esMembresia = conceptoLow.contains("membres") || conceptoLow.contains("renovaci");
                if (!esMembresia || clienteId == null) { cb.onSuccess(null); return; }
                // Buscar membresías activas del cliente y eliminarlas
                getMembresias(clienteId, true, new Callback<List<MembresiaModel>>() {
                    @Override public void onSuccess(List<MembresiaModel> activas) {
                        if (activas == null || activas.isEmpty()) { cb.onSuccess(null); return; }
                        final java.util.List<String> ids = new java.util.ArrayList<>();
                        for (MembresiaModel m : activas) if (m.id != null) ids.add(m.id);
                        borrarMembresiasEnCadena(ids, 0, () -> cb.onSuccess(null));
                    }
                    @Override public void onError(String e) { cb.onSuccess(null); } // el cobro ya se borró
                });
            }
            @Override public void onError(String e) { cb.onError(e); }
        });
    }

    private void borrarCobrosEnCadena(java.util.List<String> ids, int idx, Runnable onDone) {
        if (ids == null || idx >= ids.size()) { onDone.run(); return; }
        eliminarCobro(ids.get(idx), new Callback<Void>() {
            @Override public void onSuccess(Void v) { borrarCobrosEnCadena(ids, idx + 1, onDone); }
            @Override public void onError(String e) { borrarCobrosEnCadena(ids, idx + 1, onDone); }
        });
    }

    private void borrarMembresiasEnCadena(java.util.List<String> ids, int idx, Runnable onDone) {
        if (ids == null || idx >= ids.size()) { onDone.run(); return; }
        eliminarMembresia(ids.get(idx), new Callback<Void>() {
            @Override public void onSuccess(Void v) { borrarMembresiasEnCadena(ids, idx + 1, onDone); }
            @Override public void onError(String e) { borrarMembresiasEnCadena(ids, idx + 1, onDone); }
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

    /**
     * Pone al día una membresía activa:
     *  - Genera un cobro por CADA periodo (mes/trimestre/…) desde la fecha de inicio
     *    hasta hoy que aún no tenga cobro, fechado en su mes correspondiente.
     *  - Avanza la fecha_inicio de la membresía al periodo actual conservando el día.
     * Es idempotente: si ya está al día, no hace nada.
     */
    public void sincronizarMembresia(MembresiaModel mem, String nombre, Callback<Boolean> cb) {
        if (mem == null || !mem.activa || mem.clienteId == null
                || mem.fechaInicio == null || mem.fechaInicio.isEmpty()) {
            cb.onSuccess(false);
            return;
        }
        final String tipo = mem.tipo != null ? mem.tipo.toLowerCase() : "mensual";
        final int campo;
        final int cantidad;
        switch (tipo) {
            case "trimestral": campo = java.util.Calendar.MONTH; cantidad = 3; break;
            case "semestral":  campo = java.util.Calendar.MONTH; cantidad = 6; break;
            case "anual":      campo = java.util.Calendar.YEAR;  cantidad = 1; break;
            default:           campo = java.util.Calendar.MONTH; cantidad = 1; break;
        }

        getCobrosPorCliente(mem.clienteId, new Callback<List<CobroModel>>() {
            @Override public void onSuccess(List<CobroModel> cobros) {
                try {
                    java.text.SimpleDateFormat sdf =
                            new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());

                    // Meses (yyyy-MM) que YA tienen un cobro de membresía
                    java.util.Set<String> mesesConCobro = new java.util.HashSet<>();
                    if (cobros != null) {
                        for (CobroModel c : cobros) {
                            String con = c.concepto != null ? c.concepto.toLowerCase() : "";
                            if (!(con.contains("membres") || con.contains("renovaci"))) continue;
                            String f = null;
                            if (c.fecha != null && c.fecha.length() >= 7) f = c.fecha;
                            else if (c.createdAt != null && c.createdAt.length() >= 7) f = c.createdAt;
                            if (f != null) mesesConCobro.add(f.substring(0, 7));
                        }
                    }

                    java.util.Date dInicio = sdf.parse(mem.fechaInicio);
                    if (dInicio == null) { cb.onSuccess(false); return; }

                    java.util.Calendar cursor = java.util.Calendar.getInstance();
                    cursor.setTime(dInicio);
                    final int diaAncla = cursor.get(java.util.Calendar.DAY_OF_MONTH);

                    java.util.Calendar hoy = java.util.Calendar.getInstance();
                    hoy.set(java.util.Calendar.HOUR_OF_DAY, 0);
                    hoy.set(java.util.Calendar.MINUTE, 0);
                    hoy.set(java.util.Calendar.SECOND, 0);
                    hoy.set(java.util.Calendar.MILLISECOND, 0);

                    java.util.List<String> aCrear = new java.util.ArrayList<>();
                    String inicioPeriodoActual = mem.fechaInicio;

                    for (int i = 0; i < 1200; i++) {
                        if (cursor.after(hoy)) break;               // periodo aún no empezado
                        String fechaCobro = sdf.format(cursor.getTime());
                        inicioPeriodoActual = fechaCobro;           // último periodo <= hoy
                        String mes = fechaCobro.substring(0, 7);
                        if (!mesesConCobro.contains(mes)) {
                            aCrear.add(fechaCobro);
                            mesesConCobro.add(mes);                 // no duplicar dentro del bucle
                        }
                        cursor.add(campo, cantidad);
                        int maxDia = cursor.getActualMaximum(java.util.Calendar.DAY_OF_MONTH);
                        cursor.set(java.util.Calendar.DAY_OF_MONTH, Math.min(diaAncla, maxDia));
                    }

                    final String inicioActual = inicioPeriodoActual;
                    final boolean cambiaFecha = !inicioActual.equals(mem.fechaInicio);
                    final boolean huboCobros  = !aCrear.isEmpty();

                    // 1) Crear los cobros que faltan, en cadena
                    crearCobrosFaltantes(mem, nombre, aCrear, 0, () -> {
                        // 2) Avanzar fecha_inicio al periodo actual
                        if (cambiaFecha) {
                            Map<String, Object> patch = new HashMap<>();
                            patch.put("fecha_inicio", inicioActual);
                            api.actualizarMembresia("eq." + mem.id, patch)
                                    .enqueue(new retrofit2.Callback<List<MembresiaModel>>() {
                                        public void onResponse(Call<List<MembresiaModel>> c2,
                                                               Response<List<MembresiaModel>> r2) {
                                            mem.fechaInicio = inicioActual;
                                            cb.onSuccess(huboCobros || cambiaFecha);
                                        }
                                        public void onFailure(Call<List<MembresiaModel>> c2, Throwable t) {
                                            cb.onSuccess(huboCobros);
                                        }
                                    });
                        } else {
                            cb.onSuccess(huboCobros);
                        }
                    });
                } catch (Exception e) {
                    cb.onError(e.getMessage());
                }
            }
            @Override public void onError(String e) { cb.onError(e); }
        });
    }

    private void crearCobrosFaltantes(MembresiaModel mem, String nombre,
                                      java.util.List<String> fechas, int idx, Runnable onDone) {
        if (fechas == null || idx >= fechas.size()) { onDone.run(); return; }
        String fecha   = fechas.get(idx);
        String tipoTxt = (mem.tipo != null && !mem.tipo.isEmpty()) ? mem.tipo.toLowerCase() : "mensual";
        String concepto = "Membresía " + tipoTxt + " · " + nombre;
        String notas    = "Cobro generado automáticamente (" + fecha.substring(0, 7) + ")";
        crearCobroConFecha(mem.clienteId, nombre, concepto, mem.precio,
                "Efectivo", "pendiente", notas, fecha,
                new Callback<CobroModel>() {
                    @Override public void onSuccess(CobroModel c) {
                        crearCobrosFaltantes(mem, nombre, fechas, idx + 1, onDone);
                    }
                    @Override public void onError(String e) {
                        crearCobrosFaltantes(mem, nombre, fechas, idx + 1, onDone);
                    }
                });
    }

    public void getCobrosPorCliente(String clienteId, Callback<List<CobroModel>> cb) {
        api.getCobrosPorClienteId("eq." + clienteId, "fecha.desc")
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

    public void generarCobroMensualSiProcede(MembresiaModel membresia, String nombreCliente,
                                             Callback<Boolean> cb) {
        if (membresia == null || !membresia.activa || membresia.clienteId == null) {
            cb.onSuccess(false);
            return;
        }

        java.text.SimpleDateFormat sdfMes = new java.text.SimpleDateFormat("yyyy-MM",
                java.util.Locale.getDefault());
        final String mesActual = sdfMes.format(new java.util.Date());

        getCobrosPorCliente(membresia.clienteId, new Callback<List<CobroModel>>() {
            @Override
            public void onSuccess(List<CobroModel> cobros) {
                boolean yaExiste = false;

                for (CobroModel cobro : cobros) {
                    boolean fechaCoincide = false;
                    if (cobro.fecha != null && cobro.fecha.startsWith(mesActual)) {
                        fechaCoincide = true;
                    } else if (cobro.createdAt != null && cobro.createdAt.startsWith(mesActual)) {
                        fechaCoincide = true;
                    }

                    boolean esMembresia = cobro.concepto != null &&
                            cobro.concepto.toLowerCase().contains("membresía");

                    if (fechaCoincide && esMembresia) {
                        yaExiste = true;
                        break;
                    }
                }

                if (yaExiste) {
                    cb.onSuccess(false);
                } else {
                    String tipoAuto = (membresia.tipo != null && !membresia.tipo.isEmpty())
                            ? membresia.tipo.toLowerCase() : "mensual";
                    String concepto = "Membresía " + tipoAuto + " · " + nombreCliente;
                    String notas = "Cobro automático generado el " +
                            new java.text.SimpleDateFormat("dd/MM/yyyy",
                                    java.util.Locale.getDefault()).format(new java.util.Date());

                    crearCobro(
                            membresia.clienteId,
                            nombreCliente,
                            concepto,
                            membresia.precio,
                            "Efectivo",
                            "pendiente",
                            notas,
                            new Callback<CobroModel>() {
                                @Override
                                public void onSuccess(CobroModel data) {
                                    cb.onSuccess(true);
                                }
                                @Override
                                public void onError(String mensaje) {
                                    cb.onError(mensaje);
                                }
                            }
                    );
                }
            }

            @Override
            public void onError(String mensaje) {
                cb.onError(mensaje);
            }
        });
    }

    public void generarCobroCancelacion(MembresiaModel membresia, String nombreCliente,
                                        Callback<CobroModel> cb) {
        String tipoCanc = (membresia.tipo != null && !membresia.tipo.isEmpty())
                ? membresia.tipo.toLowerCase() : "mensual";
        String concepto = "Membresía " + tipoCanc + " (cancelación) · " + nombreCliente;
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy",
                java.util.Locale.getDefault());
        String notas = "Generado automáticamente al cancelar membresía el " +
                sdf.format(new java.util.Date());

        crearCobro(
                membresia.clienteId,
                nombreCliente,
                concepto,
                membresia.precio,
                "Efectivo",
                "pendiente",
                notas,
                cb
        );
    }
}