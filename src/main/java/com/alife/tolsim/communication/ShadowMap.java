package com.alife.tolsim.communication;

import com.alife.tolsim.animals.*;
import com.alife.tolsim.startup.Constants;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * A class for shadow maps.
 */
public class ShadowMap extends Map {

    java.util.Map<Long, ShadowIndividual> map;
    //for updates
    //for new ones
    LinkedList<ShadowIndividual> babies;
    //for dead ones
    LinkedList<ShadowIndividual> remove;

    /** The real map */
    RealMap realMap;

    /**
     * <p>
     *      Creates a shadow version of the provided map.
     * </p>
     * <p>
     *      This shadow map is initially similar to the provided map :
     *      <ul>
     *          <li>same dimensions</li>
     *          <li>same number of individuals</li>
     *          <li>similar individuals (although they are not identical, see class <code>ShadowIndividual</code>)</li>
     *          <li>similar <code>babies</code>, <code>remove</code>, <code>moved</code> and <code>newPositions</code>
     *              (meaning they contain similar individuals)</li>
     *          <li>same global ID</li>
     *          <li>same time</li>
     *          <li>same data folder name</li>
     *          <li>same constants (e.g. mutation factor, speed factor, max speed, etc.)</li>
     *      </ul>
     * </p>
     *
     * @param realMap           the real map
     * @param summaryFileName   the name of the individuals summary file
     * @param snapshotFileName  the name of the snapshot file
     * @param sensorsFileName   the name of the sensors file
     */
    public ShadowMap(RealMap realMap,
                     String summaryFileName,
                     String snapshotFileName,
                     String sensorsFileName) {
        super(realMap.dataFolderName, summaryFileName, snapshotFileName, sensorsFileName);

        map = new HashMap<>();

        //for new ones
        babies = new LinkedList<>();
        //for dead ones
        remove = new LinkedList<>();

        // read configuration file
        Properties properties = new Properties();
        InputStream propsFile;
        try {
            propsFile = getClass().getClassLoader().getResourceAsStream("config.properties");
            properties.load(propsFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        cst_mut_factor = Double.parseDouble(properties.getProperty("mut_factor"));
        cst_speed_factor = Double.parseDouble(properties.getProperty("speed_factor"));
        cst_speed_max = Integer.parseInt(properties.getProperty("speed_max"));
        cst_light_birth_dst = Double.parseDouble(properties.getProperty("light_birth_dst"));
        cst_birth_dst = Double.parseDouble(properties.getProperty("birth_dst"));
        cst_energy_max = Integer.parseInt(properties.getProperty("energy_max"));
        cst_speed_cost = Double.parseDouble(properties.getProperty("speed_cost"));
        cst_sensor_cost = Double.parseDouble(properties.getProperty("sensor_cost"));
        cst_error_cost = Double.parseDouble(properties.getProperty("error_cost"));
        cst_free_energy = Integer.parseInt(properties.getProperty("free_energy"));
        cst_sensor_precision = Integer.parseInt(properties.getProperty("sensor_precision"));
        cst_max_number_actions = Integer.parseInt(properties.getProperty("max_number_actions"));
        cst_energy_cost_factor = Double.parseDouble(properties.getProperty("energy_cost_factor"));
        cst_step_cost = Double.parseDouble(properties.getProperty("step_cost"));

        this.realMap = realMap;
        this.reset();
    }

    /**
     * Reset this shadow map's state the real map's state:
     * <ul>
     *     <li>same global ID</li>
     *     <li>same time</li>
     *     <li>same number of individuals</li>
     *     <li>similar individuals (although they are different, see class <code>ShadowIndividual</code>)</li>
     *     <li>similar <code>babies</code>, <code>remove</code>, <code>moved</code> and <code>newPositions</code>
     *         (meaning they contain similar individual</li>
     * </ul>
     */
    public void reset() {
        globalID.set(realMap.globalID.get());
        time = realMap.time;
        map.clear();
        babies.clear();
        remove.clear();
        for (IndividualWithProperties realInd : realMap) {
            ShadowIndividual shadowInd = new ShadowIndividual(realInd);
            addIndividual(shadowInd);
            if (realMap.remove.containsKey(realInd.getID()))
                remove.add(shadowInd);
        }
        for (IndividualWithProperties realBaby : realMap.babies)
            babies.add(new ShadowIndividual(realBaby));
    }

    public void setupLogFiles(){
        // individuals info
        FileBuilder fb = new FileBuilder(dataFolderName, summaryFileName + "_" + time);
        summaryWriter = fb.getFileWriter();
        fb = null;

        //csv file header
			/*
			String description =  ID +","+parentID+","+birthDate+","+life+","
				+ speed+","+maxEnergy+","+ getKidEnergy()+","
				+ hasSensors() +","+ getAncestor() + "," + getNKids() + ","
				+ death + ","+ matForKids ;
			 */
        String str = "ID,isLight,parent,created,lifeSpan,speed,maxEnergy," +
                "kidEnergy,sensors,ancestor,nkids,pgmDeath,matForKids,energy,parentIsLight"+"\n";
        try {
            summaryWriter.append(str);
            summaryWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void applyChanges() {
        //update dead
        for (ShadowIndividual creature : remove) {
            if (Constants.Save && Constants.uniformDouble() <= Constants.SaveCoarse) {
                if (!creature.isLight() & !creature.parentIsLight()) {
                    //write down info
                    String str = creature.stringDesc() + "\n";
                    try {
                        summaryWriter.append(str);
                        summaryWriter.flush();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            //remove from map
            removeIndividual(creature);
        }

        //add new babies
        for (ShadowIndividual baby : babies) {
            baby.setID(globalID.incrementAndGet());
            addIndividual(baby);
        }

        //clear
        //for new ones
        babies.clear();
        //for dead ones
        remove.clear();
    }

    void saveCreatures(String dataFolderName, String filePath){
        //open file for creatures
        FileBuilder fb = new FileBuilder(dataFolderName, filePath);
        FileWriter stateWriter = fb.getFileWriter();
        fb = null;


        try {
            // save time
            String str = "time\n"+ time + "\n";
            stateWriter.append(str);
            stateWriter.flush();
            //csv file header
			/*
			"ID,parent,created,lifeSpan,speed,maxEnergy,kidEnergy,sensors,ancestor,nkids,pgmDeath,matForKids"+"\n";
			 */
            str = "ID,isLight,parent,created,lifeSpan,speed,maxEnergy,kidEnergy," +
                    "sensors,ancestor,nkids,pgmDeath,matForKids,energy,parentIsLight"+"\n";

            stateWriter.append(str);
            stateWriter.flush();

            for (ShadowIndividual creature : map.values()){
                String astr = creature.stringDesc() + "\n";
                stateWriter.append(astr);
                stateWriter.flush();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveState(String dataFolderName) {
        //snapshot time
        DateFormat dateFormat = new SimpleDateFormat("dd_HH_mm_ss");
        Date date = new Date();
        String strDate = dateFormat.format(date);

        File theDir = new File(dataFolderName+"/"+strDate);
        // if the directory does not exist, create it
        if (!theDir.exists()) {
            mlog.say("creating directory: " + dataFolderName);
            try{
                theDir.mkdir();
                System.out.println("DIR created");
            }
            catch(SecurityException e){
                throw new RuntimeException(e);
            }
        }


        String filePath = strDate + "/" + snapshotFileName;
        saveCreatures(dataFolderName, filePath);
        // save sensors
        filePath = strDate + "/" + sensorsFileName;
        saveSensors(dataFolderName, filePath);

        String savedAt = dataFolderName + filePath;
        mlog.say("Saved at " + savedAt);
    }

    void saveSensors(String dataFolderName, String filePath) {
        //open file for creatures
        FileBuilder fb = new FileBuilder(dataFolderName, filePath);
        FileWriter stateWriter = fb.getFileWriter();
        fb = null;

        //csv file header
        StringBuilder str = new StringBuilder("creatureID,sensorId,sensorValue,action" + "\n");
        //String debugstr = "";
        try {
            stateWriter.append(str.toString());
            stateWriter.flush();

            for (ShadowIndividual creature : map.values()){
                if (creature.hasSensors() == 0)
                    continue;

                str = new StringBuilder();
                //tree nodes: root-> n properties -> detectionValue -> action
                Tree sensors = creature.getSensors();
                HashMap<Integer, Node> sensorProps = sensors.properties;
                for (int prop : sensorProps.keySet()) {
                    Node detectionValuesNode = sensorProps.get(prop);
                    HashMap<Integer, Integer> detectionValues = detectionValuesNode.getChildren();
                    for (int sensedValue : detectionValues.keySet()) {
                        int action = detectionValues.get(sensedValue);
                        // "creatureID,property,sensorValue,action"+"\n";
                        str.append(creature.getID()).append(",").append(prop).append(",").append(sensedValue).append(",").append(action).append("\n");
                    }
                }
                stateWriter.append(str.toString());
                stateWriter.flush();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * <p>
     *     Returns the list of individuals on this map.
     * </p>
     * <p>
     *     Time complexity and space complexity are in Θ(n), where n is the number of individuals of this map.
     * </p>
     *
     * @return the list of individuals on this map
     */
    public ArrayList<ShadowIndividual> getAllIndividuals() {
        return new ArrayList<>(map.values());
    }

    /**
     * Returns all individuals of this map that are non-light and whose parent is a non-light.
     *
     * @return all individuals of this map that are non-light and whose parent is a non-light
     */
    public List<IndividualWithProperties> getAllEvolvedIndividuals() {
        return new ArrayList<>(map.values()).stream()
                .filter(ind -> !ind.isLight() && !ind.parentIsLight)
                .map(ind -> (IndividualWithProperties) ind)
                .toList();
    }


    @Override
    public Iterator<IndividualWithProperties> iterator() {
        return map.values()
                  .stream()
                  .map(ind -> (IndividualWithProperties) ind)
                  .iterator();
    }


    /**
     * <p>
     *      Makes <code>number</code> random individuals have a baby.
     * </p>
     * <p>
     *      This method uses the "drawing with replacement" method, since each individual can make several babies at a
     *      given time step. Note that individuals can even make more baby than their normal maximum number of babies
     *      per time step.
     * </p>
     * <p>
     *      Space complexity is in Θ(n) and time complexity is in Θ(n+<code>number</code>), where n is the number of
     *      individuals on this map.
     * </p>
     *
     * @param number the number of individuals to create
     * @throws RuntimeException if we try to make more than 0 babies and this map contains no individual to make babies from
     */
    public void createRandomIndividuals(int number) {
        ArrayList<ShadowIndividual> allIndividuals = getAllIndividuals();
        if (number > 0 && allIndividuals.isEmpty())
            throw new RuntimeException("No individuals present to make babies");
        Random randomizer = new Random();
        for (int i = 0 ; i < number ; i++) {
            int randomIndex = randomizer.nextInt(allIndividuals.size());
            ShadowIndividual randomInd = allIndividuals.get(randomIndex);
            ShadowIndividual baby = new ShadowIndividual(randomInd, -1, time, cst_mut_factor, cst_speed_max, cst_light_birth_dst, cst_birth_dst, 0 /* valeur sentinelle */, cst_energy_max);
            babies.add(baby);
        }
    }

    /**
     * <p>
     *      Removes <code>number</code> random individuals.
     * </p>
     * <p>
     *      This method uses the "drawing without replacement" method, since each individual can only die once.
     * </p>
     * <p>
     *      Space complexity is in Θ(n) and time complexity is in Θ(n+<code>number</code>), where n is the number of
     *      individuals on this map.
     * </p>
     *
     * @param number the number of individuals to remove
     * @throws IllegalArgumentException if trying to remove more individuals than there are
     */
    public void removeRandomIndividuals(int number) {
        ArrayList<ShadowIndividual> allIndividuals = getAllIndividuals();
        if (allIndividuals.size() < number)
            throw new IllegalArgumentException("cannot remove more individuals than there are");
        Random randomizer = new Random();
        for (int i = 0 ; i < number ; i++) {
            int randomIndex = randomizer.nextInt(allIndividuals.size());
            ShadowIndividual randomIndividual = allIndividuals.get(randomIndex);
            remove.add(randomIndividual);
            // remove individual from list of individuals that can be removed
            int lastIndex = allIndividuals.size() - 1;
            allIndividuals.set(randomIndex, allIndividuals.get(lastIndex));
            allIndividuals.remove(lastIndex);
        }
    }

    public void addIndividual(ShadowIndividual in) {
        map.put(in.getID(), in);
    }

    private void removeIndividual(ShadowIndividual creature) {
        map.remove(creature.getID());
    }

    @Override
    protected MyLog createMyLog() {
        return new MyLog("Shadow map", true);
    }

    public void incrementAgeOfAllIndividuals() {
        for (ShadowIndividual ind : map.values())
            ind.update();
    }
}
