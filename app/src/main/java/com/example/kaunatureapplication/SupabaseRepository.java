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

    /** Obtiene todos los cobros asociados a una cita concreta */
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
                        // Dummy con datos reales para que la UI muestre bien el importe
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

        // 1. Actualizar cobro
        Map<String, Object> body = new HashMap<>();
        body.put("estado", "cobrado");

        api.actualizarCobro("eq." + cobroId, body)
                .enqueue(new retrofit2.Callback<List<CobroModel>>() {
                    public void onResponse(Call<List<CobroModel>> call, Response<List<CobroModel>> r) {
                        if (r.isSuccessful()) {
                            android.util.Log.d("SYNC", "✓ Cobro actualizado a cobrado");

                            // 2. Si tiene cita asociada, actualizarla también
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
                                // No tiene cita asociada, solo actualizamos cobro
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

        // 1. Actualizar cobro
        Map<String, Object> body = new HashMap<>();
        body.put("estado", "pendiente");

        api.actualizarCobro("eq." + cobroId, body)
                .enqueue(new retrofit2.Callback<List<CobroModel>>() {
                    public void onResponse(Call<List<CobroModel>> call, Response<List<CobroModel>> r) {
                        if (r.isSuccessful()) {
                            android.util.Log.d("SYNC", "✓ Cobro actualizado a pendiente");

                            // 2. Si tiene cita asociada, actualizarla también
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
     * Marca una cita como COBRADA y sincroniza el cobro asociado (si existe)
     * FLUJO: Usuario marca cita como cobrada → Cobro pasa a "cobrado"
     */
    public void marcarCitaCobradaConSync(String citaId, Callback<Void> cb) {
        android.util.Log.d("SYNC", "marcarCitaCobrada: cita=" + citaId);

        // 1. Actualizar cita
        Map<String, Object> citaBody = new HashMap<>();
        citaBody.put("estado", "cobrada");

        api.actualizarCita("eq." + citaId, citaBody)
                .enqueue(new retrofit2.Callback<List<CitaModel>>() {
                    public void onResponse(Call<List<CitaModel>> call, Response<List<CitaModel>> r) {
                        if (r.isSuccessful()) {
                            android.util.Log.d("SYNC", "✓ Cita actualizada a cobrada");

                            // 2. Buscar cobro asociado y actualizarlo
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
                                    cb.onSuccess(null); // Cita ya está actualizada
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
     * FLUJO: Usuario cambia cita a confirmada/pendiente → Cobro pasa a "pendiente"
     */
    public void cambiarEstadoCitaConSync(String citaId, String nuevoEstado, Callback<Void> cb) {
        android.util.Log.d("SYNC", "cambiarEstadoCita: cita=" + citaId + ", estado=" + nuevoEstado);

        // 1. Actualizar cita
        Map<String, Object> citaBody = new HashMap<>();
        citaBody.put("estado", nuevoEstado);

        api.actualizarCita("eq." + citaId, citaBody)
                .enqueue(new retrofit2.Callback<List<CitaModel>>() {
                    public void onResponse(Call<List<CitaModel>> call, Response<List<CitaModel>> r) {
                        if (r.isSuccessful()) {
                            android.util.Log.d("SYNC", "✓ Cita actualizada a " + nuevoEstado);

                            // 2. Buscar cobro asociado y actualizar según el estado de la cita
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
                                                // Para cancelada, dejamos el cobro como pendiente
                                                // (el usuario puede eliminarlo manualmente si quiere)
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
                                                            cb.onSuccess(null); // Cita ya está actualizada
                                                        }
                                                    }
                                                    public void onFailure(Call<List<CobroModel>> call2, Throwable t) {
                                                        android.util.Log.w("SYNC", "⚠ Error al actualizar cobro");
                                                        cb.onSuccess(null); // Cita ya está actualizada
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
                                    cb.onSuccess(null); // Cita ya está actualizada
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

    /**
     * Obtiene todos los cobros de un cliente específico.
     * Útil para verificar si ya existe un cobro de membresía del mes actual.
     */
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

    /**
     * Genera automáticamente el cobro mensual de una membresía si no existe ya
     * para el mes actual.
     *
     * LÓGICA:
     * 1. Verifica si ya existe un cobro de membresía para este cliente en el mes actual
     * 2. Si NO existe, crea un cobro "pendiente" por el importe de la membresía
     * 3. Si ya existe, no hace nada
     *
     * @param membresia La membresía activa del cliente
     * @param nombreCliente El nombre completo del cliente
     * @param cb Callback que retorna TRUE si se generó un nuevo cobro, FALSE si ya existía
     */
    public void generarCobroMensualSiProcede(MembresiaModel membresia, String nombreCliente,
                                             Callback<Boolean> cb) {
        if (membresia == null || !membresia.activa || membresia.clienteId == null) {
            cb.onSuccess(false);
            return;
        }

        // Obtener el mes actual en formato yyyy-MM
        java.text.SimpleDateFormat sdfMes = new java.text.SimpleDateFormat("yyyy-MM",
                java.util.Locale.getDefault());
        final String mesActual = sdfMes.format(new java.util.Date());

        // Verificar si ya existe un cobro de membresía este mes para este cliente
        getCobrosPorCliente(membresia.clienteId, new Callback<List<CobroModel>>() {
            @Override
            public void onSuccess(List<CobroModel> cobros) {
                boolean yaExiste = false;

                for (CobroModel cobro : cobros) {
                    // Verificar si es un cobro del mes actual
                    boolean fechaCoincide = false;
                    if (cobro.fecha != null && cobro.fecha.startsWith(mesActual)) {
                        fechaCoincide = true;
                    } else if (cobro.createdAt != null && cobro.createdAt.startsWith(mesActual)) {
                        fechaCoincide = true;
                    }

                    // Detectar cualquier cobro de membresía de este cliente en el mes,
                    // incluye cobros normales Y cobros de cancelación
                    boolean esMembresia = cobro.concepto != null &&
                            cobro.concepto.toLowerCase().contains("membresía");

                    if (fechaCoincide && esMembresia) {
                        yaExiste = true;
                        break;
                    }
                }

                if (yaExiste) {
                    // Ya existe cobro de este mes, no generar otro
                    cb.onSuccess(false);
                } else {
                    // No existe, generar cobro pendiente
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
                                    cb.onSuccess(true); // Cobro generado
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

    /**
     * Genera el cobro del mes actual cuando se cancela una membresía.
     * Este cobro queda como "pendiente" y debe ser cobrado aunque la membresía
     * se haya cancelado.
     */
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