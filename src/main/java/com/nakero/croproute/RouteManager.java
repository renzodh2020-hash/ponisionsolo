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
    private static final Path P_SETTINGS_FILE = CONFIG_DIR.resolve("p-settings.json");

    // orden fijo: RTAFK, verruga11, verruga1
    private static PSettings pSettings = new PSettings();
    private static final class PSettings {
        int[] laps = {40, 40, 40};
        int[] minSeconds = {342, 342, 1242};
        int[] maxSeconds = {378, 378, 1278};
        // X / Z de los puntos de conteo. Compartir punto entre rutas esta permitido.
        double[] gateX = {5237.0D, 5237.0D, 5292.0D};
        double[] gateZ = {4621.0D, 4621.0D, 4796.0D};
        double gateRadius = 2.0D;
    }

    private static boolean validPSettings(PSettings settings) {
        if (settings == null || settings.laps == null || settings.minSeconds == null
                || settings.maxSeconds == null || settings.laps.length != 3
                || settings.minSeconds.length != 3 || settings.maxSeconds.length != 3
                || settings.gateX == null || settings.gateX.length != 3
                || settings.gateZ == null || settings.gateZ.length != 3
                || !Double.isFinite(settings.gateRadius)
                || settings.gateRadius < 0.25D || settings.gateRadius > 8.0D) return false;
        for (int i = 0; i < 3; i++) {
            if (!Double.isFinite(settings.gateX[i]) || !Double.isFinite(settings.gateZ[i])
                    || Math.abs(settings.gateX[i]) > 30000000D
                    || Math.abs(settings.gateZ[i]) > 30000000D) return false;
            if (settings.laps[i] < 1 || settings.laps[i] > 100000
                    || settings.minSeconds[i] < 0 || settings.maxSeconds[i] < 0
                    || settings.minSeconds[i] > settings.maxSeconds[i]
                    || settings.maxSeconds[i] > 599999) return false;
        }
        return true;
    }

    public static int getSequenceLaps(int section) {
        return pSettings.laps[section];
    }
    public static int getPauseMinimumSeconds(int section) {
        return pSettings.minSeconds[section];
    }
    public static int getPauseMaximumSeconds(int section) {
        return pSettings.maxSeconds[section];
    }
    public static double getGateX(int section) { return pSettings.gateX[section]; }
    public static double getGateZ(int section) { return pSettings.gateZ[section]; }
    public static double getGateRadius() { return pSettings.gateRadius; }

    private static PSettings copyPSettings() {
        PSettings result = new PSettings();
        result.laps = pSettings.laps.clone();
        result.minSeconds = pSettings.minSeconds.clone();
        result.maxSeconds = pSettings.maxSeconds.clone();
        result.gateX = pSettings.gateX.clone();
        result.gateZ = pSettings.gateZ.clone();
        result.gateRadius = pSettings.gateRadius;
        return result;
    }

    public static void setSequenceCoordinates(double[] xs, double[] zs, double radius) {
        if (xs == null || zs == null || xs.length != 3 || zs.length != 3)
            throw new IllegalArgumentException("Se requieren las 3 rutas");
        PSettings updated = copyPSettings();
        updated.gateX = xs.clone();
        updated.gateZ = zs.clone();
        updated.gateRadius = radius;
        if (!validPSettings(updated))
            throw new IllegalArgumentException("Coordenadas o tolerancia invalidas");
        pSettings = updated;
        savePSettings();
    }

    public static void setSequenceLaps(int[] laps) {
        if (laps == null || laps.length != 3) throw new IllegalArgumentException("Se requieren 3 rutas");
        PSettings updated = copyPSettings();
        updated.laps = laps.clone();
        if (!validPSettings(updated)) throw new IllegalArgumentException("Vueltas: 1 a 100000");
        pSettings = updated;
        savePSettings();
    }
    public static void setSequencePauses(int[] minimums, int[] maximums) {
        if (minimums == null || maximums == null || minimums.length != 3
                || maximums.length != 3) throw new IllegalArgumentException("Se requieren 3 rutas");
        PSettings updated = copyPSettings();
        updated.minSeconds = minimums.clone();
        updated.maxSeconds = maximums.clone();
        if (!validPSettings(updated)) throw new IllegalArgumentException("Intervalos invalidos");
        pSettings = updated;
        savePSettings();
    }
    private static void loadPSettings() {
        pSettings = new PSettings();
        if (!Files.exists(P_SETTINGS_FILE)) return;
        try (Reader reader = Files.newBufferedReader(P_SETTINGS_FILE)) {
            PSettings loaded = GSON.fromJson(reader, PSettings.class);
            if (loaded != null) {
                // Migrar p-settings.json de la version 1.3.0 sin perder vueltas ni pausas.
                if (loaded.gateX == null) loaded.gateX = new double[] {5237, 5237, 5292};
                if (loaded.gateZ == null) loaded.gateZ = new double[] {4621, 4621, 4796};
                if (loaded.gateRadius == 0.0D) loaded.gateRadius = 2.0D;
                if (validPSettings(loaded)) pSettings = loaded;
            }
        } catch (Exception e) {
            System.err.println("[CropRoute] No se pudo leer p-settings.json: " + e.getMessage());
        }
    }
    private static void savePSettings() {
        try {
            Files.createDirectories(CONFIG_DIR);
            try (Writer writer = Files.newBufferedWriter(P_SETTINGS_FILE)) {
                GSON.toJson(pSettings, writer);
            }
        } catch (IOException e) {
            System.err.println("[CropRoute] No se pudo guardar p-settings.json: " + e.getMessage());
        }
    }


    private static RouteStore store = new RouteStore();

    private static int routeTimerHours = 3;
    private static int routeTimerMinutes = 0;

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
            loadPSettings();

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
