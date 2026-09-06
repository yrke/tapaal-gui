package pipe.gui.petrinet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import net.tapaal.gui.petrinet.Template;
import net.tapaal.gui.GuiFrameController;
import pipe.gui.GuiFrame;
import pipe.gui.petrinet.graphicElements.tapn.TimedPlaceComponent;

@Tag("gui")
class PetriNetTabSmokeTest {

    @Test
    void createsAnEditorAndKeepsPlaceModelAndViewInSyncAcrossUndoRedo() throws Exception {
        AtomicReference<PetriNetTab> tabRef = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            PetriNetTab tab = PetriNetTab.createNewEmptyTab("smoke.tapn", true, false, false, false);
            tabRef.set(tab);

            Template template = tab.currentTemplate();
            assertNotNull(template);
            assertEquals(0, template.model().places().size());

            TimedPlaceComponent place = tab.guiModelManager
                .addNewTimedPlace(template.guiModel(), new Point(100, 100)).result;
            assertEquals(1, template.model().places().size());
            assertEquals(1, template.guiModel().getPlaces().length);

            tab.getUndoManager().undo();
            assertEquals(0, template.model().places().size());
            assertEquals(0, template.guiModel().getPlaces().length);

            tab.getUndoManager().redo();
            assertEquals(1, template.model().places().size());
            assertEquals(1, template.guiModel().getPlaces().length);

            tab.getUndoManager().newEdit();
            tab.guiModelManager.deleteSelection(new ArrayList<>(List.of(place)));
            assertEquals(0, template.model().places().size());
            assertEquals(0, template.guiModel().getPlaces().length);

            // Undo must restore the model before the view repaints and reads
            // the place marking.
            tab.getUndoManager().undo();
            assertEquals(1, template.model().places().size());
            assertEquals(1, template.guiModel().getPlaces().length);
        });
    }

    @Test
    void controllerOwnsCurrentTabSelectionAndTabModelsKnowTheirOwner() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GuiFrame frame = new GuiFrame("test");
            GuiFrameController controller = new GuiFrameController(frame);
            PetriNetTab tab = PetriNetTab.createNewEmptyTab("owner.tapn", true, false, false, false);

            assertTrue(controller.getCurrentTab().isEmpty());
            controller.openTab(tab);

            assertEquals(tab, controller.getCurrentTab().orElseThrow());
            assertEquals(tab, tab.getModel().getOwnerTab());
            assertEquals(tab, tab.currentTemplate().guiModel().getOwnerTab());

            controller.closeTab(tab);
            assertTrue(controller.getCurrentTab().isEmpty());
            frame.dispose();
        });
    }
}
