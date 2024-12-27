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
    // private ArrayList<Node> children = new ArrayList<Node>();
	private HashMap<Integer, ArrayList<Node>> children = new HashMap<>();

	public void addChild(int id, int value){
		ArrayList<Node> values = children.get(id);
		if (values == null){
			values = new ArrayList<Node>();
			children.put(id, values);
		}

		Node child = new Node();
		child.data = value;

		values.add(child);
    	childCount = childCount + child.getChildCount() + 1;
    	child.parent = this;
    	if(parent!=null)
    		parent.childCount = parent.childCount+1+child.getChildCount();
    }
    
    public void removeRandomChild(int id){
		ArrayList<Node> values = children.get(id);
		int n_removed = 0;
		if(values == null){
			return;
		}

		int sens = (int) (Constants.uniformDouble(0, values.size()-1)+0.5);
		values.remove(sens);

    	if(parent!=null)
    		parent.childCount= parent.childCount-1;
    }
    
    public HashMap<Integer, ArrayList<Node>> getChildren(){
    	return (HashMap<Integer, ArrayList<Node>>) children.clone();
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