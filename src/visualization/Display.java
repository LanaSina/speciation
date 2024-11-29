package visualization;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import communication.MyLog;
import startup.Constants;

/**
 * Graphic panel
 * The field is an array.
 * @author lana
 *
 */
public class Display extends JFrame {
		MyLog mlog = new MyLog("Display",true);

		private static final long serialVersionUID = 1579747902278268747L;
		
		//surface to be drawn on
		Surface s; 
		String name;
		
		/**
		 * 
		 * @param n name of window;
		 */
		public Display(String n) {
			name = n;
	        initUI();
	        this.setVisible(true);
	    }

		public Display() {
			name = "Open Ended Evolution";
	        initUI();
	        this.setVisible(true);
	    }
	    private void initUI() {
	    	
	        setTitle(name);
	        s = new Surface();
	        add(s);
	        int width = s.getWidth();
	        int length = s.getLength();
	        setSize(width,length);
	        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	        setLocationRelativeTo(null);
	        
	        //refresh
	        int delay = Constants.refresh_rate; //milliseconds

	        ActionListener taskPerformer = new ActionListener() {
	          public void actionPerformed(ActionEvent evt) {
	        	Thread t = new Thread(new Runnable() {
					public void run() {
			        	  s.repaint();
					}
				});
	        	t.start();
	          }
	        };

	        new Timer(delay, taskPerformer).start();
	    }
	    
	    /**
	     * Add a object to be drawn on the pannel.
	     * @param c the object implementing the component interface
	     */
	    public void addComponent(GraphicalComponent c){
	    	s.addComponent(c);
	    }
	    
	   /* public void removeComponent(GraphicalComponent c) {
			s.removeComponent(c);
		}*/
	    
	    //hem ?
	    public JPanel getSurface(){
	    	return s;
	    }

		public void removeComponent(GraphicalComponent c) {
			//int i = s.components.indexOf(c);
			//s.components.remove(i);
			s.removeComponent(c);
		}
	    
	    /**
	     * Add an object to be controlled by keyboard actions.
	     * @param p the object to be controlled
	     * @param string a unique name
	     */
}
	

	/**
	 * The surface on which we draw the graphics.
	 * @author lana
	 *
	 */
	class Surface extends JPanel{
		MyLog mlog = new MyLog("Surface in Display",true);

		//size
		int w = 1200;
		int h = 1200;
        //grid step size
        int step = Constants.GridStep;
        boolean pause = false;

		//list of things to draw
		public List<GraphicalComponent> components = new ArrayList<GraphicalComponent>();

		public Surface(){
			super();

			// add button
			JButton pause = new JButton("Pause");
			pause.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					// Insert code here
					mlog.say("pause pressed");
				}
			});


			this.add(pause);
			pause.setVisible(true);
			int x = this.getWidth()*2-200; // no effect??
			pause.setLocation(new Point(x, 0));
			this.revalidate();
			this.repaint();
		}
		
	    /**
	     * Adds a object to be drawn on the pannel.
	     * @param c the object implementing the component interface
	     */
	    public void addComponent(GraphicalComponent c){
	    	while(pause){
	    		try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    	}
	    	pause = true;
	    	components.add(c);
	    	pause = false;
	    }
	    
	    public void removeComponent(GraphicalComponent c) {
	    	while(pause){
	    		try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    	}
	    	pause = true;
			int i = components.indexOf(c);
			components.remove(i);
			pause = false;
		}
	    
		private static final long serialVersionUID = 6523850037367826272L;

		/**
		 * Sets the background grid.
		 * @param g
		 */
		private void init(Graphics g) {
			this.setOpaque(true);
			this.setBackground(Color.white);
			
	        Graphics2D g2d = (Graphics2D) g;
	        
	        //draw the coordinates lines
	        g2d.setColor(Color.black);
	        

	        for(int i=0;i<w;i+=step){
	        	//horizontal lines
	        	g2d.drawLine(0,i,w,i);
	        	//vertical lines
	        	g2d.drawLine(i,0,i,w);
	        }
		}

	    @Override
	    public void paintComponent(Graphics g) {

	        super.paintComponent(g);
	        init(g);
	        
	        if(Constants.draw){
	        	while(pause){
		    		try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		    	}
	        	
	        	pause = true;
	        	
		        for(int i=0;i<components.size();i++){
		        	if(Constants.uniformDouble()<Constants.draw_coarse){
		        		components.get(i).draw(g,step);
		        	}
		        }
		        
		        pause = false;
	        }
	        
	    }
	    
	    public int getWidth(){
	    	return w;
	    }
	    
	    public int getLength(){
	    	return h;
	    }
	    
	    public int getStep(){
	    	return step;
	    }
		   
	}
	
	
