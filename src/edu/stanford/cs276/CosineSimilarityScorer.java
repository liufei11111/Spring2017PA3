package edu.stanford.cs276;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Skeleton code for the implementation of a 
 * Cosine Similarity Scorer in Task 1.
 */
public class CosineSimilarityScorer extends AScorer {

  /*
   * TODO: You will want to tune the values for
   * the weights for each field.
   */
  public double urlweight = 0.23795549267542235;
  public double titleweight  = 0.09096936314246667;
  public double bodyweight = 0.018406977715679264;
  public double headerweight = 0.25588594518954305;
  public double anchorweight = 0.39678222127688867;
  public double smoothingBodyLength = 1350.0;
  //  String[] TFTYPES = {"url","title","body","header","anchor"};
  /**
   * Construct a Cosine Similarity Scorer.
   * @param idfs the map of idf values
   */
  public CosineSimilarityScorer(Map<String,Double> idfs) {
    super(idfs);
  }

  /**
   * Get the net score for a query and a document.
   * @param tfs the term frequencies
   * @param q the Query
   * @param tfQuery the term frequencies for the query
   * @param d the Document
   * @return the net score
   */
  public double getNetScore(Map<String, Map<String, Double>> tfs, Query q, Map<String,Double> tfQuery, Document d) {
    double score = 0.0;

    /*
     * TODO : Your code here
     * See Equation 2 in the handout regarding the net score
     * between a query vector and the term score vectors
     * for a document.
     */
    Map<String,Double> urlMap = tfs.get(TFTYPES[0]);
    Map<String,Double> titleMap = tfs.get(TFTYPES[1]);
    Map<String,Double> bodyMap = tfs.get(TFTYPES[2]);
    Map<String,Double> headerMap = tfs.get(TFTYPES[3]);
    Map<String,Double> anchorMap = tfs.get(TFTYPES[4]);

    for (Entry<String, Double> entry : tfQuery.entrySet()){
      score += urlMap.get(entry.getKey())*entry.getValue()*urlweight;
      score += titleMap.get(entry.getKey())*entry.getValue()*titleweight;
      score += bodyMap.get(entry.getKey())*entry.getValue()*bodyweight;
      score += headerMap.get(entry.getKey())*entry.getValue()*headerweight;
      score += anchorMap.get(entry.getKey())*entry.getValue()*anchorweight;
    }
    return score;
  }
  
  /**
   * Normalize the term frequencies. 
   * @param tfs the term frequencies
   * @param d the Document
   * @param q the Query
   */
  public void normalizeTFs(Map<String,Map<String, Double>> tfs,Document d, Query q) {
  /*
     * TODO : Your code here
     * Note that we should give uniform normalization to all 
     * fields as discussed in the assignment handout.
     */
   double bodyLengthSmoothed =d.body_length + smoothingBodyLength;
   for (String type : TFTYPES){
     Map<String, Double> keyMap = tfs.get(type);
     for(Entry<String, Double> entry : keyMap.entrySet()){
       entry.setValue(entry.getValue()/bodyLengthSmoothed);
     }
   }
  }
  
  /**
   * Write the tuned parameters of cosineSimilarity to file.
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
        "urlweight", "titleweight", "bodyweight", "headerweight", 
        "anchorweight", "smoothingBodyLength"
      };
      double[] values = {
        this.urlweight, this.titleweight, this.bodyweight, 
    this.headerweight, this.anchorweight, this.smoothingBodyLength
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
  /** Get the similarity score between a document and a query.
   * @param d the Document
   * @param q the Query
   * @return the similarity score.
   */
  public double getSimScore(Document d, Query q) {
    Map<String,Map<String, Double>> tfs = this.getDocTermFreqs(d,q);
    this.normalizeTFs(tfs, d, q);
    Map<String,Double> tfQuery = getQueryFreqs(q);

    // Write out tuned cosineSimilarity parameters
    // This is only used for grading purposes.
    // You should NOT modify the writeParaValues method.
    writeParaValues("cosinePara.txt");
    return getNetScore(tfs,q,tfQuery,d);
  }
}
