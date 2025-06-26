package com.alife.tolsim.visualization;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.*;

import com.alife.tolsim.communication.MyLog;
import com.alife.tolsim.startup.Constants;
import com.alife.tolsim.startup.Starter;

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
	public Display(String n, Starter.LifeRunnable lifeRunnable, String dataFolder) {
		name = n;
		initUI(lifeRunnable, dataFolder);
		this.setVisible(true);
	}

	private void initUI(Starter.LifeRunnable lifeRunnable, String dataFolder) {

		setTitle(name);
		s = new Surface(lifeRunnable, dataFolder);
		add(s);
		int width = s.getWidth();
		int length = s.getLength();
		setSize(width,length);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);

		//refresh
		int delay = Constants.RefreshRate; //milliseconds

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
	public synchronized void addComponent(GraphicalComponent c){
		s.addComponent(c);
	}

   /* public void removeComponent(GraphicalComponent c) {
		s.removeComponent(c);
	}*/

	//hem ?
	public JPanel getSurface(){
		return s;
	}

	public synchronized void removeComponent(GraphicalComponent c) {
		//int i = s.components.indexOf(c);
		//s.components.remove(i);
		s.removeComponent(c);
	}

	public synchronized void removeAllComponents() {
		s.components.clear();
	}

	public synchronized int getNbOfComponents() {
		return s.components.size();
	}

	public synchronized void pauseProcedure(boolean b){
		s.pauseProcedure(b);
	}

	public synchronized void saveProcedure() {
		s.saveProcedure();
	}

	public synchronized void screenshot() {
		s.screenshot();
	}
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
		JButton saveButton;
		Starter.LifeRunnable lifeRunnable;
		String dataFolder = null;

		/** The things to draw. Associates the GraphicalComponents' IDs to the GraphicalComponents themselves*/
		public java.util.Map<Integer, GraphicalComponent> components = new HashMap<>();

		public Surface(Starter.LifeRunnable myLifeRunnable, String myDataFolder){
			super();
			this.lifeRunnable = myLifeRunnable;
			this.dataFolder = myDataFolder;

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
			saveButton = new JButton("Save");
			saveButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					saveProcedure();
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
					 pauseProcedure(false);
				 }
			});

			this.add(loadButton);
			loadButton.setVisible(true);
			x = this.getWidth()*2+200;
			loadButton.setLocation(new Point(x, 0));

			// add screenshot button
			JButton shotButton = new JButton("Screenshot");
			shotButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					pauseProcedure(true);
					screenshot();
					pauseProcedure(false);
				}
			});

			this.add(shotButton);
			shotButton.setVisible(true);
			x = this.getWidth()*2+200;
			shotButton.setLocation(new Point(x, 0));

			this.revalidate();
			this.repaint();
		}

		public void pauseProcedure(boolean b){
			pauseLife = b;
			mlog.say("pause is " + pauseLife);
			pauseButton.setText(pauseLife?"Resume":"Pause");
			lifeRunnable.running = !pauseLife;
		}

		public void saveProcedure(){
			pauseProcedure(true);
			saveButton.setText("Saving...");
			//save
			lifeRunnable.save();
			// this should actually be delayed...
			saveButton.setText("Save");
		}

		public void screenshot(){
			//get current date
			DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm");
			Date date = new Date();
			String strDate = dateFormat.format(date);

			String snapshotLocation = dataFolder + "/screenshot_" + strDate + ".jpg";
			mlog.say("Saving screenshot at " + snapshotLocation);
			BufferedImage bufImage = new BufferedImage(getSize().width, getSize().height,BufferedImage.TYPE_INT_RGB);
			paint(bufImage.createGraphics());
			File imageFile = new File("."+File.separator+snapshotLocation);
			try{
				imageFile.createNewFile();
				ImageIO.write(bufImage, "jpeg", imageFile);
			}catch(Exception ex){
			}
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
	    	components.put(c.getID(), c);
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
			components.remove(c.getID());
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

			while(pause){
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			pause = true;

			for(GraphicalComponent graphicalComponent : components.values()){
				if(Constants.uniformDouble()<Constants.DrawCoarse){
					graphicalComponent.draw(g,step);
				}
			}

			pause = false;
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
	
	
