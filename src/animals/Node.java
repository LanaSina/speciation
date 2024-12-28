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

	/**
	 *
	 * @param id
	 * @param value
	 */
	public void addChild(int id, int value){
		if (children.size()>0){
			int a = 0;
		}
		ArrayList<Node> values = children.get(id);
		if (values == null){
			values = new ArrayList<Node>();
			children.put(id, values);
		}

		Node child = new Node();
		child.data = value;

		values.add(child);
    	childCount = childCount + 1; //child.getChildCount()
		if(childCount!=children.size()){
			int a = 0;
		}
    	child.parent = this;
    	if(parent!=null)
    		parent.childCount = parent.childCount+1+child.getChildCount();
    }

	/**
	 *
	 * @return sensed value, action value
	 */
	public int[] removeRandomChild(){

		if(children.size()==0){
			return new int[]{-1, -1};
		}

		int sens = (int) (Constants.uniformDouble(0, children.size()-1)+0.5);
		Object[] a = children.keySet().toArray();
		ArrayList<Node> values = children.get(a[sens]);

		if(values.size()==0){
			return new int[]{-1, -1};
		}

		int act = (int) (Constants.uniformDouble(0, values.size()-1)+0.5);
		values.remove(act);

		childCount = childCount - 1;
    	if(parent!=null)
    		parent.childCount= parent.childCount-1;

		return new int[]{sens, act};
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
		copied.childCount = childCount;

		if(children.size()>0){
			int a = 0;
		}

		for (Iterator<Integer> childrenIt = children.keySet().iterator(); childrenIt.hasNext();){
			int key = childrenIt.next();
			ArrayList<Node> nodes = children.get(key);
			ArrayList<Node> copiedChildrenArray = new ArrayList<>();

			for (Iterator<Node> nodeIt = nodes.iterator(); nodeIt.hasNext();) {
				Node copiedChild = nodeIt.next().copy();
				copiedChild.parent = copied;
				copiedChildrenArray.add(copiedChild);
			}
			copiedChildrenMap.put(key, copiedChildrenArray);
		}

		return copied;
	}
}