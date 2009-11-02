/*
 * PetriNetObjectNameEdit.java
 */
package pipe.gui.undo;

import java.util.ArrayList;

import pipe.dataLayer.PetriNetObject;
import pipe.dataLayer.TAPNQuery;
import pipe.gui.CreateGui;


/**
 *
 * @author corveau
 */
public class PetriNetObjectNameEdit 
        extends UndoableEdit {
   
   PetriNetObject pno;
   String oldName;
   String newName;
   
   
   /** Creates a new instance of placeNameEdit */
   public PetriNetObjectNameEdit(PetriNetObject _pno,
                            String _oldName, String _newName) {
      pno = _pno;
      oldName = _oldName;      
      newName = _newName;
   }

   
   /** */
   public void undo() {
      pno.setName(oldName);
      
      ArrayList<TAPNQuery> queries = CreateGui.getModel().getQueries();
		
      for (TAPNQuery q : queries) {
    	  q.query = q.query.replaceAll(newName + "[^\\_a-zA-Z0-9]", oldName); 
      }
   }

   
   /** */
   public void redo() {
      pno.setName(newName);
      
      ArrayList<TAPNQuery> queries = CreateGui.getModel().getQueries();
		
      for (TAPNQuery q : queries) {
    	  q.query = q.query.replaceAll(oldName + "[^\\_a-zA-Z0-9]", newName); 
      }
   }
   
}
