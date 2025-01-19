package animals;

import java.awt.Color;
import java.util.LinkedList;

import communication.MyLog;

import static java.lang.Math.*;

public class EmbodiedIndividual extends IndividualWithProperties {


    public EmbodiedIndividual(double x, double y, int glID, int ancestor, int date, int parent) {
        super(x, y, glID, ancestor, date, parent);
    }

    public EmbodiedIndividual(int myId, String line) {
        super(myId, line);
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
    @Override
    public boolean update(LinkedList<Individual> babies, int date, double transparency, double cst_mut_factor, int cst_speed_max,
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


    @Override
    protected MyLog createMyLog() {
        return new MyLog("embodied ind",true);
    }

}
