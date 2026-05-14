/** 
 * @author: Saadat Emilbekova
 * @gmail: saadat.universe@gmail.com
 * @date: 2026-05-14
 */


package com.budgetapp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

public class PrimaryController {

    @FXML private TextField nameField, descField, costField;
    @FXML private ComboBox<Importance> importancePicker;
    @FXML private TreeView<Section> sectionListView;
    @FXML private javafx.scene.layout.Pane graphCanvas;

    // ── Budget tracker UI (injected from FXML) ────────────────────────────────
    @FXML private TextField budgetField;
    @FXML private javafx.scene.control.Label deficitLabel;
    @FXML private javafx.scene.control.Label houseProgressLabel;
    @FXML private javafx.scene.layout.StackPane houseProgressBarOuter;
    @FXML private javafx.scene.layout.Region houseProgressBarFill;

    private double monthlyBudget = 0.0;
    private double houseSavings  = 0.0;
    private static final double HOUSE_GOAL = 350_000.0;

    private final List<GraphNodeVisual> visibleGraphNodes = new ArrayList<>();
    private final List<javafx.scene.shape.Line> visibleGraphLinks = new ArrayList<>();
    private javafx.animation.AnimationTimer physicsEngine;

    private final Map<TreeItem<Section>, double[]> nodePositions = new HashMap<>();
    private Section rootSection = new Section("Root", "Master", Importance.HIGH);
    boolean syncingFromGraph = false;

    @FXML
    public void initialize() {
        importancePicker.getItems().setAll(Importance.values());

        TreeItem<Section> treeRoot = new TreeItem<>(rootSection);
        sectionListView.setRoot(treeRoot);
        sectionListView.setShowRoot(false);

        sectionListView.getRoot().addEventHandler(TreeItem.branchExpandedEvent(),  e -> { if (!syncingFromGraph) redrawGraphLayout(); });
        sectionListView.getRoot().addEventHandler(TreeItem.branchCollapsedEvent(), e -> { if (!syncingFromGraph) redrawGraphLayout(); });

        // ── Budget field listener ─────────────────────────────────────────────
        budgetField.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                monthlyBudget = newVal.isEmpty() ? 0 : Double.parseDouble(newVal);
            } catch (NumberFormatException e) {
                monthlyBudget = 0;
            }
            refreshBudgetStatus();
        });

        sectionListView.setCellFactory(tv -> new TreeCell<Section>() {
            private final javafx.scene.layout.HBox contentBox = new javafx.scene.layout.HBox(10);
            private final javafx.scene.control.Label textLabel   = new javafx.scene.control.Label();
            private final javafx.scene.layout.HBox  buttonBox    = new javafx.scene.layout.HBox(5);
            private final javafx.scene.control.Button btnDeleteSingle = new javafx.scene.control.Button("🗑");
            private final javafx.scene.control.Button btnDeleteAll    = new javafx.scene.control.Button("💥");

            {
                contentBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                javafx.scene.layout.HBox.setHgrow(textLabel, javafx.scene.layout.Priority.ALWAYS);
                contentBox.getChildren().addAll(textLabel, buttonBox);

                String btnStyle = "-fx-padding: 2 6 2 6; -fx-font-size: 11; -fx-cursor: hand;";
                btnDeleteSingle.setStyle(btnStyle);
                btnDeleteAll.setStyle(btnStyle);
                buttonBox.visibleProperty().bind(this.hoverProperty());

                btnDeleteSingle.setOnAction(event -> {
                    TreeItem<Section> cur = getTreeItem();
                    Section sec = getItem();
                    if (cur == null || sec == null) return;

                    Alert a = new Alert(Alert.AlertType.CONFIRMATION);
                    a.setTitle("Confirm Deletion");
                    a.setHeaderText("Delete: " + sec.getName());
                    a.setContentText(cur.getChildren().isEmpty()
                        ? "Are you sure you want to delete this item?"
                        : "Children will be moved up one level. Proceed?");

                    a.showAndWait().ifPresent(r -> {
                        if (r == ButtonType.OK) {
                            TreeItem<Section> parent = cur.getParent();
                            if (parent != null) {
                                Section ps = parent.getValue();
                                for (TreeItem<Section> c : cur.getChildren()) ps.addSubsection(c.getValue());
                                ps.removeSubsection(sec);
                                List<TreeItem<Section>> toMove = new ArrayList<>(cur.getChildren());
                                int idx = parent.getChildren().indexOf(cur);
                                parent.getChildren().addAll(idx, toMove);
                                parent.getChildren().remove(cur);
                                nodePositions.remove(cur);
                                sectionListView.refresh();
                                redrawGraphLayout();
                                refreshBudgetStatus();
                            }
                        }
                    });
                });

                btnDeleteAll.setOnAction(event -> {
                    TreeItem<Section> cur = getTreeItem();
                    Section sec = getItem();
                    if (cur == null || sec == null) return;

                    Alert a = new Alert(Alert.AlertType.CONFIRMATION);
                    a.setTitle("Confirm Cascade Deletion");
                    a.setHeaderText("Delete Everything Under: " + sec.getName());
                    a.setContentText("This will permanently delete this item and ALL sub-sections!");

                    a.showAndWait().ifPresent(r -> {
                        if (r == ButtonType.OK) {
                            TreeItem<Section> parent = cur.getParent();
                            if (parent != null) {
                                parent.getValue().removeSubsection(sec);
                                parent.getChildren().remove(cur);
                                removePositionsRecursively(cur);
                                sectionListView.refresh();
                                redrawGraphLayout();
                                refreshBudgetStatus();
                            }
                        }
                    });
                });
            }

            @Override
            protected void updateItem(Section item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }

                textLabel.setText(item.getName() + " - Total: $" + item.getGrandTotal()
                    + " [Base: $" + item.getbase_cost() + "]");

                // ── Red highlight when budget is over-spent (importance-aware) ─
                boolean underfunded = isUnderfunded(item);
                if (underfunded) {
                    textLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-weight: bold;");
                    contentBox.setStyle("-fx-background-color: rgba(244,63,94,0.08); -fx-background-radius: 4;");
                } else {
                    textLabel.setStyle("-fx-text-fill: #e0e0e0;");
                    contentBox.setStyle("");
                }

                buttonBox.getChildren().clear();
                if (getTreeItem().getChildren().isEmpty()) {
                    buttonBox.getChildren().add(btnDeleteSingle);
                } else {
                    buttonBox.getChildren().addAll(btnDeleteSingle, btnDeleteAll);
                }
                setGraphic(contentBox);
                setText(null);
            }
        });

        initPhysicsEngine();
        refreshBudgetStatus();
    }

    // ── Budget logic ──────────────────────────────────────────────────────────

    /** Total cost of all top-level sections (grand total of the budget) */
    private double totalExpenses() {
        double total = 0;
        if (sectionListView.getRoot() == null) return 0;
        for (TreeItem<Section> item : sectionListView.getRoot().getChildren()) {
            total += item.getValue().getGrandTotal();
        }
        return total;
    }

    /**
     * A section is "underfunded" when the user's budget can't cover it
     * after paying for higher-importance sections first.
     * Priority: HIGH first, then MEDIUM, then LOW.
     */
    private boolean isUnderfunded(Section section) {
        if (monthlyBudget <= 0) return false;
        double remaining = monthlyBudget;

        // Deduct HIGH sections first
        remaining -= sumByImportance(Importance.HIGH);
        if (section.getImportance() == Importance.HIGH) {
            return remaining < 0;
        }

        // Deduct MEDIUM
        remaining -= sumByImportance(Importance.MEDIUM);
        if (section.getImportance() == Importance.MEDIUM) {
            return remaining < 0;
        }

        // LOW
        remaining -= sumByImportance(Importance.LOW);
        return remaining < 0;
    }

    private double sumByImportance(Importance imp) {
        if (sectionListView.getRoot() == null) return 0;
        double sum = 0;
        for (TreeItem<Section> item : sectionListView.getRoot().getChildren()) {
            if (item.getValue().getImportance() == imp) sum += item.getValue().getGrandTotal();
        }
        return sum;
    }

    /** Refreshes deficit label, tree colours, progress bar, and graph pulses */
    void refreshBudgetStatus() {
        double expenses = totalExpenses();
        double leftover = monthlyBudget - expenses;

        // ── Deficit label ─────────────────────────────────────────────────────
        if (deficitLabel != null) {
            if (monthlyBudget <= 0) {
                deficitLabel.setText("");
            } else if (leftover >= 0) {
                deficitLabel.setText(String.format("✅  $%.0f left over this month", leftover));
                deficitLabel.setStyle("-fx-text-fill: #34d399; -fx-font-size: 12; -fx-font-family: 'Consolas';");
                // Add leftover to house savings
                houseSavings += leftover;
                if (houseSavings >= HOUSE_GOAL) houseSavings -= HOUSE_GOAL; // reset after purchase
            } else {
                deficitLabel.setText(String.format("❌  $%.0f still needed to cover all expenses", -leftover));
                deficitLabel.setStyle("-fx-text-fill: #f43f5e; -fx-font-size: 12; -fx-font-family: 'Consolas';");
            }
        }

        // ── House progress bar ────────────────────────────────────────────────
        if (houseProgressBarFill != null && houseProgressBarOuter != null) {
            double pct = Math.min(houseSavings / HOUSE_GOAL, 1.0);
            houseProgressBarOuter.widthProperty().addListener((obs, o, n) ->
                houseProgressBarFill.setPrefWidth(n.doubleValue() * pct));
            houseProgressBarFill.setPrefWidth(houseProgressBarOuter.getWidth() * pct);

            if (houseProgressLabel != null) {
                houseProgressLabel.setText(String.format("🏠  House savings: $%.0f / $%.0f  (%.1f%%)",
                    houseSavings, HOUSE_GOAL, pct * 100));
            }
        }

        // Refresh tree cell colours
        sectionListView.refresh();

        // Update graph node pulse states
        for (GraphNodeVisual node : visibleGraphNodes) {
            node.setPulse(isUnderfunded(node.associatedTreeItem.getValue()));
        }
    }

    private void removePositionsRecursively(TreeItem<Section> item) {
        nodePositions.remove(item);
        for (TreeItem<Section> child : item.getChildren()) removePositionsRecursively(child);
    }

    @FXML
    private void handleAddSection() {
        Section s = createSectionFromFields();
        if (s == null) return;
        rootSection.addSubsection(s);
        sectionListView.getRoot().getChildren().add(new TreeItem<>(s));
        clearFields();
        redrawGraphLayout();
        refreshBudgetStatus();
    }

    @FXML
    private void handleAddSubsection() {
        TreeItem<Section> selected = sectionListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert a = new Alert(Alert.AlertType.WARNING);
            a.setTitle("No Selection"); a.setHeaderText(null);
            a.setContentText("Please select a parent item in the tree first!");
            a.showAndWait(); return;
        }
        Section s = createSectionFromFields();
        if (s == null) return;
        selected.getValue().addSubsection(s);
        selected.getChildren().add(new TreeItem<>(s));
        selected.setExpanded(true);
        sectionListView.refresh();
        clearFields();
        redrawGraphLayout();
        refreshBudgetStatus();
    }

    private Section createSectionFromFields() {
        String name    = nameField.getText();
        String desc    = descField.getText();
        Importance imp = importancePicker.getValue();
        String costRaw = costField.getText();
        if (name.isEmpty() || imp == null) return null;
        try {
            double cost = costRaw.isEmpty() ? 0 : Double.parseDouble(costRaw);
            return new Section(name, desc, cost, imp);
        } catch (NumberFormatException e) { return null; }
    }

    // ── Physics engine ────────────────────────────────────────────────────────
    private void initPhysicsEngine() {
        physicsEngine = new javafx.animation.AnimationTimer() {
            @Override
            public void handle(long now) {
                double repulsion = 6000.0, spring = 0.04, damping = 0.85;

                for (int i = 0; i < visibleGraphNodes.size(); i++) {
                    GraphNodeVisual a = visibleGraphNodes.get(i);
                    for (int j = i + 1; j < visibleGraphNodes.size(); j++) {
                        GraphNodeVisual b = visibleGraphNodes.get(j);
                        double dx = a.getLayoutX() - b.getLayoutX();
                        double dy = a.getLayoutY() - b.getLayoutY();
                        double dist = Math.hypot(dx, dy);
                        if (dist < 1) dist = 1;
                        if (dist < 350) {
                            double f = repulsion / (dist * dist);
                            double fx = (dx / dist) * f, fy = (dy / dist) * f;
                            if (!a.isBeingDragged) { a.vx += fx; a.vy += fy; }
                            if (!b.isBeingDragged) { b.vx -= fx; b.vy -= fy; }
                        }
                    }
                }

                for (javafx.scene.shape.Line line : visibleGraphLinks) {
                    GraphNodeVisual[] pair = (GraphNodeVisual[]) line.getUserData();
                    if (pair == null) continue;
                    GraphNodeVisual p = pair[0], c = pair[1];
                    if (p == null || c == null) continue;
                    double dx = p.getLayoutX() - c.getLayoutX();
                    double dy = p.getLayoutY() - c.getLayoutY();
                    double dist = Math.hypot(dx, dy);
                    if (dist < 1) dist = 1;
                    double sf = spring * (dist - 120.0);
                    double fx = (dx / dist) * sf, fy = (dy / dist) * sf;
                    if (!p.isBeingDragged) { p.vx -= fx; p.vy -= fy; }
                    if (!c.isBeingDragged) { c.vx += fx; c.vy += fy; }
                }

                for (GraphNodeVisual node : visibleGraphNodes) {
                    if (node.isBeingDragged) {
                        nodePositions.put(node.associatedTreeItem, new double[]{node.getLayoutX(), node.getLayoutY()});
                        continue;
                    }

                    // ── Z-order: push child below parent if it drifts above ───
                    enforceChildBelowParent(node);

                    node.vx *= damping; node.vy *= damping;
                    double nx = Math.max(40, Math.min(graphCanvas.getWidth()  - 40, node.getLayoutX() + node.vx));
                    double ny = Math.max(40, Math.min(graphCanvas.getHeight() - 40, node.getLayoutY() + node.vy));
                    node.setLayoutX(nx); node.setLayoutY(ny);
                    nodePositions.put(node.associatedTreeItem, new double[]{nx, ny});
                }

                updateLinkLinePositions();

                // ── Enforce Z-order: parents always rendered on top ───────────
                enforceZOrder();
            }
        };
        physicsEngine.start();
    }

    /**
     * If a child node's Y is above (less than) its parent's Y, apply a gentle
     * downward nudge so children tend to stay below their parent.
     */
    private void enforceChildBelowParent(GraphNodeVisual child) {
        TreeItem<Section> parentItem = child.associatedTreeItem.getParent();
        if (parentItem == null || parentItem == sectionListView.getRoot()) return;

        for (GraphNodeVisual candidate : visibleGraphNodes) {
            if (candidate.associatedTreeItem == parentItem) {
                double parentCenterY = candidate.getLayoutY() + candidate.circle.getRadius();
                double childCenterY  = child.getLayoutY()    + child.circle.getRadius();
                if (childCenterY < parentCenterY + 30) {
                    // Nudge child downward
                    child.vy += 1.5;
                }
                break;
            }
        }
    }

    /** Bring parent nodes to front so they always render above their children */
    private void enforceZOrder() {
        // Push all nodes to back first, then bring parents to front in depth order
        for (GraphNodeVisual node : visibleGraphNodes) {
            node.toBack();
        }
        // Bring nodes with no visible parent (root-level) to front last = they appear on top
        for (GraphNodeVisual node : visibleGraphNodes) {
            TreeItem<Section> parentItem = node.associatedTreeItem.getParent();
            boolean parentVisible = false;
            if (parentItem != null && parentItem != sectionListView.getRoot()) {
                for (GraphNodeVisual candidate : visibleGraphNodes) {
                    if (candidate.associatedTreeItem == parentItem) { parentVisible = true; break; }
                }
            }
            if (!parentVisible) node.toFront();
        }
    }

    // ── Graph drawing ─────────────────────────────────────────────────────────
    void redrawGraphLayout() {
        graphCanvas.getChildren().clear();
        visibleGraphNodes.clear();
        visibleGraphLinks.clear();

        if (sectionListView.getRoot() != null) {
            for (TreeItem<Section> item : sectionListView.getRoot().getChildren()) {
                createVisualNodeRecursively(item, null, 0);
            }
        }

        graphCanvas.getChildren().addAll(visibleGraphLinks);
        graphCanvas.getChildren().addAll(visibleGraphNodes);

        // Apply pulse state immediately after draw
        for (GraphNodeVisual node : visibleGraphNodes) {
            node.setPulse(isUnderfunded(node.associatedTreeItem.getValue()));
        }
    }

    private void createVisualNodeRecursively(TreeItem<Section> currentItem, GraphNodeVisual visualParent, int depth) {
        if (currentItem == null || currentItem.getValue() == null) return;

        GraphNodeVisual visualNode = new GraphNodeVisual(currentItem, this);

        if (nodePositions.containsKey(currentItem)) {
            double[] pos = nodePositions.get(currentItem);
            visualNode.setLayoutX(pos[0]);
            visualNode.setLayoutY(pos[1]);
        } else if (visualParent != null) {
            double angle = Math.random() * 2 * Math.PI;
            double dist  = 100 + Math.random() * 40;
            visualNode.setLayoutX(visualParent.getLayoutX() + Math.cos(angle) * dist);
            visualNode.setLayoutY(visualParent.getLayoutY() + Math.sin(angle) * dist + 60);
        } else {
            visualNode.setLayoutX(150 + Math.random() * 100);
            visualNode.setLayoutY(60 + depth * 80 + Math.random() * 30);
        }

        if (visualParent != null) {
            javafx.scene.shape.Line link = new javafx.scene.shape.Line();
            link.setStroke(javafx.scene.paint.Color.web("#7c3aed", 0.5));
            link.setStrokeWidth(1.5);
            javafx.scene.effect.DropShadow lg = new javafx.scene.effect.DropShadow();
            lg.setColor(javafx.scene.paint.Color.web("#7c3aed", 0.55));
            lg.setRadius(7); lg.setSpread(0.05);
            link.setEffect(lg);
            link.setUserData(new GraphNodeVisual[]{visualParent, visualNode});
            visibleGraphLinks.add(link);
        }

        visibleGraphNodes.add(visualNode);

        if (currentItem.isExpanded()) {
            for (TreeItem<Section> child : currentItem.getChildren()) {
                createVisualNodeRecursively(child, visualNode, depth + 1);
            }
        }
    }

    private void updateLinkLinePositions() {
        int idx = 0;
        if (sectionListView.getRoot() == null) return;

        for (GraphNodeVisual target : visibleGraphNodes) {
            TreeItem<Section> parentItem = target.associatedTreeItem.getParent();
            if (parentItem == null || parentItem == sectionListView.getRoot()) continue;

            for (GraphNodeVisual candidate : visibleGraphNodes) {
                if (candidate.associatedTreeItem == parentItem) {
                    if (idx >= visibleGraphLinks.size()) break;
                    javafx.scene.shape.Line line = visibleGraphLinks.get(idx);

                    double r1 = candidate.circle.getRadius();
                    double r2 = target.circle.getRadius();
                    double x1 = candidate.getLayoutX() + r1;
                    double y1 = candidate.getLayoutY() + r1;
                    double x2 = target.getLayoutX()    + r2;
                    double y2 = target.getLayoutY()    + r2;

                    double dx = x2 - x1, dy = y2 - y1;
                    double dist = Math.hypot(dx, dy);
                    if (dist < 1) dist = 1;

                    line.setStartX(x1 + (dx / dist) * r1);
                    line.setStartY(y1 + (dy / dist) * r1);
                    line.setEndX  (x2 - (dx / dist) * r2);
                    line.setEndY  (y2 - (dy / dist) * r2);
                    idx++;
                    break;
                }
            }
        }
    }

    // ── OBSIDIAN NODE ─────────────────────────────────────────────────────────
    private static class GraphNodeVisual extends javafx.scene.layout.StackPane {
        public final TreeItem<Section> associatedTreeItem;
        public final javafx.scene.shape.Circle circle;
        public double vx = 0, vy = 0;
        public boolean isBeingDragged = false;
        private double mouseAnchorX, mouseAnchorY;

        // Pulse animation for underfunded nodes
        private javafx.animation.ScaleTransition pulseAnim;
        private boolean currentlyPulsing = false;

        private final javafx.scene.effect.DropShadow nodeGlow;
        private final String ringHex, glowHex;

        public GraphNodeVisual(TreeItem<Section> treeItem, PrimaryController controller) {
            this.associatedTreeItem = treeItem;
            Section data = treeItem.getValue();

            double baseRadius = 28.0;
            double costBonus  = Math.min(data.getGrandTotal() / 300.0, 22.0);
            double radius     = baseRadius + costBonus;

            // Colors
            Importance imp = data.getImportance();
            String fillHex, labelHex;
            if (imp == Importance.HIGH || data.getGrandTotal() > 1000) {
                ringHex = "#f43f5e"; fillHex = "#1a0508"; glowHex = "#f43f5e"; labelHex = "#fda4af";
            } else if (imp == Importance.MEDIUM) {
                ringHex = "#a78bfa"; fillHex = "#0d0a1a"; glowHex = "#a78bfa"; labelHex = "#c4b5fd";
            } else {
                ringHex = "#34d399"; fillHex = "#061310"; glowHex = "#34d399"; labelHex = "#6ee7b7";
            }

            // Halo
            javafx.scene.shape.Circle halo = new javafx.scene.shape.Circle(radius + 6);
            halo.setFill(javafx.scene.paint.Color.TRANSPARENT);
            halo.setStroke(javafx.scene.paint.Color.web(glowHex, 0.18));
            halo.setStrokeWidth(8);
            halo.setEffect(new javafx.scene.effect.GaussianBlur(8));

            // Main circle
            circle = new javafx.scene.shape.Circle(radius);
            circle.setFill(javafx.scene.paint.Color.web(fillHex));
            circle.setStroke(javafx.scene.paint.Color.web(ringHex, 0.88));
            circle.setStrokeWidth(2.0);

            nodeGlow = new javafx.scene.effect.DropShadow();
            nodeGlow.setColor(javafx.scene.paint.Color.web(glowHex, 0.65));
            nodeGlow.setRadius(radius * 0.9);
            nodeGlow.setSpread(0.12);
            circle.setEffect(nodeGlow);

            // Labels
            String shortName = data.getName().length() > 12
                ? data.getName().substring(0, 11) + "…" : data.getName();

            javafx.scene.control.Label nameLabel = new javafx.scene.control.Label(shortName);
            nameLabel.setStyle("-fx-font-family: 'Consolas',monospace; -fx-font-size: 10; -fx-font-weight: bold;"
                + "-fx-text-fill: " + labelHex + "; -fx-alignment: center; -fx-text-alignment: center;");

            javafx.scene.control.Label costLabel = new javafx.scene.control.Label("$" + (int) data.getGrandTotal());
            costLabel.setStyle("-fx-font-family: 'Consolas',monospace; -fx-font-size: 9;"
                + "-fx-text-fill: rgba(255,255,255,0.4); -fx-alignment: center;");

            javafx.scene.control.Label hint = new javafx.scene.control.Label(
                (!treeItem.getChildren().isEmpty() && !treeItem.isExpanded()) ? "▸" : "");
            hint.setStyle("-fx-text-fill: rgba(255,255,255,0.3); -fx-font-size: 9;");

            javafx.scene.layout.VBox labelBox = new javafx.scene.layout.VBox(1);
            labelBox.setAlignment(javafx.geometry.Pos.CENTER);
            labelBox.getChildren().addAll(nameLabel, costLabel, hint);
            labelBox.setMouseTransparent(true);

            this.getChildren().addAll(halo, circle, labelBox);

            // Hover
            this.setOnMouseEntered(e -> { circle.setStroke(javafx.scene.paint.Color.web(ringHex, 1.0)); circle.setStrokeWidth(3.0); nodeGlow.setSpread(0.28); });
            this.setOnMouseExited (e -> { circle.setStroke(javafx.scene.paint.Color.web(ringHex, 0.88)); circle.setStrokeWidth(2.0); nodeGlow.setSpread(0.12); });

            // Drag
            this.setOnMousePressed (e -> { isBeingDragged = true;  mouseAnchorX = e.getX(); mouseAnchorY = e.getY(); this.toFront(); e.consume(); });
            this.setOnMouseDragged (e -> { setLayoutX(getLayoutX() + e.getX() - mouseAnchorX); setLayoutY(getLayoutY() + e.getY() - mouseAnchorY); });
            this.setOnMouseReleased(e -> isBeingDragged = false);

            // Click: toggle direct children only
            this.setOnMouseClicked(e -> {
                if (isBeingDragged || treeItem.getChildren().isEmpty()) return;
                if (e.getButton() != javafx.scene.input.MouseButton.PRIMARY) return;
                controller.syncingFromGraph = true;
                treeItem.setExpanded(!treeItem.isExpanded());
                controller.syncingFromGraph = false;
                controller.redrawGraphLayout();
                e.consume();
            });

            // Pulse animation (used when underfunded)
            pulseAnim = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(700), circle);
            pulseAnim.setFromX(1.0); pulseAnim.setFromY(1.0);
            pulseAnim.setToX(1.18);  pulseAnim.setToY(1.18);
            pulseAnim.setAutoReverse(true);
            pulseAnim.setCycleCount(javafx.animation.Animation.INDEFINITE);
        }

        /** Call this to start/stop the red pulse based on underfunded state */
        public void setPulse(boolean underfunded) {
            if (underfunded == currentlyPulsing) return;
            currentlyPulsing = underfunded;
            if (underfunded) {
                circle.setStroke(javafx.scene.paint.Color.web("#f43f5e", 1.0));
                circle.setStrokeWidth(3.0);
                nodeGlow.setColor(javafx.scene.paint.Color.web("#f43f5e", 0.9));
                nodeGlow.setSpread(0.35);
                pulseAnim.play();
            } else {
                pulseAnim.stop();
                circle.setScaleX(1.0); circle.setScaleY(1.0);
                circle.setStroke(javafx.scene.paint.Color.web(ringHex, 0.88));
                circle.setStrokeWidth(2.0);
                nodeGlow.setColor(javafx.scene.paint.Color.web(glowHex, 0.65));
                nodeGlow.setSpread(0.12);
            }
        }
    }

    private void clearFields() {
        nameField.clear(); descField.clear(); costField.clear();
    }
}
