package com.alife.tolsim.animals;

import com.alife.tolsim.utils.Utils;

import java.util.*;


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
	private HashMap<Integer, Integer> children = new HashMap<>();


	public void addChild(int sensed, int action){
		children.put(sensed, action);
    	childCount = children.size();
    }

	/**
	 *
	 * @return sensed value, action value
	 */
	public int[] removeRandomChild(){

//		if(children.size()==0){
//			return;
//		}

		int sensedId = (int) (Utils.uniformDouble(0, childCount-1)+0.5);
		List<Integer> keys = new ArrayList<Integer>(children.keySet());
		if (keys.size()==0){
			int a = 0; //breakpoint
		}
		int sensed = keys.get(sensedId);
		int action = children.remove(sensed);

		childCount = children.size();
		return new int[] {sensed, action};
    }
    
    public HashMap<Integer, Integer> getChildren(){
    	return children;
    }
    
    public int getChildCount(){
		return childCount;
	}

	public Node copy() {
		Node copied = new Node();
		HashMap<Integer,Integer> copiedChildrenMap = copied.getChildren();
		copied.data = data;


		if(children.size()>0){
			int a = 0;
		}

		for (Iterator<Integer> childrenIt = children.keySet().iterator(); childrenIt.hasNext();){
			int sensed = childrenIt.next();
			int action = children.get(sensed);
			copied.addChild(sensed, action);
		}

		if(copied.childCount != childCount){
			int a = 0; //breakpoint
		}

		return copied;
	}
}