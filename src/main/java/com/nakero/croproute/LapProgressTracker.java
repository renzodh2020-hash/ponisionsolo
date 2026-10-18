package com.nakero.croproute;

import java.util.List;

/**
 * Contador independiente de Minecraft: una vuelta es una pasada completa
 * por la longitud de la ruta circular guardada, no una distancia arbitraria
 * recorrida por el jugador. Se comprueba además una visita física al punto
 * de referencia con tolerancia configurable.
 */
final class LapProgressTracker {
    private final double[] cumulative;
    private final double totalLength;
    private final double checkpointX;
    private final double checkpointZ;
    private final double radiusSquared;

    private double lastPathPosition;
    private double lastX;
    private double lastZ;
    private double accumulated;
    private boolean checkpointVisited;
    private boolean insideCheckpoint;

    LapProgressTracker(List<RoutePoint> points, double checkpointX, double checkpointZ,
                       double radius, int startSegment, double startT,
                       double startX, double startZ) {
        if (points == null || points.size() < 4 || radius <= 0) {
            throw new IllegalArgumentException("Ruta o tolerancia invalida");
        }
        cumulative = new double[points.size() + 1];
        for (int i = 0; i < points.size(); i++) {
            RoutePoint a = points.get(i);
            RoutePoint b = points.get((i + 1) % points.size());
            cumulative[i + 1] = cumulative[i] + Math.hypot(b.x - a.x, b.z - a.z);
        }
        totalLength = cumulative[points.size()];
        if (totalLength <= 1.0D) {
            throw new IllegalArgumentException("Recorrido demasiado corto");
        }
        this.checkpointX = checkpointX;
        this.checkpointZ = checkpointZ;
        radiusSquared = radius * radius;
        insideCheckpoint = inCheckpoint(startX, startZ);
        checkpointVisited = insideCheckpoint;
        lastX = startX;
        lastZ = startZ;
        lastPathPosition = pathPosition(startSegment, startT);
    }

    double getTotalLength() { return totalLength; }

    private boolean inCheckpoint(double x, double z) {
        double dx = x - checkpointX;
        double dz = z - checkpointZ;
        return dx * dx + dz * dz <= radiusSquared;
    }

    private double pathPosition(int segment, double t) {
        if (segment < 0 || segment >= cumulative.length - 1) {
            throw new IllegalArgumentException("Segmento fuera de la ruta");
        }
        double clampedT = Math.max(0.0D, Math.min(1.0D, t));
        return cumulative[segment]
                + (cumulative[segment + 1] - cumulative[segment]) * clampedT;
    }

    /** Devuelve true únicamente cuando se completó otra vuelta válida. */
    boolean update(int segment, double t, double x, double z) {
        double currentPath = pathPosition(segment, t);
        double movement = Math.hypot(x - lastX, z - lastZ);
        lastX = x;
        lastZ = z;
        if (movement > 5.0D) {
            // Un teletransporte no constituye una vuelta.
            accumulated = 0.0D;
            checkpointVisited = false;
            insideCheckpoint = inCheckpoint(x, z);
            lastPathPosition = currentPath;
            return false;
        }

        boolean nowInside = inCheckpoint(x, z);
        if (nowInside && !insideCheckpoint) {
            checkpointVisited = true;
        }
        insideCheckpoint = nowInside;

        double forward = currentPath - lastPathPosition;
        lastPathPosition = currentPath;
        if (forward < -totalLength * 0.5D) {
            forward += totalLength; // paso del último segmento al primero
        } else if (forward < 0.0D || forward > totalLength * 0.5D) {
            return false; // jitter o salto hacia una sección distante
        }
        // No permitir que un salto en la proyección (sin avanzar físicamente)
        // invente cientos de bloques de recorrido.
        if (forward > Math.max(2.0D, movement * 4.0D + 0.5D)) {
            return false;
        }
        accumulated += forward;
        if (accumulated + 0.001D >= totalLength && checkpointVisited) {
            accumulated = Math.max(0.0D, accumulated - totalLength);
            checkpointVisited = false;
            return true;
        }
        return false;
    }
}
