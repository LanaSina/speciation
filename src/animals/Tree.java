package animals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import communication.MyLog;

/**
 * A map of nodes; each node has int data + its map of nodes
 * @author lana
 *
 */
public class Tree {
	MyLog mlog = new MyLog("tree",true);
	// public Node root;
	public HashMap<Integer, Node> properties = new HashMap<>();
	//private int childCount = 0;
	//tree nodes: root-> 3properties -> detectionValue -> action
	//          0          id              value            id
	
	public Tree(int nProperties) {
	    //root = new Node();
	    //root.data = 0;
		for(int i=0; i<(nProperties);i++){
			Node sensedValues = new Node();
			sensedValues.data = i;
			properties.put(i, sensedValues);
			// ArrayList<Node> values = new ArrayList<>();
			//root.getChildren().put(i, values);
		}
	}

	public Tree(){

	}
	
	public Tree copy(){
		int nProperties = properties.size();
		Tree copied = new Tree();
		for(int i=0; i<(nProperties);i++) {
			Node sensedValues = properties.get(i);
			copied.addSensor(i, sensedValues.copy());
		}
		//copied.root = copyNode(this.root, 0);
		return copied;
	}

	// will replace sensor if it exists
	public void addSensor(int property, Node node){
		properties.put(property, node);
	}


	// root -> property being sensed -> value being sensed -> action
	public void addSensor(int property, int property_val, int action){
		properties.get(property).addChild(property_val, action);
	}

	public void removeRandomChild(int prop) {
		Node node = properties.get(prop);
		node.removeRandomChild();
	}

	public int getActionsCount() {
		int n = 0;
		for (int i = 0; i < properties.size(); i++){
			Node node = properties.get(i);
			n = n + node.getChildCount();
		}
		return n;
	}
}

