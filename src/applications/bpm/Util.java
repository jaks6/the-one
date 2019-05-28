package applications.bpm;

import core.DTNHost;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

import static applications.model.AdaptiveMobileApplication.COMBUS_PENDING_FOG_REQS;

public class Util {
    public static HashMap<String, String> PROCESSES =  new HashMap<>();

    public static String stringFromFile(String s) throws IOException {
        File process = new File(s);
        String string = readFile(process.getPath(), StandardCharsets.UTF_8);
        return string;
    }

    public static String getCachedProcess(String s) throws IOException {
        if (!PROCESSES.containsKey(s)) PROCESSES.put(s, readFile(s, StandardCharsets.UTF_8));

        return PROCESSES.get(s);
    }

    static String readFile(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    /** Gets the arraylist stored with the given key, or returns an empty arraylist if
     * no value attached to the key.
     * @param host
     * @param key
     * @return
     */
    public static <T> ArrayList<T> getArrayListFromComBus(DTNHost host, String key){
        if (host.getComBus().containsProperty(key)){
            return (ArrayList<T>) host.getComBus().getProperty(COMBUS_PENDING_FOG_REQS);
        } else {
            return new ArrayList<T>();
        }
    }

}
