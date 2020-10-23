package dk.aau.cs.gui.undo.Colored;

import dk.aau.cs.gui.undo.Command;
import dk.aau.cs.model.CPN.Expressions.ArcExpression;
import dk.aau.cs.model.tapn.TimedInputArc;
import dk.aau.cs.model.tapn.TimedOutputArc;
import pipe.gui.CreateGui;
import pipe.gui.graphicElements.Arc;
import pipe.gui.graphicElements.tapn.TimedOutputArcComponent;

public class SetArcExpressionCommand extends Command {
    Arc arc;
    ArcExpression oldExpression;
    ArcExpression newExpression;
    public SetArcExpressionCommand(Arc arc, ArcExpression oldExpression, ArcExpression newExpression){
        this.arc = arc;
        this.oldExpression = oldExpression;
        this.newExpression = newExpression;
    }

    @Override
    public void undo() {
        arc.setExpression(oldExpression);
        arc.updateLabel(true);
    }

    @Override
    public void redo() {
        arc.setExpression(newExpression);
        arc.updateLabel(true);
    }
}
