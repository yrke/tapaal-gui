package net.tapaal.gui.debug

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InspectionStateTest {
    @Test
    fun `diff marks changed values while keeping node identity`() {
        val previous = InspectionNode(
            "Net",
            children = listOf(
                InspectionNode("Place", "input", children = listOf(
                    InspectionNode("Token count", "1"),
                )),
            ),
        ).withStableKeys()
        val current = InspectionNode(
            "Net",
            children = listOf(
                InspectionNode("Place", "input", children = listOf(
                    InspectionNode("Token count", "2"),
                )),
            ),
        ).withStableKeys()

        assertEquals(
            setOf("root/Place=input#0/Token count#0"),
            InspectionSnapshotDiff.changedKeys(previous, current),
        )
        assertEquals(previous.children.single().key, current.children.single().key)
    }

    @Test
    fun `view state retains expansion and pin choices`() {
        val state = InspectionViewState()

        state.setExpanded("root/notes", true)
        state.pin("root/place-input")
        state.pin("root/transition-fire")
        state.unpin("root/place-input")

        assertEquals(setOf("root/notes"), state.expandedKeys())
        assertEquals(listOf("root/transition-fire"), state.pinnedKeys())
        assertTrue(state.pinnedKeys().none { it == "root/place-input" })
    }
}
