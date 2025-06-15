package communication;

import animals.*;
import startup.Constants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
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

    java.util.Map<Integer, ShadowIndividual> map;
    //for updates
    //for new ones
    LinkedList<ShadowIndividual> babies;
    //for dead ones
    LinkedList<ShadowIndividual> remove;

    /** The real map */
    RealMap realMap;

    /**
     * Creates a shadow version of the provided map.
     * </br>
     * This shadow map is initially similar to the provided map :
     * <ul>
     *     <li>same dimensions</li>
     *     <li>same number of individuals</li>
     *     <li>similar individuals (although they are not identical, see class <code>ShadowIndividual</code>)</li>
     *     <li>similar <code>babies</code>, <code>remove</code>, <code>moved</code> and <code>newPositions</code>
     *         (meaning they contain similar individuals)</li>
     *     <li>same global ID</li>
     *     <li>same time</li>
     *     <li>same data folder name</li>
     *     <li>same constants (e.g. mutation factor, speed factor, max speed, etc.)</li>
     * </ul>
     *
     * @param realMap           the real map
     * @param summaryFileName   the name of the individuals summary file
     * @param predationFileName the name of the predation file
     * @param snapshotFileName  the name of the snapshot file
     * @param sensorsFileName   the name of the sensors file
     */
    public ShadowMap(RealMap realMap,
                     String summaryFileName,
                     String predationFileName,
                     String snapshotFileName,
                     String sensorsFileName) {
        super(realMap.dataFolderName, summaryFileName, predationFileName, snapshotFileName, sensorsFileName);

        map = new HashMap<>();

        //for new ones
        babies = new LinkedList<ShadowIndividual>();
        //for dead ones
        remove = new LinkedList<ShadowIndividual>();

        // read configuration file
        Properties properties = new Properties();
        FileInputStream propsFile;
        try {
            propsFile = new FileInputStream("src/config.properties");
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
        globalID = realMap.globalID;
        time = realMap.time;
        map.clear();
        babies.clear();
        remove.clear();
        for (IndividualWithProperties realInd : realMap.getAllIndividuals()) {
            ShadowIndividual shadowInd = new ShadowIndividual(realInd);
            addIndividual(shadowInd);
            if (realMap.remove.contains(realInd))
                remove.add(shadowInd);
        }
        for (IndividualWithProperties realBaby : realMap.babies)
            babies.add((ShadowIndividual) new ShadowIndividual(realBaby));
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
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // predation info
        FileBuilder fb_predation = new FileBuilder(dataFolderName, predationFileName+ "_" + time);
        predationWriter = fb_predation.getFileWriter();
        fb_predation = null;

			/*
			String description =  ID +","+parentID+","+birthDate+","+life+","
				+ speed+","+maxEnergy+","+ getKidEnergy()+","
				+ hasSensors() +","+ getAncestor() + "," + getNKids() + ","
				+ death + ","+ matForKids ;
			 */
        String header_predation = "t, pred_id, pred_isLight," +
                "pred_parent, pred_created, pred_lifeSpan," +
                "pred_speed, pred_maxEnergy, pred_kidEnergy, pred_sensors," +
                "pred_ancestor, pred_nkids, pred_pgmDeath, pred_matForKids, pred_energy, pred_parentIsLight" +
                "prey_id, prey_isLight," +
                "prey_parent, prey_created, prey_lifeSpan," +
                "prey_speed, prey_maxEnergy, prey_kidEnergy, prey_sensors," +
                "prey_ancestor, prey_nkids, prey_pgmDeath, prey_matForKids, prey_energy, prey_parentIsLight" +
                "\n";

        try {
            predationWriter.append(header_predation);
            predationWriter.flush();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void updateMoved() {
        time++;

        if(Constants.Save && (time%5000 == 0)){
            mlog.say("backup all logs");
            setupLogFiles();
        }

        //update dead
        for(int i=0; i<remove.size();i++) {
            ShadowIndividual creature = remove.get(i);
            if (Constants.Save) {
                if (Constants.uniformDouble() < 1) { //0.01
                    if (!creature.isLight() & !creature.parentIsLight()) {
                        //write down info
                        String str = creature.stringDesc() + "\n";
                        try {
                            summaryWriter.append(str);
                            summaryWriter.flush();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
            }
            //remove from map
            removeIndividual(creature);
        }

        //add new babies
        for (int i = 0; i < babies.size(); i++) {
            ShadowIndividual baby = babies.get(i);
            baby.setID(++globalID);
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
            e.printStackTrace();
        }
    }

    public String saveSate(String dataFolderName) {
        //snapshot time
        DateFormat dateFormat = new SimpleDateFormat("dd_HH_mm");
        Date date = new Date();
        String strDate = dateFormat.format(date);

        File theDir = new File(dataFolderName+"/"+strDate);
        // if the directory does not exist, create it
        if (!theDir.exists()) {
            mlog.say("creating directory: " + dataFolderName);
            boolean result = false;
            try{
                theDir.mkdir();
                result = true;
            }
            catch(SecurityException se){
            }
            if(result) {
                System.out.println("DIR created");
            }
        }

        // move config file (todo: path in constants)
        Path src = Paths.get("src/config.properties");
        Path target = Paths.get(dataFolderName+"/"+strDate+"/config.properties");
        try {
            Files.copy(src, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        mlog.say("properties copied to " + dataFolderName+"/"+strDate);

        String filePath = strDate + "/" + snapshotFileName;
        saveCreatures(dataFolderName, filePath);
        // save sensors
        filePath = strDate + "/" + sensorsFileName;
        saveSensors(dataFolderName, filePath);

        return filePath;
    }

    void saveSensors(String dataFolderName, String filePath) {
        //open file for creatures
        FileBuilder fb = new FileBuilder(dataFolderName, filePath);
        FileWriter stateWriter = fb.getFileWriter();
        fb = null;

        //csv file header
        String str = "creatureID,sensorId,sensorValue,action"+"\n";
        //String debugstr = "";
        try {
            stateWriter.append(str);
            stateWriter.flush();

            for (ShadowIndividual creature : map.values()){
                if (creature.hasSensors()==0){
                    continue;
                }

                str = "";
                //tree nodes: root-> n properties -> detectionValue -> action
                Tree sensors = creature.getSensors();
                HashMap<Integer, Node> sensorProps = sensors.properties;
                for (Iterator<Integer> propIt = sensorProps.keySet().iterator(); propIt.hasNext();){
                    int prop = propIt.next();
                    Node detectionValuesNode = sensorProps.get(prop);
                    HashMap<Integer, Integer> detectionValues = detectionValuesNode.getChildren();
                    for (Iterator<Integer> senseIt = detectionValues.keySet().iterator(); senseIt.hasNext();){
                        int sensedValue = senseIt.next();
                        int action =  detectionValues.get(sensedValue);
                        // "creatureID,property,sensorValue,action"+"\n";
                        str = str + creature.getID() + "," + prop + "," + sensedValue + "," + action + "\n";
                    }
                }
                stateWriter.append(str);
                stateWriter.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the list of individuals on this map.
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

    /**
     * Make <code>number</code> random individuals have a baby.
     * </br>
     * An individual may make several babies if they are selected multiple times. They can even make more baby than
     * their normal maximum number of babies per time step.
     *
     * @param number the number of individuals to create
     */
    public void createRandomIndividuals(int number) {
        ArrayList<ShadowIndividual> allIndividuals = getAllIndividuals();
        Random randomizer = new Random();
        for (int i = 0 ; i < number ; i++) {
            int randomIndex = randomizer.nextInt(allIndividuals.size());
            ShadowIndividual randomInd = allIndividuals.get(randomIndex);
            ShadowIndividual baby = new ShadowIndividual(randomInd, -1, time, cst_mut_factor, cst_speed_max, cst_light_birth_dst, cst_birth_dst, 0 /* valeur sentinelle */, cst_energy_max);
            babies.add(baby);
        }
    }

    /**
     * Remove <code>number</code> random individuals.
     * </br>
     * The provided number must not exceed the total number of individuals.
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
