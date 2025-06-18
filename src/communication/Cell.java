package communication;

import animals.EmbodiedIndividual;

import java.util.LinkedList;

/**
 * a cell on the map
 * should have own class file.
 * @author lana
 *
 */
public class Cell {
    //cell's physical properties
    /**
     * how easy light goes through it (0=does not get out)
     */
    double transparency = 1;
    /**
     * how easy it is to move through (1=cannot move)
     */
    double density = 0;//TODO use. may also change how sound etc travels.

    /**
     * determined by animals and transparency on this cell
     */
    double luminosity;
    double sound;
    double smell;
    double temperature;
    double electric;

    /**
     * All creatures on this cell. Associates the creatures' IDs to the creatures themselves.
     */
    java.util.Map<Integer, EmbodiedIndividual> creatures;

    public Cell() {
        creatures = new java.util.HashMap<>();
    }

    public double getPhy(int kk) {
        double p = 0;
        switch (kk) {
            case 0: {
                p = transparency;
                break;
            }
            case 1: {
                p = density;
                break;
            }
            default:
                break;
        }
        return p;
    }

    /**
     * return a property of the cell
     */
    public double getProp(int k) {
        double r;
        //todo put all in an array
        switch (k) {
            case 0:
                r = luminosity;
                break;
            case 1:
                r = sound;
                break;
            case 2:
                r = smell;
                break;
            case 3:
                r = temperature;
                break;
            case 4:
                r = electric;
                break;
            default:
                r = 0;
                break;
        }
        return r;
    }
}