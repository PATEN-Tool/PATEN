package TokenAnalysis.TokenAna.Tools;

import com.github.difflib.DiffUtils;
import com.github.difflib.algorithm.DiffException;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Chunk;
import com.github.difflib.patch.Patch;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class getDiffLines {

    public static List<List<Integer>> compare(String s, String t) throws IOException, DiffException {
        File src = new File(s);
        File tar = new File(t);
        List<List<Integer>> result = new ArrayList();
        List<Integer> addLines = new ArrayList<>();
        List<Integer> delLines = new ArrayList<>();
        List<String> original = IOUtils.readLines(new FileInputStream(src), "UTF-8");
        List<String> revised = IOUtils.readLines(new FileInputStream(tar), "UTF-8");
        Patch<String> diff = DiffUtils.diff(original, revised);
        List<AbstractDelta<String>> deltas = diff.getDeltas();
        deltas.forEach(delta -> {
            switch (delta.getType()) {
                case INSERT:
                    Chunk<String> insert = delta.getTarget();
                    int insert_position = insert.getPosition();
                    for (int i = 1; i <= insert.getLines().size(); i++) {
                        addLines.add(insert_position + i);
                    }
                    break;
                case CHANGE:
                    Chunk<String> source = delta.getSource();
                    Chunk<String> target1 = delta.getTarget();
                    int delete_position2 = source.getPosition();
                    int insert_position2 = target1.getPosition();
                    for (int i = 1; i <= source.getLines().size(); i++) {
                        delLines.add(delete_position2 + i);
                    }
                    for (int i = 1; i <= target1.getLines().size(); i++) {
                        addLines.add(insert_position2 + i);
                    }
                    break;
                case DELETE:
                    Chunk<String> delete = delta.getSource();
                    int delete_position = delete.getPosition();
                    for (int i = 1; i <= delete.getLines().size(); i++) {
                        delLines.add(delete_position + i);
                    }
                    break;
                case EQUAL:
                    break;
            }

        });
        result.add(addLines);
        result.add(delLines);
        return result;
    }

    public static void main(String[] args) throws IOException, DiffException {
        String srcFile = "/home/usr1/Experiment_tools/JarTestData/dataset_noparam/SNYK-JAVA-ORGAPACHETOMCATEMBED-30965/uninfluenced_package/9eae33/AjpNioProcessor.java/vfile.java.java";
        String tarFile = "/home/usr1/Experiment_tools/JarTestData/dataset_noparam/SNYK-JAVA-ORGAPACHETOMCATEMBED-30965/uninfluenced_package/9eae33/AjpNioProcessor.java/pfile.java";
        List<List<Integer>> result = compare(srcFile, tarFile);
        System.out.println("add:" + result.get(0));
        System.out.println("del:" + result.get(1));
    }
}
