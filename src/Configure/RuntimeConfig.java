package Configure;

import java.io.File;

public class RuntimeConfig {
    public static String runtimeVfile;
    public static String runtimePfile;
    public static String runtimeTfile;
    public static String runtimeProj;
    public static String resDir;
    public static double defaultTh = 0.5;
    public static boolean useDefUseRefinement = true;

    public static void mkdirs(String path) {
        File f = new File(path);
        if (!f.exists()) {
            f.mkdirs();
        }
    }

    public static void setRuntimeDir(String path) {
        String runtimePath = path + "/runtimeFile";
        String resPath = path + "/res";
        mkdirs(runtimePath);
        mkdirs(resPath);
        runtimeTfile = path + "/runtimeFile/tfile.java";
        runtimeVfile = path + "/runtimeFile/vfile.java";
        runtimePfile = path + "/runtimeFile/pfile.java";
        runtimeProj = path + "/unzip_project";
        resDir = path + "/res.json";
    }
}
