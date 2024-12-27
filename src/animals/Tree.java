package animals;

import java.util.ArrayList;
import java.util.List;

import communication.MyLog;

/**
 * There is no tree data structure in java...
 * Three mapping integers.
 * @author lana
 *
 */
public class Tree {
	MyLog mlog = new MyLog("tree",true);
	public Node root;
	//private int childCount = 0;
	//tree nodes: root-> 3properties -> detectionValue -> action
	//          0          id              value            id
	
	public Tree(int nProperties) {
	    root = new Node();
	    root.data = 0;
		for(int i=0; i<(nProperties);i++){
			ArrayList<Node> values = new ArrayList<>();
			root.getChildren().put(i, values);
		}
	}
	
	public Tree copy(){
		int nProperties = root.getChildren().keySet().size();
		Tree copied = new Tree(nProperties);
		copied.root = root.copy();

		//copied.root = copyNode(this.root, 0);
		return copied;
	}

	// root -> property being sensed -> value being sensed -> action
	public void addSensor(int property, int property_val, int action){
		Node act = new Node();
		act.data = action;
		root.getChildren().get(property).add(property_val, act);
	}
	
//	//let's allow only 3rd level
//	/**recursive copy*/
//	private Node copyNode(Node original, int depth){
//		if (depth>3){
//			mlog.say("error depth "+depth);
//		}
//		Node copied = new Node();
//		copied.data = original.data;
//		ArrayList<Node> orChildren = original.getChildren();
//		for(int i = 0; i<orChildren.size();i++){
//			Node orChild = orChildren.get(i);
//			Node child = copyNode(orChildren.get(i),depth+1);
//			child.data = orChild.data;
//
//			copied.addChild(child);
//		}
//		return copied;
//	}
}

