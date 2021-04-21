package dk.aau.cs.TCTL;

public class LTLAGNode extends TCTLAGNode {
    TCTLAbstractStateProperty property;

    public LTLAGNode(TCTLAbstractStateProperty property) {
        this.property = property;
        this.property.setParent(this);
    }

    @Override
    public String toString() {
        String s = property.isSimpleProperty() ? property.toString() : "("
            + property.toString() + ")";
        return "G " + s;
    }

    @Override
    public StringPosition[] getChildren() {
        int start = property.isSimpleProperty() ? 0 : 1;
        start = start + 2;
        int end = start + property.toString().length();
        StringPosition position = new StringPosition(start, end, property);

        StringPosition[] children = { position };
        return children;
    }

    @Override
    public void convertForReducedNet(String templateName) {
        property.convertForReducedNet(templateName);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof LTLAGNode) {
            LTLAGNode node = (LTLAGNode) o;
            return property.equals(node.getProperty());
        }
        return false;
    }

    @Override
    public TCTLAbstractPathProperty replace(TCTLAbstractProperty object1,
                                            TCTLAbstractProperty object2) {
        if (this == object1 && object2 instanceof TCTLAbstractPathProperty) {
            return (TCTLAbstractPathProperty) object2;
        } else {
            property = property.replace(object1, object2);
            return this;
        }
    }

    @Override
    public TCTLAbstractPathProperty copy() {
        return new LTLAGNode(property.copy());
    }

    @Override
    public boolean containsPlaceHolder() {
        return property.containsPlaceHolder();
    }
}
