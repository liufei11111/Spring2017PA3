package edu.stanford.cs276.test;

import edu.stanford.cs276.AScorer;
import edu.stanford.cs276.BM25Scorer;
import edu.stanford.cs276.BaselineScorer;
import edu.stanford.cs276.CosineSimilarityScorer;
import edu.stanford.cs276.Document;
import edu.stanford.cs276.ExtraCreditScorer;
import edu.stanford.cs276.LoadHandler;
import edu.stanford.cs276.Query;
import edu.stanford.cs276.SmallestWindowScorer;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;

import edu.stanford.cs276.util.Pair;
import java.util.Map.Entry;
import javax.print.Doc;

/**
 * The entry class for this programming assignment.
 */
public class CosinNdcgMain {

  /**
   * Call this function to score and rank documents for some queries,
   * using a specified scoring function.
   * @param queryDict
   * @param scoreType
   * @param idfs
   * @return a mapping of queries to rankings
   */
  /**
   * Call this function to score and rank documents for some queries,
   * using a specified scoring function.
   * @return a mapping of queries to rankings
   */
  private static Map<Query,List<Pair<Document,Double>>> score(
      Map<Query, Map<String, Document>> queryDict, String scoreType, Map<String, Double> idfs,
      List<Pair<Pair<Integer, Integer>, Double>> config,
      List<Pair<Pair<Integer, Integer>, Double>> additionalConfig) {
    AScorer scorer = null;
    if (scoreType.equals("baseline")) {
      scorer = new BaselineScorer();
    } else if (scoreType.equals("cosine")) {
      scorer = new CosineSimilarityScorer(idfs);
      CosineSimilarityScorer temp = (CosineSimilarityScorer) scorer;
      temp.urlweight = config.get(0).getSecond();
      temp.headerweight = config.get(1).getSecond();
      temp.bodyweight = config.get(2).getSecond();
      temp.headerweight = config.get(3).getSecond();
      temp.anchorweight = config.get(4).getSecond();
      temp.smoothingBodyLength = additionalConfig.get(0).getSecond();

    } else if (scoreType.equals("bm25")) {
      scorer = new BM25Scorer(idfs, queryDict);
    } else if (scoreType.equals("window")) {
      // feel free to change this to match your cosine scorer if you choose to build on top of that instead
      scorer = new SmallestWindowScorer(idfs, queryDict);
    } else if (scoreType.equals("extra")) {
      scorer = new ExtraCreditScorer(idfs);
    }
    // ranking result Map.
    Map<Query, List<Pair<Document,Double>>> queryRankings = new HashMap<>();
    //Map<Query,List<String>> queryRankings = new HashMap<Query,List<String>>();

    //loop through urls for query, getting scores
    for (Query query : queryDict.keySet()) {
      // Pair of url and ranked relevance.
      List<Pair<Document,Double>> docAndScores = new ArrayList<Pair<Document,Double>>(queryDict.get(query).size());
      for (String url : queryDict.get(query).keySet()) {
        Document doc = queryDict.get(query).get(url);
        //force debug string to be 1-line and truncate to only includes only first 200 characters for rendering purpose
        String debugStr = scorer.getDebugStr(doc, query).trim().replace("\n", " ");
        doc.debugStr = debugStr.substring(0, Math.min(debugStr.length(), 200));

        double score = scorer.getSimScore(doc, query);
        docAndScores.add(new Pair<Document, Double>(doc, score));
      }

      /* Sort urls for query based on scores. */
      Collections.sort(docAndScores, new Comparator<Pair<Document,Double>>() {
        @Override
        public int compare(Pair<Document, Double> o1, Pair<Document, Double> o2) {
          /*
           * TODO : Your code here
           * Define a custom compare function to help sort urls
           * urls for a query based on scores.
           */
          return o2.getSecond().compareTo(o1.getSecond());

        }
      });

      //put completed rankings into map
      queryRankings.put(query, docAndScores);
    }
    return queryRankings;
  }

  /**
   * Print ranked results.
   * @param queryRankings the mapping of queries to rankings
   */
  public static void printRankedResults(Map<Query,List<Document>> queryRankings) {
    for (Query query : queryRankings.keySet()) {
      StringBuilder queryBuilder = new StringBuilder();
      for (String s : query.queryWords) {
        queryBuilder.append(s);
        queryBuilder.append(" ");
      }

      System.out.println("query: " + queryBuilder.toString());
      for (Document res : queryRankings.get(query)) {
        System.out.println(
            "  url: " + res.url + "\n" +
                "    title: " + res.title + "\n" +
                "    debug: " + res.debugStr
        );
      }
    }
  }

  /**
   * Writes ranked results to file.
   * @param queryRankings the mapping of queries to rankings
   * @param outputFilePath the destination file path
   */
  public static void writeRankedResultsToFile(Map<Query,List<Document>> queryRankings,String outputFilePath) {
    try {
      File file = new File(outputFilePath);
      // If file doesn't exists, then create it
      if (!file.exists()) {
        file.createNewFile();
      }

      FileWriter fw = new FileWriter(file.getAbsoluteFile());
      BufferedWriter bw = new BufferedWriter(fw);

      for (Query query : queryRankings.keySet()) {
        StringBuilder queryBuilder = new StringBuilder();
        for (String s : query.queryWords) {
          queryBuilder.append(s);
          queryBuilder.append(" ");
        }

        String queryStr = "query: " + queryBuilder.toString() + "\n";
        System.out.print(queryStr);
        bw.write(queryStr);

        for (Document res : queryRankings.get(query)) {
          String urlString =
              "  url: " + res.url + "\n" +
                  "    title: " + res.title + "\n" +
                  "    debug: " + res.debugStr + "\n";
          System.out.print(urlString);
          bw.write(urlString);
        }
      }
      bw.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Main method for Rank.
   * args[0] : signal files for ranking query, url pairs
   * args[1] : ranking function choice from {baseline, cosine, bm25, extra, window}
   * args[2] : PA1 corpus to build idf values (when args[3] is true), or existing idfs file to load (when args[2] is false)
   * args[3] : true: build from PA1 corpus, false: load from existing idfs.
   */
  public static void main(String[] args) throws IOException {

    if (args.length != 5) {
      System.err.println("Insufficient number of arguments: <sigFile> <taskOption> <idfPath> <buildFlag>");
      return;
    }

    /* sigFile : args[0], path for signal file. */
    String sigPath = args[0];
    /* taskOption : args[1], baseline, cosine (Task 1), bm25 (Task 2), window (Task 3), or extra (Extra Credit). */
    String taskOption = args[1];
    /* idfPath : args[2].
       When buildFlag is "true", set this as your PA1 corpus path.
       When buildFlag is "false", set this as your existing idfs file.
     */
    String idfPath = args[2];
    /* buildFlag : args[3].
       Set to "true", will buid idf from PA1 corpus.
       Set to "flase", load from existing idfs file.
     */
    String buildFlag = args[3];

    /* start building or loading idfs information. */
    Map<String, Double> idfs = null;
    /* idfFile you want to dump or already stored. */
    String idfFile = "idfs";
    if (buildFlag.equals("true")) {
      idfs = LoadHandler.buildDFs(idfPath, idfFile);
    } else {
      idfs = LoadHandler.loadDFs(idfFile);
    }

    if (!(taskOption.equals("baseline") || taskOption.equals("cosine") || taskOption.equals("bm25")
        || taskOption.equals("extra") || taskOption.equals("window"))) {
      System.err.println("Invalid scoring type; should be either 'baseline', 'bm25', 'cosine', 'window', or 'extra'");
      return;
    }

    /* start loading query pages to be ranked. */
    Map<Query,Map<String, Document>> queryDict = null;
    /* Populate map with features from file */
    try {
      queryDict = LoadHandler.loadTrainData(args[0]);
    } catch (Exception e) {
      e.printStackTrace();
    }
    CosinNdcgMain test = new CosinNdcgMain();
    // <...>CosinNdcgMain data/pa3.signal.dev cosine pa1-data/ true <ref file>
    Map<String,Map<String,Double>> relevScores = test.genRelScore(args[4]);
////////////////////////////////// below the tunning code: /////////////////////////////
    // String[] TFTYPES = {"url","title","body","header","anchor"};

//    /* print out ranking result, keep this stdout format in your submission */
//    printRankedResults(queryRankings);
//
//    //print results and save them to file "ranked.txt" (to run with NdcgMain.java)
//    String outputFilePath = "ranked.txt";
//    writeRankedResultsToFile(queryRankings,outputFilePath);
    /////////////////////linear constraint init values///////////////
    List<Pair<String,Pair<Double,Double>>> parameters = new ArrayList<>();
    parameters.add(new Pair(BaseLineConfigTunner.TFTYPES[0],new Pair(0.0,1.0)));// url
    parameters.add(new Pair(BaseLineConfigTunner.TFTYPES[1],new Pair(0.0,1.0)));// title
    parameters.add(new Pair(BaseLineConfigTunner.TFTYPES[2],new Pair(0.0,1.0)));// body
    parameters.add(new Pair(BaseLineConfigTunner.TFTYPES[3],new Pair(0.0,1.0)));// header
    parameters.add(new Pair(BaseLineConfigTunner.TFTYPES[4],new Pair(0.0,1.0)));// anchor
    Double[] arrays = {0.1,0.1,0.1,0.1,0.1};
    List<Double> initialValues = new ArrayList<>(Arrays.asList(arrays));


    ///////////////////// additional init values///////////////
    List<Pair<String,Pair<Double,Double>>> additionalParams = new ArrayList<>();
    additionalParams.add(new Pair("SmoothingBodyLength",new Pair(0.0,1000.0)));
    Double[] additionalArrays = {500.0};
    List<Double> additionalInitialValues = Arrays.asList(additionalArrays);
    AdditionalConfigTunner tunner = new AdditionalConfigTunner(additionalParams,additionalInitialValues,1);
    List<Pair<Pair<Integer,Integer>, Double>> bestConfig = null;
    List<Pair<Pair<Integer,Integer>, Double>> bestLocalConfig = null;
    double bestScore = -Double.MAX_VALUE;
    while(tunner.isFlippable()){
      System.out.println("==================Generating Config:=====================");
      while(!tunner.isCompleted()){
        // i, which -> config value
        List<Pair<Pair<Integer,Integer>, Double>> config = tunner.getConfig();
        Pair<Pair<Integer,Integer>, Double> oneTuned = null;
        for (Pair<Pair<Integer,Integer>, Double> pair : config){
          if (pair.getFirst().getSecond() < tunner.eachSize && pair.getFirst().getSecond()>=0 ){
            oneTuned = pair;
          }
        }
        if (oneTuned == null){
          oneTuned = config.get(config.size()-1);
        }
//        System.out.println("Picked pair: "+oneTuned);
        System.out.println("---Generating Config:---");
        tunner.printConfig(config);



    /* score documents for queries */
        Pair<List<Pair<Pair<Integer,Integer>, Double>>,Double> localResult = test.linearConstrainTunner(parameters, initialValues, queryDict, taskOption, idfs, test, relevScores, config);
        // update parameters and initial values for linear constraint problem
        updateParameters(localResult, parameters);
        updateInitValues(localResult, initialValues);
        //

        double ndcgScore = localResult.getSecond();
        System.out.println("ConsinNDcgMain test run for NDCGScore: "+ndcgScore);
        System.out.println("-------------------------");
        tunner.update(oneTuned.getFirst().getFirst(),oneTuned.getFirst().getSecond(),ndcgScore);
        if (ndcgScore>bestScore){
          bestScore = ndcgScore;
          bestConfig = config;
          bestLocalConfig = localResult.getFirst();
        }
      }
      tunner.flip();
    }
    System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$");
    tunner.printConfig(bestConfig);
    tunner.printConfig(bestLocalConfig);
    System.out.println("Best Score: "+bestScore);



  }

  private static void updateInitValues(
      Pair<List<Pair<Pair<Integer, Integer>, Double>>, Double> localResult,
      List<Double> initialValues) {
    initialValues.clear();
    for (Pair<Pair<Integer, Integer>, Double> pair:localResult.getFirst()){
      initialValues.add(pair.getSecond());
    }
  }

  private static void updateParameters(
      Pair<List<Pair<Pair<Integer, Integer>, Double>>, Double> localResult,
      List<Pair<String, Pair<Double, Double>>> parameters) {
    List<Pair<Pair<Integer, Integer>, Double>> newParams = localResult.getFirst();
    for (int i=0;i<parameters.size();++i){
      Pair<Double,Double> oldPair = parameters.get(i).getSecond();
      double oldRange = oldPair.getSecond()-oldPair.getFirst();
      double newRange = oldRange/4.0;
      double newCenter = newParams.get(i).getSecond();
      double newLower = Math.max(oldPair.getFirst(),newCenter-newRange);
      double newUpper = Math.min(oldPair.getSecond(),newCenter+newRange);
      parameters.get(i).setSecond(new Pair<>(newLower,newUpper));
    }
  }

  public Pair<List<Pair<Pair<Integer,Integer>, Double>>,Double> linearConstrainTunner(List<Pair<String,Pair<Double,Double>>> parameters,
      List<Double> initialValues, Map<Query,Map<String, Document>> queryDict, String taskOption,
      Map<String, Double> idfs,CosinNdcgMain test, Map<String,Map<String,Double>> relevScores, List<Pair<Pair<Integer,Integer>, Double>> additionalConfig){
    BaseLineConfigTunner tunner = new LinearContraintConfigTunner(parameters,initialValues,2);
    List<Pair<Pair<Integer,Integer>, Double>> bestConfig = null;
    double bestScore = -Double.MAX_VALUE;
    while(tunner.isFlippable()){
//      System.out.println("==================Generating Config:=====================");
      while(!tunner.isCompleted()){
        // i, which -> config value
        List<Pair<Pair<Integer,Integer>, Double>> config = tunner.getConfig();
        Pair<Pair<Integer,Integer>, Double> oneTuned = null;
        for (Pair<Pair<Integer,Integer>, Double> pair : config){
          if (pair.getFirst().getSecond() < tunner.eachSize && pair.getFirst().getSecond()>=0 ){
            oneTuned = pair;
          }
        }
        if (oneTuned == null){
          oneTuned = config.get(config.size()-1);
        }
//        System.out.println("Picked pair: "+oneTuned);
//        System.out.println("---Generating Config:---");
        tunner.printConfig(config);

        Map<Query,List<Pair<Document,Double>>> queryRankings = score(queryDict, taskOption, idfs, config, additionalConfig);
        double ndcgScore = test.runOneConfigure(relevScores,queryRankings);
//        System.out.println("ConsinNDcgMain test run for NDCGScore: "+ndcgScore);
//        System.out.println("-------------------------");
        tunner.update(oneTuned.getFirst().getFirst(),oneTuned.getFirst().getSecond(),ndcgScore);
        if (ndcgScore>bestScore){
          bestScore = ndcgScore;
          bestConfig = config;
        }
      }
      tunner.flip();
    }
//    System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$");
    tunner.printConfig(bestConfig);
//    System.out.println("Best Score: "+bestScore);
    return new Pair(bestConfig,bestScore);
  }
  private Map<String,Map<String,Double>> genRelScore(String fileName) throws IOException {
    HashMap<String, Map<String, Double>> relevScores = new HashMap<String, Map<String, Double>>();


    // read the relevance score
    BufferedReader br = new BufferedReader(new FileReader(fileName));
    Map<String, Double> urlScores = null;
    String strLine;
    String query = "";
    while ((strLine = br.readLine()) != null) {
      if (strLine.trim().charAt(0) == 'q') {
        query = strLine.substring(strLine.indexOf(":")+1).trim();
        urlScores = new HashMap<String, Double>();
        relevScores.put(query, urlScores);
      }
      else {
        String[] tokens = strLine.substring(strLine.indexOf(":")+1).trim().split(" ");
        String url = tokens[0].trim();
        double relevance = Double.parseDouble(tokens[1]);
        if (relevance < 0)
          relevance = 0;
        if (urlScores != null)
          urlScores.put(url, relevance);
      }
    }
    br.close();
    return relevScores;
  }
  private double runOneConfigure(Map<String,Map<String,Double>> relevScores,  Map<Query,List<Pair<Document,Double>>> queryRankings){
    //query -> <url, relevance score>

    ////// NDCG computation
    ArrayList<Double> rels = new ArrayList<>();
    double completeNDCG = 0;
    double count = 0.0;
    for (Entry<Query,List<Pair<Document,Double>>> entry: queryRankings.entrySet()){
      for (Pair<Document,Double> docScore : entry.getValue()){
        Map<String, Double> docToRef = relevScores.get(entry.getKey().queryString);
        rels.add(docToRef.get(docScore.getFirst().url));
      }
      completeNDCG += getNdcgQuery(rels);
      rels.clear();
      count+=1.0;
    }
    completeNDCG/=count;
    return completeNDCG;
  }
  private static double getNdcgQuery(List<Double> rels)
  {
    double localSum = 0, sortedSum = 0;
    for (int i = 0; i < rels.size(); i++)
      localSum += (Math.pow(2, rels.get(i))-1)/(Math.log(1+i+1)/Math.log(2));
    Collections.sort(rels, Collections.reverseOrder());
    for (int i = 0; i < rels.size(); i++)
      sortedSum += (Math.pow(2, rels.get(i))-1)/(Math.log(1+i+1)/Math.log(2));
    if (sortedSum == 0)
      return 1;
    else
      return localSum/sortedSum;
  }
}
