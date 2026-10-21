package com.nakero.croproute;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.client.gui.widget.TextFieldWidget;

import java.util.List;

public class RouteSelectorScreen extends Screen {
    private int page;

    public RouteSelectorScreen() {
        this(0);
    }

    private RouteSelectorScreen(int page) {
        super(Text.literal("Rutas guardadas"));
        this.page = Math.max(0, page);
    }

    @Override
    protected void init() {
        List<RouteData> routes = RouteManager.getRoutesNewestFirst();
        // Reducir las filas visibles en resoluciones pequenas, sin ocultar botones.
        int routesPerPage = height >= 390 ? 6 : height >= 330 ? 4 : height >= 280 ? 3 : 2;
        int maxPage = Math.max(0, (routes.size() - 1) / routesPerPage);
        page = Math.min(page, maxPage);

        int panelWidth = Math.min(330, width - 30);
        int x = (width - panelWidth) / 2;
        int startY = height < 280 ? 30 : Math.max(45,
                (height - (routesPerPage * 24 + 115)) / 2);

        int from = page * routesPerPage;
        int to = Math.min(routes.size(), from + routesPerPage);

        for (int i = from; i < to; i++) {
            RouteData route = routes.get(i);
            RouteData selected = RouteManager.getSelectedRoute();
            boolean isSelected = selected != null && selected.id.equals(route.id);

            String label = (isSelected ? "▶ " : "")
                    + route.name
                    + "  [" + route.points.size() + " pts]";

            int row = i - from;

            addDrawableChild(ButtonWidget.builder(
                    Text.literal(label),
                    button -> {
                        RouteManager.select(route.id);
                        CropRouteClient.showActionBar("Ruta seleccionada: " + route.name);
                        if (client != null) {
                            client.setScreen(null);
                        }
                    }
            ).dimensions(x, startY + row * 24, panelWidth, 20).build());
        }

        int bottomY = startY + routesPerPage * 24 + 7;

        if (page > 0) {
            addDrawableChild(ButtonWidget.builder(
                    Text.literal("< Anterior"),
                    button -> {
                        if (client != null) {
                            client.setScreen(new RouteSelectorScreen(page - 1));
                        }
                    }
            ).dimensions(x, bottomY, 90, 20).build());
        }

        if (page < maxPage) {
            addDrawableChild(ButtonWidget.builder(
                    Text.literal("Siguiente >"),
                    button -> {
                        if (client != null) {
                            client.setScreen(new RouteSelectorScreen(page + 1));
                        }
                    }
            ).dimensions(x + panelWidth - 90, bottomY, 90, 20).build());
        }

        RouteData selected = RouteManager.getSelectedRoute();
        boolean canDelete = selected != null;

        ButtonWidget deleteButton = ButtonWidget.builder(
                Text.literal("Eliminar seleccionada"),
                button -> {
                    RouteData current = RouteManager.getSelectedRoute();
                    if (current != null) {
                        String deletedName = current.name;
                        RouteManager.delete(current.id);
                        CropRouteClient.showActionBar("Ruta eliminada: " + deletedName);
                        if (client != null) {
                            client.setScreen(new RouteSelectorScreen(page));
                        }
                    }
                }
        ).dimensions(x + (panelWidth - 150) / 2, bottomY + 27, 150, 20).build();
        deleteButton.active = canDelete;
        addDrawableChild(deleteButton);

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Configuración"),
                button -> {
                    if (client != null) client.setScreen(new ConfigurationScreen());
                }
        ).dimensions(x + (panelWidth - 180) / 2, bottomY + 54, 180, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Cerrar"),
                button -> {
                    if (client != null) client.setScreen(null);
                }
        ).dimensions(x + (panelWidth - 90) / 2, bottomY + 81, 90, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal("Crop Route — elige una ruta"),
                width / 2,
                22,
                0xFFFFFF
        );

        RouteData selected = RouteManager.getSelectedRoute();
        String selectedText = selected == null
                ? "Seleccionada: ninguna"
                : "Seleccionada: " + selected.name
                    + " | ~" + Math.round(selected.approximateLength()) + " bloques";

        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal(selectedText),
                width / 2,
                37,
                0xCFCFCF
        );

        if (RouteManager.getRoutes().isEmpty()) {
            context.drawCenteredTextWithShadow(
                    textRenderer,
                    Text.literal("Todavía no hay rutas. Presiona J en el mundo para grabar una."),
                    width / 2,
                    height / 2 - 5,
                    0xFFFFFF
            );
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private static class ConfigurationScreen extends Screen {
        ConfigurationScreen() { super(Text.literal("Configuración")); }

        @Override
        protected void init() {
            String[] labels = {"Temporizador", "Vueltas P", "Pausas P", "Coordenadas P", "Volver"};
            int top = Math.max(36, (height - 5 * 28) / 2);
            for (int i = 0; i < labels.length; i++) {
                final int option = i;
                addDrawableChild(ButtonWidget.builder(Text.literal(labels[i]), button -> {
                    if (client == null) return;
                    switch (option) {
                        case 0: client.setScreen(new TimerConfigScreen()); break;
                        case 1: client.setScreen(new LapsConfigScreen()); break;
                        case 2: client.setScreen(new PausesConfigScreen()); break;
                        case 3: client.setScreen(new CoordinatesConfigScreen()); break;
                        default: client.setScreen(new RouteSelectorScreen()); break;
                    }
                }).dimensions(width / 2 - 105, top + i * 28, 210, 20).build());
            }
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            renderBackground(context, mouseX, mouseY, delta);
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("Configuración de Crop Route"), width / 2, 17, 0xFFFFFF);
            super.render(context, mouseX, mouseY, delta);
        }
    }

    private static class TimerConfigScreen extends Screen {

        private TextFieldWidget hoursField;
        private TextFieldWidget minutesField;

        protected TimerConfigScreen() {
            super(Text.literal("Temporizador"));
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            renderBackground(context, mouseX, mouseY, delta);
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("Temporizador (solo tecla O)"), width / 2, 35, 0xFFFFFF);
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("Horas"), width / 2, 56, 0xCFCFCF);
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("Minutos"), width / 2, 91, 0xCFCFCF);
            super.render(context, mouseX, mouseY, delta);
        }

        @Override
        protected void init() {

            hoursField = new TextFieldWidget(
                    textRenderer,
                    width / 2 - 60,
                    70,
                    120,
                    20,
                    Text.literal("Horas")
            );

            minutesField = new TextFieldWidget(
                    textRenderer,
                    width / 2 - 60,
                    105,
                    120,
                    20,
                    Text.literal("Minutos")
            );

            hoursField.setText(
                    String.valueOf(RouteManager.getRouteTimerHours())
            );

            minutesField.setText(
                    String.valueOf(RouteManager.getRouteTimerMinutes())
            );

            addDrawableChild(hoursField);
            addDrawableChild(minutesField);

            addDrawableChild(
                    ButtonWidget.builder(
                            Text.literal("Guardar"),
                            button -> {

                                try {
                                    int hours = Integer.parseInt(hoursField.getText());
                                    int minutes = Integer.parseInt(minutesField.getText());

                                    RouteManager.setRouteTimer(hours, minutes);

                                    CropRouteClient.showChatMessage(
                                            "[Crop Route] Temporizador cambiado a "
                                                    + RouteManager.getRouteTimerHours() + " h "
                                                    + RouteManager.getRouteTimerMinutes() + " min. "
                                                    + "Se aplicara al iniciar el recorrido con O."
                                    );

                                } catch (Exception ignored) {
                                    CropRouteClient.showChatMessage(
                                            "[Crop Route] Tiempo invalido. Usa al menos 1 minuto "
                                                    + "(horas 0-999, minutos 0-59)."
                                    );
                                }

                                if (client != null) {
                                    client.setScreen(
                                            new ConfigurationScreen()
                                    );
                                }
                            }
                    ).dimensions(
                            width / 2 - 60,
                            140,
                            120,
                            20
                    ).build()
            );

            addDrawableChild(
                    ButtonWidget.builder(
                            Text.literal("Volver"),
                            button -> {
                                if (client != null) {
                                    client.setScreen(
                                            new ConfigurationScreen()
                                    );
                                }
                            }
                    ).dimensions(
                            width / 2 - 60,
                            170,
                            120,
                            20
                    ).build()
            );
        }
    }

    private static class LapsConfigScreen extends Screen {
        private static final String[] NAMES = {"RTAFK", "verruga11", "verruga1"};
        private final TextFieldWidget[] fields = new TextFieldWidget[3];

        protected LapsConfigScreen() { super(Text.literal("Vueltas P")); }

        @Override
        protected void init() {
            int top = Math.max(55, height / 2 - 77);
            for (int i = 0; i < 3; i++) {
                final int index = i;
                fields[i] = new TextFieldWidget(textRenderer, width / 2 + 12,
                        top + i * 34, 92, 20, Text.literal(NAMES[i]));
                fields[i].setText(String.valueOf(RouteManager.getSequenceLaps(index)));
                fields[i].setMaxLength(6);
                addDrawableChild(fields[i]);
            }
            addDrawableChild(ButtonWidget.builder(Text.literal("Guardar vueltas"), button -> {
                try {
                    int[] newLaps = new int[3];
                    for (int i = 0; i < 3; i++) newLaps[i] = Integer.parseInt(fields[i].getText().trim());
                    RouteManager.setSequenceLaps(newLaps);
                    CropRouteClient.showChatMessage("[Crop Route] Vueltas P guardadas: RTAFK "
                            + newLaps[0] + " | verruga11 " + newLaps[1]
                            + " | verruga1 " + newLaps[2] + ". Se aplicaran al reiniciar P.");
                    if (client != null) client.setScreen(new ConfigurationScreen());
                } catch (Exception e) {
                    CropRouteClient.showChatMessage("[Crop Route] Vueltas invalidas. Usa enteros entre 1 y 100000.");
                }
            }).dimensions(width / 2 - 99, top + 115, 198, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Volver"), button -> {
                if (client != null) client.setScreen(new ConfigurationScreen());
            }).dimensions(width / 2 - 50, top + 142, 100, 20).build());
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            renderBackground(context, mouseX, mouseY, delta);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("Vueltas P (1 a 100000)"),
                    width / 2, 27, 0xFFFFFF);
            int top = Math.max(55, height / 2 - 77);
            for (int i = 0; i < 3; i++) {
                context.drawTextWithShadow(textRenderer, Text.literal(NAMES[i]),
                        width / 2 - 105, top + i * 34 + 6, 0xD0D0D0);
            }
            super.render(context, mouseX, mouseY, delta);
        }
    }

    private static class PausesConfigScreen extends Screen {
        private static final String[] NAMES = {"RTAFK", "verruga11", "verruga1"};
        private final TextFieldWidget[] minFields = new TextFieldWidget[3];
        private final TextFieldWidget[] maxFields = new TextFieldWidget[3];
        private String error = "";

        protected PausesConfigScreen() { super(Text.literal("Pausas P")); }

        private static String asClock(int seconds) {
            return String.format(java.util.Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60);
        }

        // Valores en minutos:segundos. Se permiten de 00:00 a 9999:59.
        private static int parseClock(String value) {
            String[] parts = value.trim().split(":", -1);
            if (parts.length != 2 || !parts[0].matches("[0-9]{1,4}")
                    || !parts[1].matches("[0-9]{1,2}")) {
                throw new IllegalArgumentException("Usa MM:SS");
            }
            int minutes = Integer.parseInt(parts[0]);
            int seconds = Integer.parseInt(parts[1]);
            if (seconds > 59) throw new IllegalArgumentException("Segundos: 0-59");
            return minutes * 60 + seconds;
        }

        @Override
        protected void init() {
            int top = Math.max(69, height / 2 - 70);
            for (int i = 0; i < 3; i++) {
                minFields[i] = new TextFieldWidget(textRenderer, width / 2 - 13,
                        top + i * 30, 69, 20, Text.literal("Minimo"));
                maxFields[i] = new TextFieldWidget(textRenderer, width / 2 + 66,
                        top + i * 30, 69, 20, Text.literal("Maximo"));
                minFields[i].setMaxLength(7);
                maxFields[i].setMaxLength(7);
                minFields[i].setText(asClock(RouteManager.getPauseMinimumSeconds(i)));
                maxFields[i].setText(asClock(RouteManager.getPauseMaximumSeconds(i)));
                addDrawableChild(minFields[i]);
                addDrawableChild(maxFields[i]);
            }
            addDrawableChild(ButtonWidget.builder(Text.literal("Guardar pausas"), button -> {
                try {
                    int[] minimums = new int[3];
                    int[] maximums = new int[3];
                    for (int i = 0; i < 3; i++) {
                        minimums[i] = parseClock(minFields[i].getText());
                        maximums[i] = parseClock(maxFields[i].getText());
                    }
                    RouteManager.setSequencePauses(minimums, maximums);
                    CropRouteClient.showChatMessage("[Crop Route] Pausas P guardadas: RTAFK "
                            + asClock(minimums[0]) + "-" + asClock(maximums[0])
                            + " | verruga11 " + asClock(minimums[1]) + "-" + asClock(maximums[1])
                            + " | verruga1 " + asClock(minimums[2]) + "-" + asClock(maximums[2])
                            + ". Se aplicaran al reiniciar P.");
                    if (client != null) client.setScreen(new ConfigurationScreen());
                } catch (Exception e) {
                    error = "Formato MM:SS; minimo no puede superar maximo.";
                    CropRouteClient.showChatMessage("[Crop Route] " + error);
                }
            }).dimensions(width / 2 - 100, top + 101, 200, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Volver"), button -> {
                if (client != null) client.setScreen(new ConfigurationScreen());
            }).dimensions(width / 2 - 50, top + 128, 100, 20).build());
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            renderBackground(context, mouseX, mouseY, delta);
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("Pausas P (minutos:segundos)"), width / 2, 23, 0xFFFFFF);
            int top = Math.max(69, height / 2 - 70);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("Minimo       Maximo"),
                    width / 2 + 48, top - 17, 0xB0B0B0);
            for (int i = 0; i < 3; i++) {
                context.drawTextWithShadow(textRenderer, Text.literal(NAMES[i]),
                        width / 2 - 122, top + i * 30 + 6, 0xD0D0D0);
            }
            if (!error.isEmpty()) context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal(error), width / 2, top + 160, 0xFF7777);
            super.render(context, mouseX, mouseY, delta);
        }
    }

    private static class CoordinatesConfigScreen extends Screen {
        private static final String[] NAMES = {"RTAFK", "verruga11", "verruga1"};
        private final TextFieldWidget[] xFields = new TextFieldWidget[3];
        private final TextFieldWidget[] zFields = new TextFieldWidget[3];
        private TextFieldWidget radiusField;
        private String error = "";

        CoordinatesConfigScreen() { super(Text.literal("Coordenadas P")); }

        private int formTop() { return Math.max(42, (height - 196) / 2); }

        private static double parseNumber(String raw) {
            return Double.parseDouble(raw.trim().replace(',', '.'));
        }

        @Override
        protected void init() {
            int top = formTop();
            for (int i = 0; i < 3; i++) {
                xFields[i] = new TextFieldWidget(textRenderer, width / 2 - 15,
                        top + i * 27, 80, 20, Text.literal("X"));
                zFields[i] = new TextFieldWidget(textRenderer, width / 2 + 72,
                        top + i * 27, 80, 20, Text.literal("Z"));
                xFields[i].setMaxLength(20);
                zFields[i].setMaxLength(20);
                xFields[i].setText(String.valueOf(RouteManager.getGateX(i)));
                zFields[i].setText(String.valueOf(RouteManager.getGateZ(i)));
                addDrawableChild(xFields[i]);
                addDrawableChild(zFields[i]);
            }
            radiusField = new TextFieldWidget(textRenderer, width / 2 + 40,
                    top + 86, 76, 20, Text.literal("Tolerancia"));
            radiusField.setMaxLength(12);
            radiusField.setText(String.valueOf(RouteManager.getGateRadius()));
            addDrawableChild(radiusField);

            addDrawableChild(ButtonWidget.builder(Text.literal("Guardar coordenadas"), button -> {
                try {
                    double[] xs = new double[3], zs = new double[3];
                    for (int i = 0; i < 3; i++) {
                        xs[i] = parseNumber(xFields[i].getText());
                        zs[i] = parseNumber(zFields[i].getText());
                    }
                    double tolerance = parseNumber(radiusField.getText());
                    RouteManager.setSequenceCoordinates(xs, zs, tolerance);
                    CropRouteClient.showChatMessage("[Crop Route] Coordenadas P y tolerancia guardadas ("
                            + tolerance + " bloques). Se aplicarán al reiniciar P.");
                    if (client != null) client.setScreen(new ConfigurationScreen());
                } catch (Exception e) {
                    error = "Coordenadas invalidas o tolerancia fuera de 0.25 a 8.";
                    CropRouteClient.showChatMessage("[Crop Route] " + error);
                }
            }).dimensions(width / 2 - 105, top + 115, 210, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Volver"), button -> {
                if (client != null) client.setScreen(new ConfigurationScreen());
            }).dimensions(width / 2 - 50, top + 142, 100, 20).build());
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            renderBackground(context, mouseX, mouseY, delta);
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("Coordenadas de vueltas P (ejes X / Z)"), width / 2, 16, 0xFFFFFF);
            int top = formTop();
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("X                   Z"),
                    width / 2 + 65, top - 14, 0xCCCCCC);
            for (int i = 0; i < 3; i++) {
                context.drawTextWithShadow(textRenderer, Text.literal(NAMES[i]),
                        width / 2 - 122, top + i * 27 + 6, 0xEEEEEE);
            }
            context.drawTextWithShadow(textRenderer, Text.literal("Tolerancia:"),
                    width / 2 - 83, top + 92, 0xEEEEEE);
            if (!error.isEmpty()) {
                context.drawCenteredTextWithShadow(textRenderer, Text.literal(error),
                        width / 2, top + 168, 0xFF6666);
            }
            super.render(context, mouseX, mouseY, delta);
        }
    }

}
