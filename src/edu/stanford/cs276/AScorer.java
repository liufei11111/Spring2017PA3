package edu.stanford.cs276;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * An abstract class for a scorer. 
 * Needs to be extended by each specific implementation of scorers.
 */
public abstract class AScorer {

  // Map: term -> idf
  Map<String,Double> idfs; 

  // Various types of term frequencies that you will need
  String[] TFTYPES = {"url","title","body","header","anchor"};
  
  /**
   * Construct an abstract scorer with a map of idfs.
   * @param idfs the map of idf scores
   */
  public AScorer(Map<String,Double> idfs) {
    this.idfs = idfs;
  }

  /**
  * You can implement your own function to whatever you want for debug string
  * The following is just an example to include page information in the debug string
  * The string will be forced to be 1-line and truncated to only include the first 200 characters
  */
  public String getDebugStr(Document d, Query q)
  {
    return "Pagerank: " + Integer.toString(d.page_rank);
  }
  
    /**
     * Score each document for each query.
     * @param d the Document
     * @param q the Query
     */
  public abstract double getSimScore(Document d, Query q);
  
  /**
   * Get frequencies for a query.
   * @param q the query to compute frequencies for
   */
  public Map<String,Double> getQueryFreqs(Query q) {

    // queryWord -> term frequency
    Map<String,Double> tfQuery = new HashMap<String, Double>();     

    /*
     * TODO : Your code here
     * Compute the raw term (and/or sublinearly scaled) frequencies
     * Additionally weight each of the terms using the idf value
     * of the term in the query (we use the PA1 corpus to determine
     * how many documents contain the query terms which is stored
     * in this.idfs).
     */
    for (String str : q.queryWords){
      str = str.toLowerCase();
      if (tfQuery.containsKey(str)){
        tfQuery.put(str,tfQuery.get(str)+1.0);
      }else{
        tfQuery.put(str,1.0);
      }
    }
    double logTotalDocumentCount = this.idfs.get(Config.totalDocumentCountKey);
    for (Entry<String,Double> entry : tfQuery.entrySet()){
      double sublinearTermFreq = entry.getValue();// no sub linear scaling
      if (!this.idfs.containsKey(entry.getKey())){
        // cancel out the term freq when calculating idf and add laplace smoothed the term freq
        double freshIdfs = Math.log(entry.getValue()+1);
        double idfFresh = logTotalDocumentCount-freshIdfs;
        entry.setValue(sublinearTermFreq*idfFresh);
      }else{
        // We are sublinear tf + log idf
        // so that the terms in the corpus is better than not
        entry.setValue(sublinearTermFreq*this.idfs.get(entry.getKey()));
      }
    }
    return tfQuery;
  }
  public Map<String,Double> getRawQueryFreqs(Query q) {

    // queryWord -> term frequency
    Map<String,Double> tfQuery = new HashMap<String, Double>();

    /*
     * TODO : Your code here
     * Compute the raw term (and/or sublinearly scaled) frequencies
     * Additionally weight each of the terms using the idf value
     * of the term in the query (we use the PA1 corpus to determine
     * how many documents contain the query terms which is stored
     * in this.idfs).
     */
    for (String str : q.queryWords){
      str = str.toLowerCase();
      if (tfQuery.containsKey(str)){
        tfQuery.put(str,tfQuery.get(str)+1.0);
      }else{
        tfQuery.put(str,1.0);
      }
    }
    return tfQuery;
  }
  
  /*
   * TODO : Your code here
   * Include any initialization and/or parsing methods
   * that you may want to perform on the Document fields
   * prior to accumulating counts.
   * See the Document class in Document.java to see how
   * the various fields are represented.
   */

  public Map<String, Map<String,Double>> emptyDcoTermFreqs(){
    Map<String,Map<String,Double>> map = new HashMap<>();
    for (String type : TFTYPES){
      map.put(type, new HashMap<String,Double>());
    }
    return map;
  }
  /**
   * Accumulate the various kinds of term frequencies 
   * for the fields (url, title, body, header, and anchor).
   * You can override this if you'd like, but it's likely 
   * that your concrete classes will share this implementation.
   * @param d the Document
   * @param q the Query
   */
  public Map<String,Map<String, Double>> getDocTermFreqs(Document d, Query q) {
    // Map from tf type -> queryWord -> score
    Map<String,Map<String, Double>> tfs = emptyDcoTermFreqs();

    /*
     * TODO : Your code here
     * Initialize any variables needed
     */
    //{"url","title","body","header","anchor"};
    Map<String, Double> docUrlMap = genDocUrlMap(d.url);
    Map<String, Double> docTitleMap = genDocTextMap(d.title);
    Map<String, Double> docBodyMap = genDocBodyToMap(d.body_hits);
    Map<String, Double> docHeaderMap = genDocListTextToMap(d.headers);
    Map<String, Double> docAnchorMap = genDocMapTextToMap(d.anchors);

    for (String queryWord : q.queryWords) {
      queryWord = queryWord.toLowerCase();
      /*
       * Your code here
       * Loop through query terms and accumulate term frequencies.
       * Note: you should do this for each type of term frequencies,
       * i.e. for each of the different fields.
       * Don't forget to lowercase the query word.
       */
      populateNewScore(tfs, queryWord, docUrlMap, 0);
      populateNewScore(tfs, queryWord, docTitleMap, 1);
      populateNewScore(tfs, queryWord, docBodyMap, 2);
      populateNewScore(tfs, queryWord, docHeaderMap, 3);
      populateNewScore(tfs, queryWord, docAnchorMap, 4);
    }
    return tfs;
  }
  public Map<String,Map<String, Double>> getRawDocTermFreqs(Document d, Query q) {
    // Map from tf type -> queryWord -> score
    Map<String,Map<String, Double>> tfs = emptyDcoTermFreqs();

    /*
     * TODO : Your code here
     * Initialize any variables needed
     */
    //{"url","title","body","header","anchor"};
    Map<String, Double> docUrlMap = genDocUrlMap(d.url);
    Map<String, Double> docTitleMap = genDocTextMap(d.title);
    Map<String, Double> docBodyMap = genDocBodyToMap(d.body_hits);
    Map<String, Double> docHeaderMap = genDocListTextToMap(d.headers);
    Map<String, Double> docAnchorMap = genDocMapTextToMap(d.anchors);

    for (String queryWord : q.queryWords) {
      queryWord = queryWord.toLowerCase();
      /*
       * Your code here
       * Loop through query terms and accumulate term frequencies.
       * Note: you should do this for each type of term frequencies,
       * i.e. for each of the different fields.
       * Don't forget to lowercase the query word.
       */
      populateOldScore(tfs, queryWord, docUrlMap, 0);
      populateOldScore(tfs, queryWord, docTitleMap, 1);
      populateOldScore(tfs, queryWord, docBodyMap, 2);
      populateOldScore(tfs, queryWord, docHeaderMap, 3);
      populateOldScore(tfs, queryWord, docAnchorMap, 4);
    }
    return tfs;
  }
  private void populateNewScore(Map<String,Map<String, Double>> tfs, String queryWord, Map<String, Double> truthMap, int fieldIndex){
    if (truthMap.containsKey(queryWord)){
      double subLinearScore = Math.log(truthMap.get(queryWord))+1;
      tfs.get(TFTYPES[fieldIndex]).put(queryWord,subLinearScore);
    }else{
      tfs.get(TFTYPES[fieldIndex]).put(queryWord,0.0);
    }
  }

  private void populateOldScore(Map<String,Map<String, Double>> tfs, String queryWord, Map<String, Double> truthMap, int fieldIndex){
    if (truthMap.containsKey(queryWord)){
      tfs.get(TFTYPES[fieldIndex]).put(queryWord,truthMap.get(queryWord));
    }else{
      tfs.get(TFTYPES[fieldIndex]).put(queryWord,0.0);
    }
  }
  private Map<String,Double> genDocBodyToMap(Map<String, List<Integer>> body_hits) {
    Map<String,Double> body = new HashMap<>();
    if (body_hits == null){return body;}
    for (Entry<String,List<Integer>> bodyList :body_hits.entrySet()){
      body.put(bodyList.getKey(),bodyList.getValue().size()*1.0);
    }
    return body;
  }

  private Map<String,Double> genDocMapTextToMap(Map<String, Integer> anchors) {
    Map<String,Double> anchorMap = new HashMap<>();
    if (anchors == null){return anchorMap;}
    for (Entry<String,Integer> anchor:anchors.entrySet()){
      Map<String,Double> tempMap = genDocTextMap(anchor.getKey());
      anchorMap = updateAnchorMap(anchorMap,tempMap,anchor.getValue());
    }
    return anchorMap;
  }

  private Map<String,Double> updateAnchorMap(Map<String, Double> anchorMap, Map<String, Double> tempMap,
      Integer value) {
    for (Entry<String,Double> entry:tempMap.entrySet()){
      String key = entry.getKey();
      double diff = entry.getValue()*value;
      if (anchorMap.containsKey(key)){
        anchorMap.put(key,anchorMap.get(key)+diff);
      }else{
        anchorMap.put(key,diff);
      }
    }
    return anchorMap;
  }

  private Map<String,Double> genDocListTextToMap(List<String> headers) {
    Map<String,Double> headerMap = new HashMap<>();
    if (headers == null){return headerMap;}
    for (String str : headers){
      Map<String,Double> nextMap = genDocTextMap(str);
      headerMap = mergeTwoMap(headerMap,nextMap);
    }
    return headerMap;
  }

  private Map<String,Double> mergeTwoMap(Map<String, Double> headerMap, Map<String, Double> nextMap) {
    for (Entry<String,Double> entry:nextMap.entrySet()){
      String key = entry.getKey();
      if (headerMap.containsKey(key)){
        headerMap.put(key,headerMap.get(key)+entry.getValue());
      }else{
        headerMap.put(key,entry.getValue());
      }
    }
    return headerMap;
  }
  private Map<String,Double> genDocTextMap(String title) {
    Map<String,Double> urlMap = new HashMap<>();
    String[] parsed = title.split(" ");
    for (String str : parsed){
      // TO lower case
      str= str.toLowerCase();
      if (urlMap.containsKey(str)){
        urlMap.put(str,urlMap.get(str)+1.0);
      }else{
        urlMap.put(str,1.0);
      }
    }
    return urlMap;
  }

  private Map<String,Double> genDocUrlMap(String text) {
    Map<String,Double> urlMap = new HashMap<>();
    if (text == null){return urlMap;}
    StringBuilder sb = new StringBuilder();
    char[] chars = text.toCharArray();
    for (char aChar : chars){
      if (Character.isLetterOrDigit(aChar)){
        sb.append(aChar);
      }else{
        sb.append(" ");
      }
    }
    return genDocTextMap(sb.toString().replace("\\s+"," "));
  }



}
