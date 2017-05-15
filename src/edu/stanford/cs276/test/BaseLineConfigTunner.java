package edu.stanford.cs276.test;

import edu.stanford.cs276.util.Pair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by feiliu on 5/13/17.
 */
public abstract class BaseLineConfigTunner {
  List<Pair<String,Pair<Double,Double>>> parameters;
  List<Integer> progress;
  List<Double> initialValues;
  List<Pair<Integer,Double>> bestTracker;// for current round which config and what score
  double eachSize = 10.0;
  int flipLimit;
  int currIndex ;

  public static String[] TFTYPES = {"url","title","body","header","anchor","burl","btitle","bbody","bheader","banchor","k1","pageRankLambda","pageRankLambdaPrime"};
  public BaseLineConfigTunner(List<Pair<String,Pair<Double,Double>>> parameters, List<Double> initialValues, int flipLimit){
    this.parameters = parameters;
    this.initialValues = initialValues;
    this.progress = new ArrayList<>();
    this.flipLimit = flipLimit;
    init();
  }
  protected void init(){
    progress.clear();
    bestTracker = new ArrayList<>();
    for (int i=0;i<parameters.size();++i){
      this.progress.add(-1);
      bestTracker.add(new Pair(-1, -Double.MAX_VALUE));
    }
    currIndex=0;
  }
  abstract List<Pair<Pair<Integer,Integer>, Double>> getConfig();

  public void update(int i, int which, double score){
//    System.out.println(i+", "+which+", score: "+score);
    Pair<Integer,Double> stepOpt = bestTracker.get(i);
    if (score>stepOpt.getSecond()){
      stepOpt.setFirst(which);
      stepOpt.setSecond(score);
    }
  }
  abstract void flip();
  protected double genParams(int i, int which) {
    if (which == eachSize || which < 0){
      return initialValues.get(i);
    }else{
      Pair<String,Pair<Double,Double>> config =  parameters.get(i);
      double diff = (config.getSecond().getSecond() - config.getSecond().getFirst())*1.0/eachSize;
      double nextParam = config.getSecond().getFirst()+diff*(which+1.0);
      return nextParam;
    }
  }

  protected void updateProgress(){
//    for (int i=0;i<progress.size();++i){
    int i = currIndex;
      int currProg = progress.get(i);
      if (currProg == eachSize -1){
        progress.set(i,progress.get(i)+1);
        if (i+1<progress.size()){
          progress.set(i+1,progress.get(i+1)+1);
          currIndex++;
        }
//        break;
      }else if ( currProg < eachSize){
        progress.set(i,progress.get(i)+1);
//        break;
      }
//    }
  }

  public boolean isCompleted(){
    if (progress.size()==0){
      throw  new RuntimeException("No configuration provided!");
    }
    return progress.get(progress.size()-1) == eachSize-1;
  }

  public boolean isFlippable(){
    return flipLimit>0;
  }

  public void printConfig(List<Pair<Pair<Integer,Integer>, Double>> config){
    StringBuilder sb = new StringBuilder();
    for (int i=0;i<config.size();++i){
      Pair<Pair<Integer,Integer>,Double> pair = config.get(i);
      sb.append(parameters.get(i).getFirst()+"_"+pair.getFirst().getSecond()+": "+pair.getSecond()+"\n");
    }
    if (this instanceof LinearContraintConfigTunner){
      LinearContraintConfigTunner.keepRunningConfigPrintOut = sb.toString();
    }
    System.out.println(sb.toString());
  }
}
