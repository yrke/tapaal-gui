package dk.aau.cs.TCTL;

public class TCTLAGNode extends TCTLAbstractPathProperty {

private TCTLAbstractStateProperty property;
	
	public TCTLAbstractStateProperty getProperty() {
		return property;
	}
	
	
	
	public TCTLAGNode(TCTLAbstractStateProperty property) {
		this.property = property;
	}


	@Override
	public boolean isSimpleProperty() {
		return false;
	}

	@Override
	public StringPosition[] getChildren() {
		int start = property.isSimpleProperty() ? 0 : 1;
		start = 3 + start;
		int end = start + property.toString().length();
		StringPosition position = new StringPosition(start, end, property);
		
		StringPosition[] children = { position };
		return children;
	}

	
	@Override
	public boolean equals(Object o) {
		if(o instanceof TCTLEFNode) {
			TCTLEFNode node = (TCTLEFNode)o;
			return property.equals(node.getProperty());
		}
		return false;
	}

	@Override
	public String toString() {
		String s = property.isSimpleProperty() ? property.toString() : "(" + property.toString() + ")";
		return "AG " + s;
	}

	@Override
	public TCTLAbstractPathProperty copy() {
		return new TCTLEFNode(property.copy());
	}

	@Override
	public TCTLAbstractPathProperty replace(TCTLAbstractProperty object1, TCTLAbstractProperty object2) {
		if (this == object1 && object2 instanceof TCTLAbstractPathProperty) {
			return (TCTLAbstractPathProperty)object2;
		} else {
			property = property.replace(object1, object2);
			return this;
		}
	}



	@Override
	public void accept(ITCTLVisitor visitor) {
		property.accept(visitor);
		visitor.visit(this);
		
	}



	@Override
	public boolean containsPlaceHolder() {
		return property.containsPlaceHolder();
	}

}
