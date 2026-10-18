package com.nakero.croproute;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class RouteManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR =
            FabricLoader.getInstance().getConfigDir().resolve("crop-route");
    private static final Path ROUTES_FILE = CONFIG_DIR.resolve("routes.json");
    private static final Path TIMER_FILE = CONFIG_DIR.resolve("timer.json");
    private static final Path LAPS_FILE = CONFIG_DIR.resolve("vueltas-p.json");

    private static RouteStore store = new RouteStore();

    private static int routeTimerHours = 3;
    private static int routeTimerMinutes = 0;

    // Configuración independiente de P; valores por defecto del ciclo original.
    private static int rtafkLaps = 40;
    private static int verruga11Laps = 40;
    private static int verruga1Laps = 40;

    public static int getRtafkLaps() { return rtafkLaps; }
    public static int getVerruga11Laps() { return verruga11Laps; }
    public static int getVerruga1Laps() { return verruga1Laps; }

    public static void setSequenceLaps(int rtafk, int verruga11, int verruga1) {
        if (!validLaps(rtafk) || !validLaps(verruga11) || !validLaps(verruga1)) {
            throw new IllegalArgumentException("Las vueltas deben estar entre 1 y 9999");
        }
        rtafkLaps = rtafk;
        verruga11Laps = verruga11;
        verruga1Laps = verruga1;
        saveSequenceLaps();
    }

    private static boolean validLaps(int amount) {
        return amount >= 1 && amount <= 9999;
    }

    private static final class SequenceLapsSettings {
        int RTAFK = 40;
        int verruga11 = 40;
        int verruga1 = 40;
    }

    private static void loadSequenceLaps() {
        rtafkLaps = 40;
        verruga11Laps = 40;
        verruga1Laps = 40;
        if (!Files.exists(LAPS_FILE)) return;
        try (Reader reader = Files.newBufferedReader(LAPS_FILE)) {
            SequenceLapsSettings value = GSON.fromJson(reader, SequenceLapsSettings.class);
            if (value != null && validLaps(value.RTAFK)
                    && validLaps(value.verruga11) && validLaps(value.verruga1)) {
                rtafkLaps = value.RTAFK;
                verruga11Laps = value.verruga11;
                verruga1Laps = value.verruga1;
            }
        } catch (Exception e) {
            System.err.println("[CropRoute] Error leyendo vueltas P: " + e.getMessage());
        }
    }

    private static void saveSequenceLaps() {
        try {
            Files.createDirectories(CONFIG_DIR);
            SequenceLapsSettings value = new SequenceLapsSettings();
            value.RTAFK = rtafkLaps;
            value.verruga11 = verruga11Laps;
            value.verruga1 = verruga1Laps;
            try (Writer writer = Files.newBufferedWriter(LAPS_FILE)) {
                GSON.toJson(value, writer);
            }
        } catch (IOException e) {
            System.err.println("[CropRoute] Error guardando vueltas P: " + e.getMessage());
        }
    }


    public static int getRouteTimerHours() {
        return routeTimerHours;
    }

    public static int getRouteTimerMinutes() {
        return routeTimerMinutes;
    }

    public static void setRouteTimer(int hours, int minutes) {
        // 0:00 deshabilita la duracion; evitamos recorrido O sin limite.
        if (hours < 0 || hours > 999 || minutes < 0 || minutes > 59
                || (hours == 0 && minutes == 0)) {
            throw new IllegalArgumentException("Tiempo invalido (minimo 1 minuto)");
        }
        routeTimerHours = hours;
        routeTimerMinutes = minutes;
        saveTimer();
    }

    private static final class TimerSettings {
        int hours = 3;
        int minutes = 0;
    }

    private static void loadTimer() {
        routeTimerHours = 3;
        routeTimerMinutes = 0;
        if (!Files.exists(TIMER_FILE)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(TIMER_FILE)) {
            TimerSettings value = GSON.fromJson(reader, TimerSettings.class);
            if (value != null && value.hours >= 0 && value.hours <= 999
                    && value.minutes >= 0 && value.minutes <= 59
                    && (value.hours != 0 || value.minutes != 0)) {
                routeTimerHours = value.hours;
                routeTimerMinutes = value.minutes;
            }
        } catch (Exception e) {
            System.err.println("[CropRoute] Error leyendo temporizador: " + e.getMessage());
        }
    }

    private static void saveTimer() {
        try {
            Files.createDirectories(CONFIG_DIR);
            TimerSettings value = new TimerSettings();
            value.hours = routeTimerHours;
            value.minutes = routeTimerMinutes;
            try (Writer writer = Files.newBufferedWriter(TIMER_FILE)) {
                GSON.toJson(value, writer);
            }
        } catch (IOException e) {
            System.err.println("[CropRoute] Error guardando temporizador: " + e.getMessage());
        }
    }

    private RouteManager() {
    }

    public static void load() {
        try {
            Files.createDirectories(CONFIG_DIR);
            loadTimer();
            loadSequenceLaps();

            if (!Files.exists(ROUTES_FILE)) {
                store = new RouteStore();
                save();
                return;
            }

            try (Reader reader = Files.newBufferedReader(ROUTES_FILE)) {
                RouteStore loaded = GSON.fromJson(reader, RouteStore.class);
                store = loaded != null ? loaded : new RouteStore();
            }

            if (store.routes == null) {
                store.routes = new ArrayList<>();
            }

            // Limpieza defensiva por si se editó el JSON manualmente.
            store.routes.removeIf(route ->
                    route == null || route.id == null || route.points == null);

            if (store.selectedRouteId != null && getSelectedRoute() == null) {
                store.selectedRouteId = null;
            }
        } catch (Exception e) {
            System.err.println("[CropRoute] No se pudieron cargar las rutas: " + e.getMessage());
            store = new RouteStore();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_DIR);
            try (Writer writer = Files.newBufferedWriter(ROUTES_FILE)) {
                GSON.toJson(store, writer);
            }
        } catch (IOException e) {
            System.err.println("[CropRoute] No se pudieron guardar las rutas: " + e.getMessage());
        }
    }

    public static List<RouteData> getRoutes() {
        return store.routes;
    }

    public static List<RouteData> getRoutesNewestFirst() {
        List<RouteData> copy = new ArrayList<>(store.routes);
        copy.sort(Comparator.comparingLong((RouteData r) -> r.createdAt).reversed());
        return copy;
    }

    public static RouteData getSelectedRoute() {
        if (store.selectedRouteId == null) {
            return null;
        }

        for (RouteData route : store.routes) {
            if (store.selectedRouteId.equals(route.id)) {
                return route;
            }
        }
        return null;
    }

    public static void select(String routeId) {
        store.selectedRouteId = routeId;
        save();
    }

    public static RouteData addRoute(String requestedName, String dimension, List<RoutePoint> points) {
        String name = makeUniqueName(sanitizeName(requestedName));
        RouteData route = new RouteData(name, dimension, points);
        store.routes.add(route);
        store.selectedRouteId = route.id;
        save();
        return route;
    }

    public static void delete(String routeId) {
        store.routes.removeIf(route -> route.id.equals(routeId));

        if (routeId != null && routeId.equals(store.selectedRouteId)) {
            store.selectedRouteId = store.routes.isEmpty()
                    ? null
                    : store.routes.get(store.routes.size() - 1).id;
        }

        save();
    }

    public static String makeUniqueName(String baseName) {
        String base = sanitizeName(baseName);
        String candidate = base;
        int number = 2;

        while (nameExists(candidate)) {
            candidate = base + " (" + number + ")";
            number++;
        }

        return candidate;
    }

    private static boolean nameExists(String name) {
        for (RouteData route : store.routes) {
            if (route.name != null && route.name.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private static String sanitizeName(String name) {
        if (name == null) {
            return "Ruta";
        }

        String clean = name.trim();
        if (clean.isEmpty()) {
            clean = "Ruta";
        }

        if (clean.length() > 40) {
            clean = clean.substring(0, 40);
        }

        return clean;
    }
}
