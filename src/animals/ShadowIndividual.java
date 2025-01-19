package animals;

import communication.MyLog;

/**
 * A class for individuals of the shadow model.
 */
public class ShadowIndividual extends IndividualWithProperties {

    public ShadowIndividual(double x, double y, int glID, int ancestor, int date, int parent) {
        super(x, y, glID, ancestor, date, parent);
    }

    public ShadowIndividual(int myId, String line) {
        super(myId, line);
    }

    ShadowIndividual(IndividualWithProperties in, int glID, int date, double cst_mut_factor, int cst_speed_max,
                     double cst_light_birth_dst, double birth_dst, int cst_grid_max, int cst_energy_max
    ) {
        super(in, glID, date, cst_mut_factor, cst_speed_max, cst_light_birth_dst, birth_dst, cst_grid_max, cst_energy_max);
    }

    /**
     * Creates a shadow version of the given individual.
     * </br>
     * All fields of the given individual are copied for creating the ShadowIndividual.
     *
     * @param in the individual to copy
     */
    public ShadowIndividual(IndividualWithProperties in) {
        this(in.getPosition()[0], in.getPosition()[1], in.getID(), in.getFirstAncestorID(), in.getBirthDate(), in.getParentID());

        this.cellTransparency = in.cellTransparency;

        this.speed = in.speed;
        this.maxEnergy = in.maxEnergy;
        this.kidEnergy = in.kidEnergy;
        this.matForKids = in.matForKids;
        this.nKids = in.nKids;
        this.death = in.death;

        this.sensors = in.sensors.copy();
        this.life = in.life;
        this.isLight = in.isLight;
        this.energy = in.energy;
        this.color = in.color;
        this.borderColor = in.borderColor;
    }


    /**
     * Increments this individual's age.
     */
    public void update() {
        life++;
    }


    @Override
    protected MyLog createMyLog() {
        return new MyLog("shadow ind",true);
    }


}
