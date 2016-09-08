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
	
	public Tree(Integer rootData) {
	    root = new Node();
	    root.data = rootData;
	   // root.children = new ArrayList<Node>();
	}
	
	public Tree copy(){
		Tree copied = new Tree(this.root.data);		
		copied.root = copyNode(this.root, 0);
		return copied;
	}
	
	//let's allow only 3rd level
	/**recursive copy*/
	private Node copyNode(Node original, int depth){
		if (depth>3){
			mlog.say("error depth "+depth);
		}
		Node copied = new Node();
		copied.data = original.data;
		ArrayList<Node> orChildren = original.getChildren();
		for(int i = 0; i<orChildren.size();i++){
			Node orChild = orChildren.get(i);
			Node child = copyNode(orChildren.get(i),depth+1);
			child.data = orChild.data;
			
			copied.addChild(child);
		}
		return copied;
	}
	
//	public int getChildCount(){
//		return childCount;
//	}
}

