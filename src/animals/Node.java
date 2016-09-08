package animals;

import java.util.ArrayList;

/**
 * do not add children directly!!!! use functions.
 * @author lana
 *
 */
public class Node {
    public Integer data;
    private Node parent = null;
    private int childCount = 0;
    private ArrayList<Node> children = new ArrayList<Node>();
    
    public void addChild(Node child){
    	children.add(child);
    	//childCount++;
    	childCount = childCount + child.getChildCount() + 1;//child + grandchildren
    	child.parent = this;
    	if(parent!=null)
    		parent.childCount = parent.childCount+1+child.getChildCount();
    }
    
//    public void addChilde(Node child, int n){
//    	children.add(child);
//    	
//    	childCount = childCount + n + 1;//child + grandchildren
//    	child.parent = this;
//    	if(parent!=null)
//   		parent.childCount++;
//    }
    
    public void removeChild(int index){
    	int rem = children.get(index).childCount;
    	childCount=childCount-1-rem;
    	children.remove(index);

    	if(parent!=null)
    		parent.childCount= parent.childCount-1-rem;
    }
    
    public ArrayList<Node> getChildren(){
    	return (ArrayList<Node>) children.clone();
    }
    
    public int getChildCount(){
		return childCount;
	}
    
//    public void setChildCount(int c){
//    	childCount = c;
//    }
}