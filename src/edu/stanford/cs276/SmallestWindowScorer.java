package edu.stanford.cs276;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**
 * A skeleton for implementing the Smallest Window scorer in Task 3.
 * Note: The class provided in the skeleton code extends BM25Scorer in Task 2. However, you don't necessarily
 * have to use Task 2. (You could also use Task 1, in which case, you'd probably like to extend CosineSimilarityScorer instead.)
 * Also, feel free to modify or add helpers inside this class.
 */
public class SmallestWindowScorer extends BM25Scorer {

  public double B = 1.554;
  public SmallestWindowScorer(Map<String, Double> idfs, Map<Query,Map<String, Document>> queryDict) {
    super(idfs, queryDict);
  }
  
  private int getSmallestFieldWindow(Map<String,Double> tfQuery, String s, String reg) {
    int ss = 0;
    if (s.equals("http://www-cs.stanford.edu/pub/mirrors/ubuntu/ubuntu/dists/xenial/universe/i18n/Translation-sl")) {
      ss = 0;
    }
    if (s == null || s.isEmpty()) {
      return Integer.MAX_VALUE;
    }
    int needToSatisfy = tfQuery.size() + ss;
    Map<String,Double> sQuery = new HashMap<String, Double>();
    String[] t = s.split(reg, -1);
    int sl = 0;
    for (String k : t) {
      if (!k.isEmpty()) {
        sl++;
      }
    }
    int smallestFieldWindow = Integer.MAX_VALUE;
    int satisfied = 0;
    int index1 = 0;
    int index2 = 0;
    int rindex1 = 0;
    int rindex2 = 0;
    while (t[rindex1].isEmpty()) {
      rindex1++;
      continue;
    }
    while (t[rindex2].isEmpty()) {
      rindex2++;
      continue;
    }
    while(index2 < sl) {
      String sIndex2 = t[rindex2];
      double SOccurence = 0.0;
      if (sQuery.containsKey(sIndex2)) {
        SOccurence = sQuery.get(sIndex2);
        sQuery.put(sIndex2, SOccurence + 1);
      } else {
        sQuery.put(sIndex2, 1.0);
      }
      double TfOccurence = 0.0;
      if (tfQuery.containsKey(sIndex2)) {
        TfOccurence = tfQuery.get(sIndex2);
      }
      if (SOccurence == TfOccurence - 1) {
        satisfied++;
      }
      index2++;
      rindex2++;
      while (index2 < sl &&t[rindex2].isEmpty()) {
        rindex2++;
        continue;
      }
 
      if (satisfied == needToSatisfy) {
        while(true) {
          String sIndex1 = t[rindex1];
          double oldTfPreOccurence = 0.0;
          if (tfQuery.containsKey(sIndex1)) {
            oldTfPreOccurence = tfQuery.get(sIndex1);
          }
          double oldSPreOccurence = sQuery.get(sIndex1);
          if (oldSPreOccurence > oldTfPreOccurence) {
            if (oldSPreOccurence == 1) {
              sQuery.remove(sIndex1);
            } else {
              sQuery.put(sIndex1, oldSPreOccurence - 1);
            }
            index1++;
            rindex1++;
            while (index1 < sl && t[rindex1].isEmpty()) {
              rindex1++;
              continue;
            }
          } else {
            break;
          }
        }
        if (index2 - index1 < smallestFieldWindow) {
          smallestFieldWindow = index2 - index1;
        }
      }
    }
    return smallestFieldWindow;
  }
  
  private int getSmallestFieldWindow(Map<String,Double> tfQuery, Map<String, List<Integer>> bodyHits) {
    if (bodyHits == null) {
      return Integer.MAX_VALUE;
    }
    
    int needToSatisfy = tfQuery.size();
    Map<String,Double> sQuery = new HashMap<String, Double>();
    Map<Integer, String> t = new TreeMap<Integer, String>();
    List<Integer> arrT = new ArrayList<Integer>();
    for (String k : bodyHits.keySet()) {
      for (Integer pos : bodyHits.get(k)) {
        t.put(pos, k);
      }
    }
    for (Integer k : t.keySet()) {
      arrT.add(k);
    }
    int sl = arrT.size();
    int smallestFieldWindow = Integer.MAX_VALUE;
    int satisfied = 0;
    int index1 = 0;
    int index2 = 0;
    while(index2 < sl) {
      Integer pos2 = arrT.get(index2);
      String sIndex2 = t.get(pos2);
      double SOccurence = 0.0;
      if (sQuery.containsKey(sIndex2)) {
        SOccurence = sQuery.get(sIndex2);
      }
      sQuery.put(sIndex2, SOccurence + 1);
      double TfOccurence = 0.0;
      if (tfQuery.containsKey(sIndex2)) {
        TfOccurence = tfQuery.get(sIndex2);
      }
      if (SOccurence == TfOccurence - 1) {
        satisfied++;
      }
      index2++;
      if (satisfied == needToSatisfy) {
        while(true) {
          Integer pos1 = arrT.get(index1);
          String sIndex1 = t.get(pos1);
          double oldTfPreOccurence = 0.0;
          if (tfQuery.containsKey(sIndex1)) {
            oldTfPreOccurence = tfQuery.get(sIndex1);
          }
          double oldSPreOccurence = sQuery.get(sIndex1);
          if (oldSPreOccurence > oldTfPreOccurence) {
            if (oldSPreOccurence == 1) {
              sQuery.remove(sIndex1);
            } else {
              sQuery.put(sIndex1, oldSPreOccurence - 1);
            }
            index1++;
          } else {
            break;
          }
        }
        if (arrT.get(index2 - 1) - arrT.get(index1) + 1 < smallestFieldWindow) {
          smallestFieldWindow = arrT.get(index2 - 1) - arrT.get(index1) + 1;
        }
      }
    }
    return smallestFieldWindow;
  }
  
  private int getSmallestFieldWindow(Map<String,Double> tfQuery, Document d, String tfType) {
    int smallestFieldWindow = Integer.MAX_VALUE;
    int tempSmallestFieldWindow = Integer.MAX_VALUE;
    switch(tfType) {
    case "url":
      smallestFieldWindow = getSmallestFieldWindow(tfQuery, d.url, "[^A-Za-z0-9]+");
      break;
    case "title":
      smallestFieldWindow = getSmallestFieldWindow(tfQuery, d.title, " ");
      break;
    case "body":
      if (d.body_hits == null || tfQuery.size() != d.body_hits.size()) {
        return Integer.MAX_VALUE;
      }
      smallestFieldWindow = getSmallestFieldWindow(tfQuery, d.body_hits);
      break;
    case "header":
      if (d.headers == null) {
        return Integer.MAX_VALUE;
      }
      for (String header : d.headers) {
        tempSmallestFieldWindow = getSmallestFieldWindow(tfQuery, header, " ");
        if (tempSmallestFieldWindow < smallestFieldWindow) {
          smallestFieldWindow = tempSmallestFieldWindow;
        }
      }
      break;
    case "anchor":
      if (d.anchors == null) {
        return Integer.MAX_VALUE;
      }
      for (String anchor : d.anchors.keySet()) {
        tempSmallestFieldWindow = getSmallestFieldWindow(tfQuery, anchor, " ");
        if (tempSmallestFieldWindow < smallestFieldWindow) {
          smallestFieldWindow = tempSmallestFieldWindow;
        }
      }
      break;
    }
    return smallestFieldWindow;
  }

  /**
   * get smallest window of one document and query pair.
   * @param d: document
   * @param q: query
   */  
  private int getWindow(Document d, Query q, Map<String,Double> tfQuery) {
    int smallestWindow = Integer.MAX_VALUE;
    for (String tfType : this.TFTYPES) {
      int smallestFieldWindow = getSmallestFieldWindow(tfQuery, d, tfType);
      if (smallestFieldWindow < smallestWindow) {
        smallestWindow = smallestFieldWindow;
      }
    }
    return smallestWindow;
  }

  
  /**
   * get boost score of one document and query pair.
   * @param d: document
   * @param q: query
   */  
  private double getBoostScore (Document d, Query q, Map<String,Double> tfQuery) {
    int smallestWindow = getWindow(d, q, tfQuery);
    double queryLength = 0;
    for (String query : tfQuery.keySet()) {
      queryLength = queryLength + tfQuery.get(query);
    }
    if (smallestWindow == Integer.MAX_VALUE) {
      return 1;
    }
    double diff = smallestWindow - queryLength;
    return 1 + (B - 1) * Math.exp(-diff);
  }
  
  @Override
  public double getSimScore(Document d, Query q) {
    Map<String,Map<String, Double>> tfs = this.getRawDocTermFreqs(d,q);
    this.normalizeTFs(tfs, d, q);
    Map<String,Double> tfQuery = getRawQueryFreqs(q);
    double boost = getBoostScore(d, q, tfQuery);
    double rawScore = this.getNetScore(tfs, q, tfQuery, d);
    return boost * rawScore;
  }

}
