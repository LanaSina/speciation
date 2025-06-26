package com.alife.tolsim.animals;

import com.alife.tolsim.communication.MyLog;


/**
 * A class for individuals of the shadow model.
 */
public class ShadowIndividual extends IndividualWithProperties {


    /**
     * <p>
     *      Creates a shadow version of the given individual.
     * </p>
     * <p>
     *      All fields of the given individual are copied for creating the ShadowIndividual.
     * </p>
     *
     * @param in the individual to copy
     */
    public ShadowIndividual(IndividualWithProperties in) {
        this(in.getID(), in.getFirstAncestorID(), in.getBirthDate(), in.getParentID());

        this.speed = in.speed;
        this.maxEnergy = in.maxEnergy;
        this.kidEnergy = in.kidEnergy;
        this.matForKids = in.matForKids;
        this.nKids = in.nKids;
        this.death = in.death;

        this.sensors = in.sensors.copy();
        this.parentIsLight = in.parentIsLight;
        this.life = in.life;
        this.isLight = in.isLight;
        this.energy = in.energy;
    }


    /**
     * creates "light" at specified postion
     *
     * @param glID     id to give to this individual
     * @param ancestor id of the 1st ancestor
     * @param date     in-simulation time
     * @param parent   parent id
     */
    public ShadowIndividual(int glID, int ancestor, int date, int parent) {
        super(glID, ancestor, date, parent);
    }


    /**
     * clones with mutations
     *
     * @param in   individual to be cloned
     * @param glID id of this one
     */
    public ShadowIndividual(ShadowIndividual in, int glID, int date, double cst_mut_factor, int cst_speed_max,
                            double cst_light_birth_dst, double birth_dst, int cst_grid_max, int cst_energy_max
    ) {
        super(in, glID, date, cst_mut_factor, cst_speed_max, cst_light_birth_dst, birth_dst, cst_grid_max, cst_energy_max);
    }

    /**
     * Increments this individual's age.
     */
    public void update() {
        life++;
    }

    protected MyLog createMyLog() {
        return new MyLog("shadow ind",true);
    }

}
