package com.alife.tolsim.oee_analysis;

import com.alife.tolsim.animals.IndividualWithProperties;
import com.alife.tolsim.communication.Map;

import java.io.*;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * <p>
 *      A class for saving deltas (i.e. activity increments) into a file.
 * </p>
 * <p>
 *      It is used for steps 1 to 3 of Channon's procedure to analyze the open-endedness of evolutionary systems.
 * </p>
 */
public class DeltasSaver {

    /** The map to analyze. */
    private final Map map;
    /** Folder where to put the file. */
    private final String dataFolderName;
    /** The name of the file to save. */
    private final String fileName;


    /** The deltas of the real model at a given time stamp. */
    public HashMap<String, HashMap<Integer, Integer>> currDeltas;
    /** The number of components that have emerged so far. */
    public int numberOfComponents;
    /** The number of times this DeltaSaver has been updated (see <code>update()</code>). */
    public int numberOfIterations;
    /** The list of components that are new at a given time. A component can only be new during one time step. */
    public List<String> newComponents;
    /** Mappings between components and the index of their dedicated column in the file. */
    public HashMap<String, Integer> columnsIndexes;

    /** Separates components' names from their values in the CSV */
    private static final String SEPARATOR = "_";

    /**
     * Builds a DeltaSaver.
     *
     * @param map the map to analyze
     * @param dataFolderName the name of the folder where to save the deltas
     * @param fileName the name of the file where to save the deltas
     */
    public DeltasSaver(Map map, String dataFolderName, String fileName) {
        this.map = map;
        this.dataFolderName = dataFolderName;
        this.fileName = fileName;
        this.currDeltas = initCurrDeltas();
        this.numberOfComponents = 0;
        this.numberOfIterations = 0;
        this.newComponents = new LinkedList<>();
        this.columnsIndexes = new HashMap<>();
        createFile();
    }

    /**
     * Creates the file where to save the deltas.
     */
    private void createFile() {
        // create the file
        String filePath = Paths.get(dataFolderName, fileName).toString();
        try {
            FileWriter fileWriter = new FileWriter(filePath);
            // write the file's header
            fileWriter.append("t\r\n");
            fileWriter.close();
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    /**
     * Creates a hash map used to store the current deltas.
     *
     * @return the created hash map
     */
    private HashMap<String, HashMap<Integer, Integer>> initCurrDeltas() {
        HashMap<String, HashMap<Integer, Integer>> res = new HashMap<>();
        res.put("speed", new HashMap<>());
        res.put("maxEnergy", new HashMap<>());
        res.put("kidEnergy", new HashMap<>());
        res.put("nKids", new HashMap<>());
        res.put("death", new HashMap<>());
        res.put("matForKids", new HashMap<>());
        return res;
    }

    /**
     * Computes current deltas and write them in the file.
     */
    public void update() {
        updateDeltas(currDeltas, map);
        addColumns();
        newComponents.clear();
        int[] deltasArray = generateDeltasArray();
        String line = map.getTime()
                      + Arrays.stream(deltasArray)
                                            .mapToObj(d -> "," + d)
                                            .collect(Collectors.joining())
                      + "\r\n";
        String filePath = Paths.get(dataFolderName, fileName).toString();
        try {
            FileWriter fileWriter = new FileWriter(filePath, true);
            fileWriter.append(line);
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            throw new RuntimeException();
        }
        numberOfIterations++;
    }

    /**
     * <p>
     *      Updates the deltas.
     * </p>
     * <p>
     *      Also stores new components in newComponents and updates columnsIndexes to associate new components to a column
     *      index.
     * </p>
     *
     * @param deltas the deltas to update
     * @param map the associated map
     */
    private void updateDeltas(HashMap<String, HashMap<Integer, Integer>> deltas, Map map) {
        // reinitialize the deltas
        deltas.forEach((property, values) -> values.replaceAll((k, v) -> 0));

        List<String> properties = List.of("speed", "maxEnergy", "kidEnergy", "nKids", "death", "matForKids");
        for (IndividualWithProperties individual : map.getAllEvolvedIndividuals()) {
            for (String property : properties) {
                int value = getPropertyValue(individual, property);
                if (!deltas.get(property).containsKey(value)) {
                    String componentString = property + SEPARATOR + value;
                    newComponents.add(componentString);
                    columnsIndexes.put(componentString, numberOfComponents++);
                }
                deltas.get(property).put(value, 1);
            }
        }
    }

    /**
     * Gives the property value associated to the given property name.
     *
     * @param individual the individual of which the property is to be retrieved
     * @param property the property to retrieve
     * @return the property value associated to the given property name
     */
    private int getPropertyValue(IndividualWithProperties individual, String property) {
        return switch (property) {
            case "speed" -> individual.speed;
            case "maxEnergy" -> individual.getMaxEnergy();
            case "kidEnergy" -> individual.getKidEnergy();
            case "nKids" -> individual.getNKids();
            case "death" -> individual.death;
            case "matForKids" -> individual.getMatForKids();
            default -> throw new IllegalArgumentException("Invalid property: " + property);
        };
    }

    /**
     * <p>
     *      For each new component, adds a new column, that is:
     *      <ul>
     *          <li>adds a header</li>
     *          <li>fills all the cells under it with 0s until the current line is reached</li>
     *      </ul>
     * </p>
     * <p>
     *      Do so by creating a new file, deleting the old one, then renaming the new file to the name of the old one.
     * </p>
     */
    private void addColumns() {
        File oldFile = new File(dataFolderName, fileName);
        File newFile = new File(dataFolderName, fileName + ".tmp");

        try (BufferedReader reader = new BufferedReader(new FileReader(oldFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(newFile, true))
        ) {
            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    line += newComponents.stream()
                                         .map(s -> "," + s)
                                         .collect(Collectors.joining());
                    isFirstLine = false;
                } else {
                    line += ",0".repeat(newComponents.size());
                }
                // write the new line in the new file
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Error while adding columns to file " + fileName);
        }

        // replace the old file by the new one
        if (!oldFile.delete())
            throw new RuntimeException("Could not delete the old file " + fileName);
        if (!newFile.renameTo(oldFile))
            throw new RuntimeException("Could not rename the new file " + fileName + ".tmp to " + fileName);
    }

    /**
     * Gives the current deltas, in the order of columnsIndexes.
     *
     * @return the array of deltas
     */
    private int[] generateDeltasArray() {
        int[] deltasArray = new int[numberOfComponents];
        for (HashMap.Entry<String, Integer> entry: columnsIndexes.entrySet()) {
            String component = entry.getKey();
            String[] componentParts = component.split(SEPARATOR);
            String componentDimension = componentParts[0];
            int componentValue = Integer.parseInt(componentParts[1]);
            int columnIndex = entry.getValue();
            int delta = currDeltas.get(componentDimension).get(componentValue);
            deltasArray[columnIndex] = delta;
        }
        return deltasArray;
    }

}
