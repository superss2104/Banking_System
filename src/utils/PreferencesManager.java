//package utils;
//
//import java.util.prefs.Preferences;
//
//public class PreferencesManager {
//    private static final String PREF_NODE = "/com/example/bankingsystem";
//    private final Preferences prefs;
//
//    public PreferencesManager() {
//        prefs = Preferences.userRoot().node(PREF_NODE);
//    }
//
//    public void setPreference(String key, String value) {
//        prefs.put(key, value);
//    }
//
//    public String getPreference(String key, String defaultValue) {
//        return prefs.get(key, defaultValue);
//    }
//
//    public void removePreference(String key) {
//        prefs.remove(key);
//    }
//
//    public void clearPreferences() {
//        try {
//            prefs.clear();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//}