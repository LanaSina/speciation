package animals;

import communication.MyLog;
import startup.Constants;
import visualization.GraphicalComponent;

import java.awt.*;
import java.util.ArrayList;

import static java.lang.Math.*;
import static java.lang.Math.abs;

public class EmbodiedIndividual extends IndividualWithProperties implements GraphicalComponent {

    //cell
    /**
     * 1 = completely transparent
     */
    double cellTransparency = 1;

    protected double[] position = new double[2];

    public Color color;
    public Color borderColor = Color.white;


    /**
     * creates "light" at specified postion
     *
     * @param x
     * @param y
     * @param glID     id to give to this individual
     * @param ancestor id of the 1st ancestor
     * @param date     in-simulation time
     * @param parent   parent id
     */
    public EmbodiedIndividual(double x, double y, int glID, int ancestor, int date, int parent) {
        super(glID, ancestor, date, parent);
        position[0] = x;
        position[1] = y;
        makeColor();
    }

    public EmbodiedIndividual(int myId, String line) {
        String[] lineArray = line.split(",");
        ID = myId;

        //x, y, id
        int pos = 3;
        position[0] = Double.parseDouble(lineArray[pos]);
        position[1] = Double.parseDouble(lineArray[pos + 1]);
        pos = pos + 2;
        isLight = Boolean.parseBoolean(lineArray[pos]);
        pos++;
        parentID = Integer.parseInt(lineArray[pos]);
        pos++;
        birthDate = Integer.parseInt(lineArray[pos]);
        pos++;
        // lifeSpan
        pos++;
        //
        speed = Integer.parseInt(lineArray[pos]);
        pos++;
        maxEnergy = Integer.parseInt(lineArray[pos]);
        pos++;
        kidEnergy = Integer.parseInt(lineArray[pos]);
        pos++;
        // number or sensors
        pos++;
        firstAncestorID = Integer.parseInt(lineArray[pos]);
        pos++;
        nKids = Integer.parseInt(lineArray[pos]);
        pos++;
        death = Integer.parseInt(lineArray[pos]);
        pos++;
        matForKids = Integer.parseInt(lineArray[pos]);
        pos++;
        energy = Double.parseDouble(lineArray[pos]);
        pos++;
        parentIsLight = Boolean.parseBoolean(lineArray[pos]);

        sensors = new Tree(nProperties);
        makeColor();
    }

    /**
     * clones with mutations
     *
     * @param in   individual to be cloned
     * @param glID id of this one
     */
    EmbodiedIndividual(EmbodiedIndividual in, int glID, int date, double cst_mut_factor, int cst_speed_max,
                       double cst_light_birth_dst, double birth_dst, int cst_grid_max, int cst_energy_max
    ) {
        super(in, glID, date, cst_mut_factor, cst_speed_max, cst_light_birth_dst, birth_dst, cst_grid_max, cst_energy_max);
        //spawn at different postion
        //random close position
        int i = 1, j = 1;
        if (generateBool()) {
            i = -1;
        }
        if (generateBool()) {
            j = -1;
        }
        if (parentIsLight) {
            position[0] = (in.position[0] + i * cst_light_birth_dst * Constants.uniformDouble());
            position[1] = (in.position[1] + j * cst_light_birth_dst * Constants.uniformDouble());
        } else {
            position[0] = (in.position[0] + i * birth_dst * Constants.uniformDouble());
            position[1] = (in.position[1] + j * birth_dst * Constants.uniformDouble());
        }

        for (int k = 0; k < 2; k++) {
            if (position[k] < 0) position[k] = 0;
            if (position[k] >= cst_grid_max - 1) position[k] = cst_grid_max - 2;
        }

        makeColor();
    }

    protected void makeColor() {
		/*int red = (hasSensors()-(nPhysicalProperties-1))*256/(2*10);
		if(red>255) red = 255; if(red<0) red =0;
		//red = 255-red;
		int green = maxEnergy*255/70;//20
		//green = 255-green;
		if(green>255) green = 255; if(green<0) green =0;
		int blue = kidEnergy*255/10;//13
		if(blue>255) blue = 255; if(green<0) green =0;
		//blue = 255 - blue;*/
        int green = (int) (min(1, (speed * 1.0 / 100)) * 255 + 0.5);
        double d = min(1, (kidEnergy * 1.0 / 100));
        int blue = (int) (d * 255 + 0.5);
        d = min(1, (maxEnergy * 1.0 / 200));
        int red = (int) (d * 255 + 0.5);
        color = new Color(red, green, blue);
    }


    /**
     * <p>
     *      Copies the fields of the specified individual into this one, except for this individual's energy, which is set to
     *      the value of the specified individual's kidEnergy.
     * </p>
     * <p>
     *      This process is analogous to genome replication in nature, where the parent (the specified individual) passes its
     *      genes to its offspring (this individual). However, this method does not simulate mutations. These happen after
     *      this method is called.
     * </p>
     *
     * @param in the individual to copy
     */
    protected void copy(EmbodiedIndividual in) {
        super.copy(in);
        this.cellTransparency = in.cellTransparency;
        this.color = in.color;
    }

    public void draw(Graphics g, int gridStep) {
        Graphics2D g2d = (Graphics2D) g;
        int x = (int) (position[0] * gridStep + 0.5);
        int y = (int) (position[1] * gridStep + 0.5);
        int size = 8;

        if (!parentIsLight) {
            Color c = color;
            g2d.setColor(c);
            if (isLight) {
                g2d.drawRect(x, y, size, size);
            } else {
                g2d.fillRect(x, y, size, size);
            }
            g2d.setColor(borderColor);
            g2d.drawRect(x, y, size, size);
        }
        if (isLight) {
            borderColor = Color.black;
            Color c = new Color(borderColor.getRed() / 255.0f, borderColor.getGreen() / 255.0f, borderColor.getBlue() / 255.0f, (float) (cellTransparency));
            g2d.setColor(c);
            g2d.setColor(c);
            g2d.fillRect(x, y, size, size);
        }
    }

    public double[] getPosition() {
        return position;
    }

    public void setPosition(double[] position2) {
        position[0] = position2[0];
        position[1] = position2[1];
    }

    public void setBorderColor(Color color) {
        borderColor = color;
    }

    /**
     * @return a csv string description of this creature
     */
    public String stringDesc() {

        String description = ID + "," + position[0] + "," + position[1] + "," + isLight + "," + parentID + "," + birthDate + "," + life + ","
                + speed + "," + maxEnergy + "," + getKidEnergy() + ","
                + hasSensors() + "," + getAncestor() + "," + getNKids() + ","
                + death + "," + matForKids + "," + energy + "," + parentIsLight;
        return description;
    }

    public void setCellTransparency(double cellTransparency) {
        this.cellTransparency = cellTransparency;
    }


    /**
     * Updates this individual's age and energy, and makes him/her reproduce if possible.
     *
     * @param babies                 the list where this individual's babies are added
     * @param date                   the current time
     * @param transparency           the cell's transparency
     * @param cst_mut_factor         a constant
     * @param cst_speed_max          a constant
     * @param cst_light_birth_dst    a constant
     * @param cst_birth_dst          a constant
     * @param cst_grid_max           a constant
     * @param cst_energy_max         a constant
     * @param cst_speed_cost         a constant
     * @param cst_sensor_cost        a constant
     * @param cst_free_energy        a constant
     * @param cst_energy_cost_factor a constant
     * @param cst_step_cost          a constant
     * @return true if this individual still has energy after the update, false otherwise
     */
    public boolean update(ArrayList<EmbodiedIndividual> babies, int date, double transparency, double cst_mut_factor, int cst_speed_max,
                          double cst_light_birth_dst, double cst_birth_dst, int cst_grid_max, int cst_energy_max, double cst_speed_cost,
                          double cst_sensor_cost, int cst_free_energy, double cst_energy_cost_factor, double cst_step_cost
    ) {
        life = life + 1;
        cellTransparency = transparency;
        double effect = 0.5;

        //remove energy due to sensors
        double se = sensors.getChildCount();
        // se = se/2;
        //mlog.say("se "+se);
        if (!isLight) {
            energy = energy -
                    abs(Math.pow(se, 1.2) * cst_sensor_cost)//*0.1
                    - abs(cst_step_cost);///*maxEnergy);
            if (life >= death) {
                energy = -1;
            }

            if (energy > (maxEnergy)) {
                energy = maxEnergy;
            }
        } else {
            //free energy into light
            energy = energy + cst_free_energy;
        }

        if (energy > 0 && energy >= matForKids) {
            //add children to the map
            int n = 0;
            if (isLight) {
                while (energy - kidEnergy > 0) {//matForKids
                    EmbodiedIndividual baby = new EmbodiedIndividual(this, -1, date, cst_mut_factor, cst_speed_max,
                            cst_light_birth_dst, cst_birth_dst, cst_grid_max, cst_energy_max);
                    babies.add(baby);
                    energy = energy - kidEnergy;
                    n++;
                }
            } else {
                while ((n < getNKids())) {//
                    EmbodiedIndividual baby = new EmbodiedIndividual(this, -1, date, cst_mut_factor, cst_speed_max,
                            cst_light_birth_dst, cst_birth_dst, cst_grid_max, cst_energy_max);
                    babies.add(baby);
                    energy = energy - kidEnergy;//*(1-transparency*effect);
                    n++;

                    if (energy < kidEnergy) {
                        energy = -1;
                        break;
                    }

                }
            }
            borderColor = Color.BLUE;
        }

        if (energy <= 0) {
            return false;
        }

        return true;
    }


    protected MyLog createMyLog() {
        return new MyLog("embodied ind",true);
    }

}
