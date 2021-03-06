package edu.stanford.cs276.test;

import edu.stanford.cs276.util.Pair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by feiliu on 5/14/17.
 */
public class LinearContraintConfigTunner extends  BaseLineConfigTunner {

  public LinearContraintConfigTunner(
      List<Pair<String, Pair<Double, Double>>> parameters,
      List<Double> initialValues, int flipLimit) {
    super(parameters, initialValues, flipLimit);
  }

  public List<Pair<Pair<Integer,Integer>, Double>> getConfig(){
    updateProgress();
    List<Pair<Pair<Integer,Integer>, Double>> params = new ArrayList<>();
    double sum = 0;
    double chozenValue = 0;
    for (int i =0;i< progress.size();++i){
      int which = progress.get(i);
      double nextParam = genParams(i,which);
//      System.out.println("getConfig: i-"+i+", which"+which+", nextParam: "+nextParam);
      params.add(new Pair(new Pair(i,which),nextParam));
      if (i!=currIndex){
        sum += nextParam;
      }else{
        chozenValue = nextParam;
      }
    }
    for (int i=0;i< params.size();++i){
      Pair<Pair<Integer,Integer>, Double> pair = params.get(i);
      if (i!=currIndex){
        pair.setSecond((1-chozenValue)*pair.getSecond()/sum);
      }

    }
    return  params;
  }

  public void flip(){
    if (flipLimit==0){
      return;
    }
    double sum = 0;
    for (int i=0;i<parameters.size();++i){

      Pair<Integer,Double> best = bestTracker.get(i);
      double currOptConfig = genParams(i,best.getFirst());
      initialValues.set(i,currOptConfig);
      sum += currOptConfig;

    }
    for (int i=0;i<parameters.size();++i){
      Pair<String,Pair<Double,Double>> oldPair = parameters.get(i);
      double oldUpper = oldPair.getSecond().getSecond();
      double oldLower = oldPair.getSecond().getFirst();
      double range = oldUpper-oldLower;
      range/=4.0;
      initialValues.set(i,initialValues.get(i)/sum);
      parameters.set(i,new Pair(oldPair.getFirst(),new Pair(Math.max(oldLower,initialValues.get(i)-range)
          ,Math.min(oldUpper,initialValues.get(i)+range))));
    }
//    for (int i =0;i<initialValues.size();++i){
//      System.out.println(i+": init v: "+initialValues.get(i));
//    }

    init();
    flipLimit--;
  }
  public static void main(String[] args){
    List<Pair<String,Pair<Double,Double>>> parameters = new ArrayList<>();
    parameters.add(new Pair<>("Param1",new Pair<>(0.0,1.0)));
    parameters.add(new Pair<>("Param2",new Pair<>(0.0,0.5)));
    Double[] arrays = {0.1,0.9};
    List<Double> initialValues = Arrays.asList(arrays);
    LinearContraintConfigTunner tunner = new LinearContraintConfigTunner(parameters,initialValues,2);
    while(tunner.isFlippable()){
      System.out.println("=====Generating Config:=====");
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
        System.out.println("---Generating Config:");
        tunner.printConfig(config);
        tunner.update(oneTuned.getFirst().getFirst(),oneTuned.getFirst().getSecond(),-Math.pow(oneTuned.getFirst().getSecond()-0,2));
      }
      tunner.flip();
    }
  }
}
