package animals;

import startup.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * do not add children directly!!!! use functions.
 * @author lana
 *
 */
// there was no need for an actual tree
public class Node {
    public Integer data;
    private Node parent = null;
    private int childCount = 0;
    // sensed value, action
	private HashMap<Integer, ArrayList<Node>> children = new HashMap<>();

	public void addChild(int id, int value){
		ArrayList<Node> values = children.get(id);
		if (values == null){
			values = new ArrayList<Node>();
			children.put(id, values);
		}

		// actually because of sensor cost this is not necessary...
//		boolean doAdd = true;
//		for (Iterator<Node> actionsIt = values.iterator(); actionsIt.hasNext();){
//			Node child = actionsIt.next();
//			if (child.data == value){
//				doAdd = false;
//			}
//		}

		Node child = new Node();
		child.data = value;

		values.add(child);
    	childCount = childCount + child.getChildCount() + 1;
    	child.parent = this;
    	if(parent!=null)
    		parent.childCount = parent.childCount+1+child.getChildCount();
    }
    
    public void removeRandomChild(){
		int sens = (int) (Constants.uniformDouble(0, children.size()-1)+0.5);
		ArrayList<Node> values = children.remove(sens);
		if(values == null){
			return;
		}

    	if(parent!=null)
    		parent.childCount= parent.childCount-1;
    }
    
    public HashMap<Integer, ArrayList<Node>> getChildren(){
    	return children;
    }
    
    public int getChildCount(){
		return childCount;
	}

	public Node copy() {
		Node copied = new Node();
		HashMap<Integer, ArrayList<Node>> copiedChildrenMap = copied.getChildren();
		copied.data = data;
		for (Iterator<Integer> childrenIt = children.keySet().iterator(); childrenIt.hasNext();){
			int key = childrenIt.next();
			ArrayList<Node> nodes = children.get(key);
			ArrayList<Node> copiedChildrenArray = new ArrayList<>();
			for (Iterator<Node> nodeIt = nodes.iterator(); nodeIt.hasNext();) {
				Node copiedChild = nodeIt.next().copy();
				copiedChildrenArray.add(copiedChild);
			}
			copiedChildrenMap.put(key, copiedChildrenArray);
		}

		return copied;
	}
}