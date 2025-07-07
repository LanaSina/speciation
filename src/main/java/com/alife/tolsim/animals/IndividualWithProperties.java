package com.alife.tolsim.animals;

import com.alife.tolsim.communication.MyLog;
import com.alife.tolsim.startup.Constants;
import com.alife.tolsim.utils.Utils;

import static java.lang.Math.max;


public abstract class IndividualWithProperties implements Individual {

    MyLog mlog = createMyLog();


    long eaten_by = -1;//1 = true;

    //general
    public int speed = 0;
    //int lifespan;
    int maxEnergy;
    //energy level transmitted to offspring
    int kidEnergy = 2;
    //level of energy at which to have kids
    int matForKids = 4;
    //nr of kids
    protected int nKids = 2;
    //sensors: map of property value to action
    public int death = (int) (Utils.uniformDouble(0, 4) - 2 + 0.5) + 20;//50


    //will be replaced by cell properties sensors
    /**
     * a tree with properties->pair(value,action)
     */
    public Tree sensors;

    int nProperties = 6;//sum of above
    public int[] properties = new int[nProperties];
    long ID;
    long parentID;
    protected long firstAncestorID;
    //if parent is light, won't be written down (data too big)
    public boolean parentIsLight = false;
    int birthDate;
    //life span
    int life = 0;

    //particular
    public double energy;
    protected double[] position = new double[2];
    //for eternal light cells
    protected boolean isLight = false;

    protected IndividualWithProperties() {}

    /**
     * creates "light" at specified postion
     *
     * @param glID     id to give to this individual
     * @param ancestor id of the 1st ancestor
     * @param date     in-simulation time
     * @param parent   parent id
     */
    public IndividualWithProperties(long glID, long ancestor, int date, long parent) {
        super();

        ID = glID;
        firstAncestorID = ancestor;
        parentID = parent;
        //mlog.say("id " + ID);
        birthDate = date;

        speed = 0;
        maxEnergy = 4;
        energy = 3;
        isLight = true;
        //mlog.say("--------------- IS LIGHT");

        properties[0] = speed;
        properties[1] = maxEnergy;
        properties[2] = kidEnergy;
        properties[3] = getNKids();
        properties[4] = death;
        properties[5] = matForKids;

        sensors = new Tree(nProperties);//root is not important
    }


    /**
     * clones with mutations
     *
     * @param in   individual to be cloned
     * @param glID id of this one
     */
    IndividualWithProperties(IndividualWithProperties in, long glID, int date, double cst_mut_factor, int cst_speed_max,
                             double cst_light_birth_dst, double birth_dst, int cst_grid_max, int cst_energy_max
    ) {
        copy(in);
        if (in.isLight) parentIsLight = true;

        ID = glID;
        firstAncestorID = in.getAncestor();
        //if your ancestor is the sun, then ou're the new first ancestor
        if (firstAncestorID < 0) {
            mlog.say("Error: negative ancestor ID; consider checking whether the maximum value for a long integer has been reached.");
        }

        if ((firstAncestorID == 0) && (ID != -1)) {
            firstAncestorID = ID;
        }

        energy = in.kidEnergy;
        birthDate = date;
        parentID = in.ID;

        //copy sensor map
        sensors = in.sensors.copy();

        // 50% chance to mutate
        if (generateBool()) {

            // mutation parameters
            double minMut = Utils.uniformDouble(-2, 2);
            //double plus = Utils.uniformDouble(-cst_mut_factor, cst_mut_factor);
            double plus = Utils.uniformDouble(-0.01, 0.01);


            //do this after too
            properties[0] = speed;
            properties[1] = maxEnergy;
            properties[2] = kidEnergy;
            properties[3] = getNKids();
            properties[4] = death;
            properties[5] = matForKids;

            //maybe make this a mutable value!
            double bias = 0.6;
            // mutate speed
            if (generateBool(bias)) {
                // double minMut = Utils.uniformDouble(-2, 2);
                speed = (int) (speed + minMut);
                // todo speed = check()
                if (speed < 0) speed = 0;
                if (speed > cst_speed_max) speed = cst_speed_max;
                //break;
            }
            // mutate maxEnergy
            if (generateBool(bias)) {
                // double minMut = Utils.uniformDouble(-2, 2);
                maxEnergy = (int) (maxEnergy + minMut);
                if (maxEnergy < 0) {
                    maxEnergy = 0;
                } else if (maxEnergy > cst_energy_max) {
                    maxEnergy = cst_energy_max;
                }
            }
            // mutate kidEnergy
            if (generateBool(bias)) {
                // double minMut = Utils.uniformDouble(-2, 2);
                kidEnergy = (int) (kidEnergy + minMut);
                if (kidEnergy < 0) kidEnergy = 0;
            }
            // mutate matForKids
            if (generateBool(bias)) {
                // double minMut = Utils.uniformDouble(-2, 2);
                matForKids = (int) (matForKids + minMut);
                if (matForKids < 0) matForKids = 0;
            }

            if (generateBool(bias)) {
                //if(true){
                //create or modify sensor
                // double plus = Utils.uniformDouble(-1, 1);
                if (plus > 0) {
                    int prop = (int) (Utils.uniformDouble(0, nProperties - 1) + 0.5);//-1

                    // modify the detection value if sensor exists
                    if (generateBool()) {
                        // tree nodes: properties -> detectionValue -> action
                        Node sensedValues = sensors.properties.get(prop);
                        if (sensedValues.getChildCount() > 0) {
                            //get random sensor
                            int[] actionPair = sensors.removeRandomSensor(prop);
                            // if(actionPair[0]>-1) {
                            // new sensed value
                            int value = (int) max(0, (actionPair[0] + Utils.uniformDouble(-3, 3)));
                            // same action
                            sensedValues.addChild(value, actionPair[1]);
                            // }
                        }
//						else {
//							// create sensor.
//							// property being sensed -> value being sensed -> action
//							int sensor_value = (int) Utils.uniformDouble(0, cst_energy_max);
//							int action = (int) (Utils.uniformDouble(0, Constants.ActionTypes-1)+0.5);
//							sensors.addSensor(prop, sensor_value, action);
//						}
                    } else {
                        // create sensor.
                        // root -> property being sensed -> value being sensed -> action
                        // root -> [prop id, array]
                        int sensor_value = (int) Utils.uniformDouble(0, cst_energy_max);
                        int action = (int) (Utils.uniformDouble(0, Constants.ActionTypes - 1) + 0.5);
                        sensors.addSensor(prop, sensor_value, action);
                    }
                } else {
                    int prop = (int) (Utils.uniformDouble(0, nProperties - 1) + 0.5);
                    if (sensors.properties.get(prop).getChildCount() > 0) {
                        //tree nodes: properties -> detectionValue -> action
                        int[] values = sensors.removeRandomChild(prop);
                    }
                }
            }

            // mutate nKids
            if (generateBool(bias)) {
				/*double plus = Utils.uniformDouble(-1, 1)*0.01; //1% change
				double minMut = Utils.uniformDouble(-2, 2); // direct intervention for small values*/

                int n = (int) (getNKids() * (plus + 1) + minMut + 0.5);
                if (n < 0) n = 0;
                setNKids(n);
            }

            // mutate death
            if (generateBool(bias)) {
				/*double plus = Utils.uniformDouble(-1, 1)*0.01; //1% change
				double minMut = Utils.uniformDouble(-2, 2); // direct intervention for small values*/

                death = (int) (death * (1 + plus) + minMut + 0.5);
                if (death < 0) death = 0;
            }
        }

        properties[0] = speed;
        properties[1] = maxEnergy;
        properties[2] = kidEnergy;
        properties[3] = getNKids();
        properties[4] = death;
        properties[5] = matForKids;
    }

    protected double check(double val, double low, double high) {
        if (val < low) val = low;
        if (val > high) val = high;
        return val;
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
    protected void copy(IndividualWithProperties in) {
        this.position = in.position.clone();
        this.maxEnergy = in.maxEnergy;
        this.energy = in.kidEnergy;
        //this.lifespan = in.lifespan;
        this.kidEnergy = in.kidEnergy;
        this.speed = in.speed;
        this.matForKids = in.matForKids;
        this.setNKids(in.getNKids());
        this.death = in.death;

        this.sensors = in.sensors.copy();
    }

    /**
     * @param bias between 0 and 1; probability to return true.
     * @return a boolean, with a probability of <code>bias</code> to be true
     */
    protected boolean generateBool(double bias) {
        return Utils.uniformDouble() < bias;
    }

    protected boolean generateBool() {
        return Utils.uniformDouble() > 0.5;
    }

    public long getID() {
        return ID;
    }

    public void setID(long id) {
        ID = id;
        if (firstAncestorID == 0) firstAncestorID = id;
    }

    public long getParentID() {
        return parentID;
    }

    public int getBirth() {
        return birthDate;
    }

    public int getLifeSpan() {
        return life;
    }

    public double getSpeed() {
        return speed;
    }

    public int getMaxEnergy() {
        return maxEnergy;
    }

    public int getKidEnergy() {
        return kidEnergy;
    }

    public long getAncestor() {
        return firstAncestorID;
    }

    public int hasSensors() {
        return sensors.getChildCount();
    }


    public int getNKids() {
        return nKids;
    }

    public void setNKids(int nKids) {
        this.nKids = nKids;
    }

    public boolean isLight() {
        return isLight;
    }

    public boolean parentIsLight() {
        return parentIsLight;
    }

    /**
     * @return a csv string description of this creature
     */
    public String stringDesc() {

        return ID + "," + isLight + "," + parentID + "," + birthDate + "," + life + ","
                + speed + "," + maxEnergy + "," + getKidEnergy() + ","
                + hasSensors() + "," + getAncestor() + "," + getNKids() + ","
                + death + "," + matForKids + "," + energy + "," + parentIsLight;
    }

    public int[] getProperties() {
        return properties;
    }

    public int getDeath() {
        return death;
    }

    public void setDeath(int death) {
        this.death = death;
    }

    public Tree getSensors() {
        return sensors;
    }

    public void setSensors(Tree sensors) {
        this.sensors = sensors;
    }

    public double getEnergy() {
        return energy;
    }

    public void setEnergy(double energy) {
        if (energy > maxEnergy) {
            energy = maxEnergy;
        }
        this.energy = energy;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public void setEatenBy(long eaten) {
        this.eaten_by = eaten;
    }

    public void setParentID(long parentID) {
        this.parentID = parentID;
    }

    public void setMaxEnergy(int maxEnergy) {
        this.maxEnergy = maxEnergy;
    }

    public void setKidEnergy(int kidEnergy) {
        this.kidEnergy = kidEnergy;
    }

    public int getMatForKids() {
        return matForKids;
    }

    public void setMatForKids(int matForKids) {
        this.matForKids = matForKids;
    }

    public int getnProperties() {
        return nProperties;
    }

    public void setnProperties(int nProperties) {
        this.nProperties = nProperties;
        this.properties = new int[nProperties];
    }

    public long getFirstAncestorID() {
        return firstAncestorID;
    }

    public void setFirstAncestorID(long firstAncestorID) {
        this.firstAncestorID = firstAncestorID;
    }

    public int getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(int birthDate) {
        this.birthDate = birthDate;
    }

    public void setSensors() {

    }

    public void setIsLight(boolean b) {
        isLight = b;
    }

    protected abstract MyLog createMyLog();

    public boolean equals(Object o) {
        if (!(o instanceof IndividualWithProperties other))
            return false;
        return this.speed == other.speed &&
                this.maxEnergy == other.maxEnergy &&
                this.kidEnergy == other.kidEnergy &&
                this.matForKids == other.matForKids &&
                this.nKids == other.nKids &&
                this.death == other.death &&
                this.ID == other.ID &&
                this.parentID == other.parentID &&
                this.firstAncestorID == other.firstAncestorID &&
                this.birthDate == other.birthDate &&
                this.parentIsLight == other.parentIsLight &&
                this.life == other.life &&
                this.isLight == other.isLight &&
                this.energy == other.energy &&
                this.position[0] == other.position[0] &&
                this.position[1] == other.position[1];
    }

}
