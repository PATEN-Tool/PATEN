package Gumtree.main;

import Configure.RuntimeConfig;
import Gumtree.DataObj.List_Structure;
import Gumtree.function.tools.*;
import com.github.gumtreediff.client.Run;
import com.github.gumtreediff.gen.TreeGenerators;
import com.github.gumtreediff.tree.Tree;
import Gumtree.function.similaritycalc.GraphBasedSim.GraphSimilarity;
import Gumtree.function.similaritycalc.GraphBasedSim.getGraph;
import Preprocess.DataRecover;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphBasedMain {


    public static Map<String, String> run_gumtree_ana(String target_file, DataRecover astInfo) throws Exception {
        Map<String, String> res = new HashMap<>();  // analysis result
        // get diff of code change
        String[] Del_Add = getDiff(FileReaders.readFileToString(RuntimeConfig.runtimeVfile), FileReaders.readFileToString(RuntimeConfig.runtimePfile));
        Map<String, Integer> tokenChange = TokenAnalysis.tokenAna(Del_Add[0], Del_Add[1]); //  get changed tokens

        List<List_Structure> in_list = astInfo.add_list;
        List<List_Structure> del_list = astInfo.del_list;
        if (in_list.size() == 0 && del_list.size() == 0) {  //if all lists are empty, nothing changed
            res.put("Analysis Res", "NoChange");
            return res;
        }
        Run.initGenerators();
        Tree targetTree = TreeGenerators.getInstance().getTree(target_file).getRoot(); // get target tree
        // get graphs of VFile,PFile,TFile((g.VGraph, g.PGraph, g.TGraph, g.VTmap, g.PTmap)
        getGraph g = new getGraph(in_list, del_list, targetTree, astInfo.vTree, astInfo.pTree);


        boolean isOnlyAddOrRemove = in_list.size() * del_list.size() == 0;
        if (!isOnlyAddOrRemove && g.no_target_flag && RuntimeConfig.useDefUseRefinement) { // if no target subtree found
            doSlicing s = new doSlicing(RuntimeConfig.runtimeVfile, RuntimeConfig.runtimePfile, RuntimeConfig.runtimeTfile); // do slicing
            SlicingGraph slicingGraph; //calculate similarity of origin and target sliced features
            if (s.addFlag)
                slicingGraph = new SlicingGraph(s.slicedLine, s.def_use, astInfo.pTree, targetTree, tokenChange);
            else
                slicingGraph = new SlicingGraph(s.slicedLine, s.def_use, astInfo.vTree, targetTree, tokenChange);
            if (slicingGraph.NormDistance != 1.0) {
                res.put("Tokens", "");
                for (String tmp : s.def_use.keySet()) {
                    res.put("Tokens", res.get("Tokens") + "-" + tmp);
                }
                res.put("Step", "3");

                if (s.addFlag)
                    res.put("Analysis Res", "PT NormalizedDistance is " + slicingGraph.NormDistance);  // distance of sliced part, less than threshold means vulnerable
                else
                    res.put("Analysis Res", "VT NormalizedDistance is " + slicingGraph.NormDistance);  // distance of sliced part, more than threshold means vulnerable
                return res;
            }
        }

        boolean AnaRes;
        GraphSimilarity graphSimilarity = new GraphSimilarity(g.VGraph, g.PGraph, g.TGraph, g.VTmap, g.PTmap, tokenChange); // calculate graph similarity
        res.put("Step", "2");
        if (graphSimilarity.Ttotal != 0) {
            double defaultTh = RuntimeConfig.defaultTh;
            if (graphSimilarity.Vtotal == 0 && graphSimilarity.Ptotal != 0) {
                if (graphSimilarity.PTsim <= defaultTh) {
                    res.put("Analysis Res", "false");
                    res.put("PT Edit distance", String.valueOf(graphSimilarity.PTsim));
                } else {
                    res.put("Analysis Res", "true");
                    res.put("PT Edit distance", String.valueOf(graphSimilarity.PTsim));
                }
            } else if (graphSimilarity.Vtotal != 0 && graphSimilarity.Ptotal == 0) {
                if (graphSimilarity.VTsim <= defaultTh) {
                    res.put("Analysis Res", "true");
                    res.put("VT Edit distance", String.valueOf(graphSimilarity.VTsim));
                } else {
                    res.put("Analysis Res", "false");
                    res.put("VT Edit distance", String.valueOf(graphSimilarity.VTsim));
                }
            } else {
                if (graphSimilarity.VTsim < graphSimilarity.PTsim)
                    res.put("Analysis Res", "true");
                else
                    res.put("Analysis Res", "false");
                res.put("VT Edit distance", String.valueOf(graphSimilarity.VTsim));
                res.put("PT Edit distance", String.valueOf(graphSimilarity.PTsim));
            }

        } else {
            if (graphSimilarity.Vtotal == 0) {
                res.put("Analysis Res", "true");
                res.put("PT Edit distance", String.valueOf(graphSimilarity.PTsim));

            } else if (graphSimilarity.Ptotal == 0) {
                res.put("Analysis Res", "false");
                res.put("VT Edit distance", String.valueOf(graphSimilarity.VTsim));
            } else {
                if (graphSimilarity.Vtotal < graphSimilarity.Ptotal)
                    res.put("Analysis Res", "true");
                else
                    res.put("Analysis Res", "false");
            }
        }
        return res;


    }

    private static String[] getDiff(String vulcontent, String patcontent) {
        String[] DelAdd = new String[2];

        DelAdd[0] = getDiffString(vulcontent.split("\n"), patcontent.split("\n"));
        DelAdd[1] = getDiffString(patcontent.split("\n"), vulcontent.split("\n"));
        return DelAdd;
    }

    private static String getDiffString(String[] ori, String[] tgt) {
        String s = "";
        for (String a : ori) {
            boolean flag = false;
            for (String tmp : tgt)
                if (tmp.equals(a)) {
                    flag = true;
                    break;
                }
            if (!flag) {
                s += a;
            }
        }
        return s;
    }

}
