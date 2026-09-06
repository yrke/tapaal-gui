package net.tapaal.gui;

import pipe.gui.petrinet.PetriNetTab;

import java.util.Optional;

/**
 * The narrow application seam for resolving the active tab.
 */
public interface TabInteraction {
    Optional<PetriNetTab> getCurrentTab();
}
