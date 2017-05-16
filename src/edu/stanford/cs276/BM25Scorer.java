package edu.stanford.cs276;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.Map.Entry;

/**
 * Skeleton code for the implementation of a BM25 Scorer in Task 2.
 */
public class BM25Scorer extends AScorer {

  /*
   *  TODO: You will want to tune these values
   */
  public double urlweight = 6.0;
  public double titleweight  = 4.5;
  public double bodyweight = 0.1;
  public double headerweight = 0.7;
  public double anchorweight = 10.0;

  Map<String, Double> Wf;

  // BM25-specific weights
  public double burl = 2;
  public double btitle = 1;
  public double bheader = 0.5;
  public double bbody = 0.75;
  public double banchor = 0.1;

  Map<String, Double> Bf;

  public double k1 = 1;
  public double pageRankLambda = 1.2;
  public double pageRankLambdaPrime = 0.5;
  
  // query -> url -> document
  Map<Query,Map<String, Document>> queryDict; 

  Set<Document> docs;

  // BM25 data structures--feel free to modify these
  // Document -> field -> length
  Map<Document,Map<String,Double>> lengths;  

  // field name -> average length
  Map<String,Double> avgLengths;    

  // Document -> pagerank score
  Map<Document,Double> pagerankScores; 
  
  
    /**
     * Construct a BM25Scorer.
     * @param idfs the map of idf scores
     * @param queryDict a map of query to url to document
     */
    public BM25Scorer(Map<String,Double> idfs, Map<Query,Map<String, Document>> queryDict) { 
      super(idfs);
      this.queryDict = queryDict;
      this.calcAverageLengths();
    }

    /**
     * Set up average lengths for BM25, also handling PageRank.
     */
  public void calcAverageLengths() {
    lengths = new HashMap<Document,Map<String,Double>>();
    avgLengths = new HashMap<String,Double>();
    pagerankScores = new HashMap<Document,Double>();
    docs = new HashSet<Document>();
    for (Query query : queryDict.keySet()) {
      Map<String, Document> urls = queryDict.get(query);
      for (String url : urls.keySet()) {
        Document doc = urls.get(url);
        docs.add(doc);
      }
    }
    Bf = new HashMap<String, Double>();
    Bf.put("url", burl);
    Bf.put("title", btitle);
    Bf.put("body", bheader);
    Bf.put("header", bbody);
    Bf.put("anchor", banchor);
    Wf = new HashMap<String, Double>();
    Wf.put("url", urlweight);
    Wf.put("title", titleweight);
    Wf.put("body", bodyweight);
    Wf.put("header", headerweight);
    Wf.put("anchor", anchorweight);
    
    for (Query query : queryDict.keySet()) {
      Map<String, Document> urls = queryDict.get(query);
      for (String url : urls.keySet()) {
        Document doc = urls.get(url);
        pagerankScores.put(doc, getPageRankScores(doc.page_rank));
        Map<String,Double> fieldLength = new HashMap<String, Double>();
        for (String tfType : this.TFTYPES) {
          double len = 0;
          switch(tfType) {
            case "url":
              len = getUrlLength(doc.url);
              break;
            case "title":
              len = getStringLength(doc.title);
              break;
            case "body":
              len = doc.body_length;
              break;
            case "header":
              len = getHeaderLength(doc.headers);
              break;
            case "anchor":
              len = getAnchorLength(doc.anchors);
              break;
          }
          fieldLength.put(tfType, len);
        }
        lengths.put(doc, fieldLength);
      }
    }

    double docNum = docs.size();
    for (String tfType : this.TFTYPES) {
      double totalLen = 0;
      for (Document doc : docs) {
        Map<String, Double> fieldLength = lengths.get(doc);
        totalLen = totalLen + fieldLength.get(tfType);
      }
      avgLengths.put(tfType, totalLen / docNum);
    }
  }
  
  double getPageRankScores(int pageRank) {
    return (double)pageRank / (pageRankLambdaPrime + pageRank);
    //return Math.log(pageRankLambdaPrime + pageRank);
  }
  
  double getUrlLength(String s) {
    if (s == null) {
      return 0.0;
    }
    String[] t = s.split("[^A-Za-z0-9]+", -1);
    double l = 0;
    for (String k : t) {
      if (!k.isEmpty()) {
        l++;
      }
    }
    return l;
  }
  
  double getStringLength(String s) {
    if (s == null) {
      return 0.0;
    }
    String[] t = s.split(" ", -1);
    double l = 0;
    for (String k : t) {
      if (!k.isEmpty()) {
        l++;
      }
    }
    return l;
  }
  
  double getHeaderLength(List<String> sg) {
    if (sg == null) {
      return 0.0;
    }
    double l = 0;
    for (String s : sg) {
      l = l + getStringLength(s);
    }
    return l;
  }
  
  double getAnchorLength(Map<String, Integer> sm) {
    if (sm == null) {
      return 0.0;
    }
    double l = 0.0;
    for (String s : sm.keySet()) {
      l = l + getStringLength(s) * sm.get(s);
    }
    return l;
  }

  /**
   * Get the net score. 
   * @param tfs the term frequencies
   * @param q the Query 
   * @param tfQuery
   * @param d the Document
   * @return the net score
   */
  public double getNetScore(Map<String,Map<String, Double>> tfs, Query q, Map<String,Double> tfQuery,Document d) {
    double logTotalDocumentCount = idfs.get(Config.totalDocumentCountKey);
    double score = 0.0;
    for (String term : tfQuery.keySet()) {
      double termScore = 0.0;
      for (String tfType : tfs.keySet()) {
        Map<String, Double> termFreqHolder = tfs.get(tfType);
        double freq = 0.0;
        if (termFreqHolder.containsKey(term)) {
          freq = termFreqHolder.get(term);
        }
        termScore = termScore + freq * Wf.get(tfType);
      }
      double idfsTerm = 0.0;
      if (!idfs.containsKey(term)) {
        double freshIdfs = Math.log(tfQuery.get(term)+1);
        idfsTerm = logTotalDocumentCount-freshIdfs;
      } else {
        idfsTerm = idfs.get(term);
      }
      score = score + (termScore / (k1 + termScore) * idfsTerm) * tfQuery.get(term);
    }
    return score + pageRankLambda * pagerankScores.get(d);
  }

  /**
   * Do BM25 Normalization.
   * @param tfs the term frequencies
   * @param d the Document
   * @param q the Query
   */
  public void normalizeTFs(Map<String,Map<String, Double>> tfs,Document d, Query q) {
    for (String tfType : tfs.keySet()) {
      Map<String, Double> termFreqHolder = tfs.get(tfType);
      for (String term : termFreqHolder.keySet()) {
        double freqOriginal = termFreqHolder.get(term);
        double avgLength = avgLengths.get(tfType);
        double curLength = lengths.get(d).get(tfType);
        double denominator = (curLength / avgLength - 1) * Bf.get(tfType) + 1;
        termFreqHolder.put(term, freqOriginal / denominator);
      }
    }
  }
  
  /**
   * Write the tuned parameters of BM25 to file.
   * Only used for grading purpose, you should NOT modify this method.
   * @param filePath the output file path.
   */
  private void writeParaValues(String filePath) {
    try {
      File file = new File(filePath);
      if (!file.exists()) {
        file.createNewFile();
      }
      FileWriter fw = new FileWriter(file.getAbsoluteFile());
      String[] names = {
        "urlweight", "titleweight", "bodyweight", 
        "headerweight", "anchorweight", "burl", "btitle", 
        "bheader", "bbody", "banchor", "k1", "pageRankLambda", "pageRankLambdaPrime"
      };
      double[] values = {
        this.urlweight, this.titleweight, this.bodyweight, 
        this.headerweight, this.anchorweight, this.burl, this.btitle, 
        this.bheader, this.bbody, this.banchor, this.k1, this.pageRankLambda, 
        this.pageRankLambdaPrime
      };
      BufferedWriter bw = new BufferedWriter(fw);
      for (int idx = 0; idx < names.length; ++ idx) {
        bw.write(names[idx] + " " + values[idx]);
        bw.newLine();
      }
      bw.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  /**
   * Get the similarity score.
   * @param d the Document
   * @param q the Query
   * @return the similarity score
   */
  public double getSimScore(Document d, Query q) {
    Map<String,Map<String, Double>> tfs = this.getRawDocTermFreqs(d,q);
    this.normalizeTFs(tfs, d, q);
    Map<String,Double> tfQuery = getRawQueryFreqs(q);

    // Write out the tuned BM25 parameters
    // This is only used for grading purposes.
    // You should NOT modify the writeParaValues method.
    writeParaValues("bm25Para.txt");
    return getNetScore(tfs,q,tfQuery,d);
  } 
}