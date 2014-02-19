package dk.aau.cs.TCTL.visitors;

import dk.aau.cs.TCTL.TCTLAtomicPropositionNode;
import dk.aau.cs.TCTL.TCTLConstNode;
import dk.aau.cs.TCTL.TCTLPlaceNode;

public class BroadcastTranslationQueryVisitor extends QueryVisitor {
	protected static final String ID_TYPE = "id_t";
	protected static final String PLOCK = "P_lock";
	protected static final String TOKEN_TEMPLATE_NAME = "Token";

	private boolean useSymmetry;
	private int totalTokens;

	public BroadcastTranslationQueryVisitor(boolean useSymmetry, int totalTokens) {
		this.useSymmetry = useSymmetry;
		this.totalTokens = totalTokens;
	}

	@Override
	public void visit(TCTLAtomicPropositionNode atomicPropositionNode,
			Object context) {
		assert(atomicPropositionNode.getRight() instanceof TCTLPlaceNode && atomicPropositionNode.getLeft() instanceof TCTLConstNode):
			"The " + getClass().getCanonicalName() + " cannot translate this query, as the prepositions are too complex";
		TCTLPlaceNode placeNode = (TCTLPlaceNode) atomicPropositionNode.getLeft();
		TCTLConstNode constNode = (TCTLConstNode) atomicPropositionNode.getRight();
		
		if (useSymmetry) {
			append("(sum(i:");
			append(ID_TYPE);
			append(")");
			append(TOKEN_TEMPLATE_NAME);
			append("(i).");
			append(placeNode.getPlace());
			append(") ");
			append(operatorConversion(atomicPropositionNode.getOp()));
			append(" ");
			append(constNode.getConstant());
		} else if (totalTokens == 0) {
			append("(");
			append(TOKEN_TEMPLATE_NAME);
			append(".");
			append(placeNode.getPlace());
			append(") ");
			append(operatorConversion(atomicPropositionNode.getOp()));
			append(" ");
			append(constNode.getConstant());
		} else {
			append("(");
			for (int i = 0; i < totalTokens; i++) {
				if (i > 0) {
					append(" + ");
				}

				append(TOKEN_TEMPLATE_NAME);
				append(i);
				append(".");
				append(placeNode.getPlace());
			}
			append(") ");
			append(operatorConversion(atomicPropositionNode.getOp()));
			append(" ");
			append(constNode.getConstant());
		}
	}

	@Override
	protected void addEnding(QueryType type) {
		if (type == QueryType.EF || type == QueryType.AF) {
			append(" && ");
		} else {
			append(" || !");
		}
		append("Control.");
		append(PLOCK);
	}
}
