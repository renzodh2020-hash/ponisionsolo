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
        int panelWidth = Math.min(330, width - 30);
        int x = (width - panelWidth) / 2;
        int startY = Math.max(50, height / 2 - 92);
        // Reservar espacio para los botones, incluso con una escala de GUI grande.
        int routesPerPage = Math.max(1, Math.min(6, (height - startY - 142) / 24));
        int maxPage = Math.max(0, (routes.size() - 1) / routesPerPage);
        page = Math.min(page, maxPage);

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
                Text.literal("Temporizador"),
                button -> {
                    if (client != null) {
                        client.setScreen(new TimerConfigScreen());
                    }
                }
        ).dimensions(x + (panelWidth - 180) / 2, bottomY + 54, 180, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Vueltas P"),
                button -> {
                    if (client != null) {
                        client.setScreen(new LapsConfigScreen());
                    }
                }
        ).dimensions(x + (panelWidth - 180) / 2, bottomY + 81, 180, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Cerrar"),
                button -> {
                    if (client != null) {
                        client.setScreen(null);
                    }
                }
        ).dimensions(x + (panelWidth - 90) / 2, bottomY + 108, 90, 20).build());
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
                                            new RouteSelectorScreen()
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
                                            new RouteSelectorScreen()
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


    /** Edita el objetivo por ruta de P; no modifica ni el recorrido ni el temporizador de O. */
    private static class LapsConfigScreen extends Screen {
        private TextFieldWidget rtafkField;
        private TextFieldWidget verruga11Field;
        private TextFieldWidget verruga1Field;

        protected LapsConfigScreen() {
            super(Text.literal("Vueltas P"));
        }

        private int top() {
            return Math.max(30, height / 2 - 108);
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            renderBackground(context, mouseX, mouseY, delta);
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("Vueltas por ruta (solo tecla P)"), width / 2, top() - 15, 0xFFFFFF);
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("RTAFK"), width / 2, top() + 2, 0xCFCFCF);
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("verruga11"), width / 2, top() + 52, 0xCFCFCF);
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("verruga1"), width / 2, top() + 102, 0xCFCFCF);
            super.render(context, mouseX, mouseY, delta);
        }

        @Override
        protected void init() {
            int y = top();
            rtafkField = new TextFieldWidget(textRenderer, width / 2 - 60, y + 17,
                    120, 20, Text.literal("RTAFK"));
            verruga11Field = new TextFieldWidget(textRenderer, width / 2 - 60, y + 67,
                    120, 20, Text.literal("verruga11"));
            verruga1Field = new TextFieldWidget(textRenderer, width / 2 - 60, y + 117,
                    120, 20, Text.literal("verruga1"));
            for (TextFieldWidget field : new TextFieldWidget[] {
                    rtafkField, verruga11Field, verruga1Field }) {
                field.setMaxLength(4);
                field.setTextPredicate(value -> value.matches("[0-9]*"));
                addDrawableChild(field);
            }
            rtafkField.setText(String.valueOf(RouteManager.getRtafkLaps()));
            verruga11Field.setText(String.valueOf(RouteManager.getVerruga11Laps()));
            verruga1Field.setText(String.valueOf(RouteManager.getVerruga1Laps()));

            addDrawableChild(ButtonWidget.builder(Text.literal("Guardar vueltas"), button -> {
                try {
                    int rtafk = Integer.parseInt(rtafkField.getText());
                    int verruga11 = Integer.parseInt(verruga11Field.getText());
                    int verruga1 = Integer.parseInt(verruga1Field.getText());
                    RouteManager.setSequenceLaps(rtafk, verruga11, verruga1);
                    CropRouteClient.showChatMessage("[Crop Route] Vueltas P guardadas: RTAFK "
                            + rtafk + ", verruga11 " + verruga11 + ", verruga1 " + verruga1
                            + ". Aplican al iniciar P de nuevo.");
                    if (client != null) client.setScreen(new RouteSelectorScreen());
                } catch (IllegalArgumentException e) {
                    CropRouteClient.showChatMessage(
                            "[Crop Route] Vueltas invalidas. Usa valores entre 1 y 9999.");
                }
            }).dimensions(width / 2 - 75, y + 155, 150, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Volver"), button -> {
                if (client != null) client.setScreen(new RouteSelectorScreen());
            }).dimensions(width / 2 - 60, y + 182, 120, 20).build());
        }
    }

}
