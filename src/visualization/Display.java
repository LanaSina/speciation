package visualization;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import communication.MyLog;
import startup.Constants;
import startup.Starter;

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
		public Display(String n, Starter.LifeRunnable lifeRunnable) {
			name = n;
	        initUI(lifeRunnable);
	        this.setVisible(true);
	    }

//		public Display(Object lock) {
//			name = "Open Ended Evolution";
//	        initUI(lock);
//	        this.setVisible(true);
//	    }
	    private void initUI(Starter.LifeRunnable lifeRunnable) {
	    	
	        setTitle(name);
	        s = new Surface(lifeRunnable);
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
		boolean pauseLife = false;

		// buttons
		JButton pauseButton;
		Starter.LifeRunnable lifeRunnable;

		//list of things to draw
		public List<GraphicalComponent> components = new ArrayList<GraphicalComponent>();

		public Surface(Starter.LifeRunnable myLifeRunnable){
			super();
			this.lifeRunnable = myLifeRunnable;

			// add pause button
			pauseButton = new JButton("Pause");
			pauseButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					pauseProcedure(!pauseLife);
				}
			});
			this.add(pauseButton);
			pauseButton.setVisible(true);
			int x = this.getWidth()*2-200; // no effect??
			pauseButton.setLocation(new Point(x, 0));

			// add save button
			JButton saveButton = new JButton("Save");
			saveButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					pauseProcedure(true);
					saveButton.setText("Saving...");
					//save
					String savedAt = lifeRunnable.save();
					mlog.say("Saved at " + savedAt);
					saveButton.setText("Save");
				}
			});
			this.add(saveButton);
			saveButton.setVisible(true);
			x = this.getWidth()*2+200; // no effect??
			saveButton.setLocation(new Point(x, 0));

			// add load button
			JButton loadButton = new JButton("Load file");
			loadButton.addActionListener(new ActionListener() {
				 public void actionPerformed(ActionEvent e) {
					 pauseProcedure(true);
					 File workingDirectory = new File(System.getProperty("user.dir"));
					 UIManager.put("FileChooser.saveButtonText","Load");

					 JFileChooser fileChooser = new JFileChooser();
					 fileChooser.setCurrentDirectory(workingDirectory);
					 fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					 fileChooser.showSaveDialog(null);

					 System.out.println(fileChooser.getCurrentDirectory());
					 File directory = fileChooser.getCurrentDirectory();
					 lifeRunnable.load(directory);
					 //openButton.getProperties().put("FILE_LOCATION", file.getAbsolutePath());
				 }
			});

			this.add(loadButton);
			loadButton.setVisible(true);
			x = this.getWidth()*2+200;
			loadButton.setLocation(new Point(x, 0));

			this.revalidate();
			this.repaint();
		}

		private void pauseProcedure(boolean b){
			pauseLife = b;
			mlog.say("pause is " + pauseLife);
			pauseButton.setText(pauseLife?"Resume":"Pause");
			lifeRunnable.running = !pauseLife;
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
	
	
