package at.yawk.hdr.gui.type;

import at.yawk.hdr.gui.DisposalListeners;
import at.yawk.hdr.gui.MainController;
import at.yawk.hdr.gui.ProgressBarCounter;
import at.yawk.hdr.gui.tree.TreeNode;
import at.yawk.hdr.gui.tree.TreePane;
import at.yawk.hdr.index.HeapDumpItemVisitor;
import at.yawk.hdr.index.Indexer;
import at.yawk.hdr.index.StackData;
import at.yawk.hdr.index.TypeData;
import at.yawk.hdr.scanner.ObjectReferenceScanner;
import at.yawk.hdr.scanner.TypeScanner;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.set.TLongSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

/**
 * @author yawkat
 */
public class ReferenceTreeController {
    @FXML StackPane progressPane;
    @FXML Text progressText;
    @FXML ProgressBar progress;

    @FXML TreePane<ReferenceData> tree;

    private <F extends Future<?>> F bind(F future) {
        DisposalListeners.bind(tree, future, true);
        return future;
    }

    void load(Indexer indexer, MainController mainController, long type) {
        tree.setCellValueFactory(node -> {
            ReferenceData value = node.getValue();
            Text text = new Text(value.getTypeData().getName() + "\n" + value.getReferenceCount());
            text.setTextAlignment(TextAlignment.CENTER);
            text.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                    mainController.openReferenceOwnerData(indexer, value.getTypeData());
                }
            });
            return text;
        });

        TypeScanner rootObjectScanner = new TypeScanner(type);
        bind(indexer.walkHeapDump(rootObjectScanner, createCounter("Collecting root objects…"))).addListener(() -> {
            TLongSet objects = rootObjectScanner.getObjects();

            TypeData typeData = indexer.getTypeIndex().get(type);
            TreeNode<ReferenceData> rootNode = new TreeNode<>(
                    new ReferenceData(typeData.getInstanceCount(), typeData)
            );
            Platform.runLater(() -> tree.setRootNode(rootNode));
            walk(new Pass(indexer, rootNode, objects));
        });
    }

    private ProgressBarCounter createCounter(String name) {
        Platform.runLater(() -> progressText.setText(name));
        return new ProgressBarCounter(progress);
    }

    private void walk(Pass pass) {
        walk(pass.objects, Collections.singleton(pass), 0);
    }

    private void walk(TLongSet allKnownObjects, Collection<Pass> passes, int depth) {
        if (depth >= 8) { // TODO make configurable
            finish();
            return;
        }

        ObjectReferenceScanner[] scanners = passes.stream().map(p -> p.scanner).toArray(ObjectReferenceScanner[]::new);
        if (scanners.length == 0) {
            finish();
            return;
        }

        HeapDumpItemVisitor next = ObjectReferenceScanner.toVisitor(allKnownObjects, scanners);
        bind(passes.iterator().next().indexer.walkHeapDump(
                next,
                createCounter("Collecting branch objects (level " + (depth + 1) + ")")
        )).addListener(() -> {
            for (Pass pass : passes) {
                // clear for GC
                pass.objects = null;
                pass.scanner.close();

                // add found objects to global object set
                pass.scanner.getOwnerObjects().valueCollection().forEach(allKnownObjects::addAll);
            }

            List<Pass> nextPasses = passes.stream()
                    .flatMap(pass -> pass.afterPass().stream())
                    .collect(Collectors.toList());
            walk(allKnownObjects, nextPasses, depth + 1);
        });
    }

    private void finish() {
        Platform.runLater(() -> ((Pane) progressPane.getParent()).getChildren().remove(progressPane));
    }

    private class Pass {
        private final Indexer indexer;
        private final TreeNode<ReferenceData> node;
        private TLongSet objects;
        private final ObjectReferenceScanner scanner;

        public Pass(Indexer indexer, TreeNode<ReferenceData> node, TLongSet objects) {
            this.indexer = indexer;
            this.node = node;
            this.objects = objects;
            this.scanner = new ObjectReferenceScanner(objects);
        }

        List<Pass> afterPass() {
            TLongObjectMap<TLongSet> nextObjects = scanner.getOwnerObjects();

            List<ReferenceData> referenceDataList = new ArrayList<>();

            nextObjects.forEachEntry((classId, objects) -> {
                ReferenceData data = new ReferenceData(
                        objects.size(),
                        indexer.getTypeIndex().get(classId)
                );
                referenceDataList.add(data);
                return true;
            });

            scanner.getOwnerStacks().forEachEntry((stackSerial, referenceCount) -> {
                StackData thread = indexer.getThreadIndex().get(stackSerial);
                if (thread != null) {
                    referenceDataList.add(new ReferenceData(referenceCount, thread));
                }
                return true;
            });

            List<Pass> nextPasses = new ArrayList<>();
            List<TreeNode<ReferenceData>> nextNodes = new ArrayList<>();

            for (ReferenceData data : referenceDataList) {
                TreeNode<ReferenceData> child = new TreeNode<>(data);

                if (data.getTypeData() instanceof TypeData) {
                    nextPasses.add(new Pass(
                            indexer,
                            child,
                            nextObjects.get(((TypeData) data.getTypeData()).getClassHeader().id)
                    ));
                }

                if (referenceDataList.size() > 8) {
                    child.setSize(data.getReferenceCount()); // linear scale
                } else {
                    child.setSize(Math.log10(data.getReferenceCount())); // log10 scale
                }
                nextNodes.add(child);
            }

            Platform.runLater(() -> node.getChildren().addAll(nextNodes));

            return nextPasses;
        }
    }
}
