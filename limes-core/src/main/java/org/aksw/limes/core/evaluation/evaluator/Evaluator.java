/*
 * LIMES Core Library - LIMES – Link Discovery Framework for Metric Spaces.
 * Copyright © 2011 Data Science Group (DICE) (ngonga@uni-paderborn.de)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.aksw.limes.core.evaluation.evaluator;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.aksw.limes.core.datastrutures.EvaluationRun;
import org.aksw.limes.core.datastrutures.GoldStandard;
import org.aksw.limes.core.datastrutures.TaskAlgorithm;
import org.aksw.limes.core.datastrutures.TaskData;
import org.aksw.limes.core.evaluation.evaluationDataLoader.DataSetChooser;
import org.aksw.limes.core.evaluation.evaluationDataLoader.EvaluationData;
import org.aksw.limes.core.evaluation.qualititativeMeasures.McNemarsTest;
import org.aksw.limes.core.evaluation.qualititativeMeasures.QualitativeMeasuresEvaluator;
import org.aksw.limes.core.evaluation.quantitativeMeasures.IQuantitativeMeasure;
import org.aksw.limes.core.evaluation.quantitativeMeasures.RunRecord;
import org.aksw.limes.core.exceptions.UnsupportedMLImplementationException;
import org.aksw.limes.core.io.cache.ACache;
import org.aksw.limes.core.io.cache.HybridCache;
import org.aksw.limes.core.io.cache.Instance;
import org.aksw.limes.core.io.config.Configuration;
import org.aksw.limes.core.io.mapping.AMapping;
import org.aksw.limes.core.io.mapping.MappingFactory;
import org.aksw.limes.core.measures.mapper.MappingOperations;
import org.aksw.limes.core.measures.measure.MeasureType;
import org.aksw.limes.core.ml.algorithm.*;
import org.aksw.limes.core.ml.algorithm.wombat.AWombat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * This evaluator is responsible for evaluating set of datasets that have
 * source, target, gold standard and mappings against set of measures
 *
 * @author Mofeed Hassan (mounir@informatik.uni-leipzig.de)
 * @author Daniel Obraczka (obraczka@studserv.uni-leipzig.de)
 * @version 1.0
 * @since 1.0
 */
public class Evaluator {
    static Logger logger = LoggerFactory.getLogger(Evaluator.class);

    public List<EvaluationRun> runsList = new ArrayList<EvaluationRun>();
    // algo, algo, successesFailures
    public Map<String, Map<String, int[]>> successesAndFailures = new HashMap<>();
    // dataset, algo, algo, value
    public Map<String, Map<String, Map<String, Double>>> statisticalTestResults = new HashMap<>();

    /**
     * The qualitative measure evaluator that evaluates the predictions generated by
     * a machine learning algorithm using a gold standard and set of measures
     */
    private QualitativeMeasuresEvaluator eval = new QualitativeMeasuresEvaluator();

    // remember
    // ---------AMLAlgorithm(concrete:SupervisedMLAlgorithm,ActiveMLAlgorithm or
    // UnsupervisedMLAlgorithm--------
    // --- --------
    // --- ACoreMLAlgorithm (concrete: EAGLE,WOMBAT,LION) u can retrieve it by get()
    // --------
    // --- --------
    // ---------------------------------------------------------------------------------------------------------

    /**
     * @param TaskAlgorithms
     *            the set of algorithms used to generate the predicted mappings
     * @param datasets
     *            the set of the datasets to apply the algorithms on them. The
     *            should include source Cache, target Cache, goldstandard and
     *            predicted mapping
     * @param QlMeasures
     *            the set of qualitative measures
     * @param QnMeasures
     *            the set of quantitative measures
     * @return List - contains list of multiple runs evaluation results
     *         corresponding to the algorithms, its implementation and used dataset
     */
    public List<EvaluationRun> evaluate(List<TaskAlgorithm> TaskAlgorithms, Set<TaskData> datasets,
                                        Set<EvaluatorType> QlMeasures, Set<IQuantitativeMeasure> QnMeasures) {
        AMapping predictions = null;
        Map<EvaluatorType, Double> evaluationResults = null;
        try {
            for (TaskAlgorithm tAlgorithm : TaskAlgorithms) { // iterate over algorithms tasks(type,algorithm,parameter)
                logger.info("Running algorihm: " + tAlgorithm.getMlAlgorithm().getName());
                for (TaskData dataset : datasets) { // iterate over
                    // datasets(name,source,target,mapping,training,pseudofm)
                    logger.info("Used dataset: " + dataset.dataName);
                    // initialize the algorithm with source and target data, passing its parameters
                    // too ( if it is null in case of WOMBAT it will use its defaults)
                    tAlgorithm.getMlAlgorithm().init(null, dataset.source, dataset.target);

                    MLResults mlModel = null; // model resulting from the learning process

                    if (tAlgorithm.getMlType().equals(MLImplementationType.SUPERVISED_BATCH)) {
                        logger.info("Implementation type: " + MLImplementationType.SUPERVISED_BATCH);
                        SupervisedMLAlgorithm sml = (SupervisedMLAlgorithm) tAlgorithm.getMlAlgorithm();
                        mlModel = sml.learn(dataset.training);
                    } else if (tAlgorithm.getMlType().equals(MLImplementationType.SUPERVISED_ACTIVE)) {
                        logger.info("Implementation type: " + MLImplementationType.SUPERVISED_ACTIVE);
                        ActiveMLAlgorithm sml = (ActiveMLAlgorithm) tAlgorithm.getMlAlgorithm();
                        sml.getMl().setConfiguration(dataset.evalData.getConfigReader().getConfiguration());
                        // if(tAlgorithm.getMlAlgorithm().getName().equals("Decision Tree Learning")){
                        // ((DecisionTreeLearning)sml.getMl()).setPropertyMapping(dataset.evalData.getPropertyMapping());
                        // ((DecisionTreeLearning)sml.getMl()).setInitialMapping(dataset.training);
                        // }
                        sml.activeLearn();
                        // mlModel = sml.activeLearn(dataset.training);
                        AMapping nextExamples = sml.getNextExamples((int) Math.round(0.5 * dataset.training.size()));
                        AMapping oracleFeedback = oracleFeedback(nextExamples, dataset.training);
                        mlModel = sml.activeLearn(oracleFeedback);
                    } else if (tAlgorithm.getMlType().equals(MLImplementationType.UNSUPERVISED)) {
                        logger.info("Implementation type: " + MLImplementationType.UNSUPERVISED);
                        UnsupervisedMLAlgorithm sml = (UnsupervisedMLAlgorithm) tAlgorithm.getMlAlgorithm();
                        mlModel = sml.learn(dataset.pseudoFMeasure);
                    }
                    predictions = tAlgorithm.getMlAlgorithm().predict(dataset.source, dataset.target, mlModel);
                    logger.info("Start the evaluation of the results");
                    evaluationResults = eval.evaluate(predictions, dataset.goldStandard, QlMeasures);
                    EvaluationRun er = new EvaluationRun(tAlgorithm.getMlAlgorithm().getName().replaceAll("\\s+", ""),
                            tAlgorithm.getMlType().name().replaceAll("//s", ""), dataset.dataName.replaceAll("//s", ""),
                            evaluationResults);
                    runsList.add(er);
                }
            }
        } catch (UnsupportedMLImplementationException e) {
            e.printStackTrace();
        }
        return runsList;

    }

    /**
     * @param algorithm
     *            the algorithm used to generate the predicted mappings
     * @param datasets
     *            the set of the datasets to apply the algorithms on them. The
     *            should include source Cache, target Cache, goldstandard and
     *            predicted mapping
     * @param parameter
     *            the parameters of the algorithm (will be set to default if this is
     *            null)
     * @param foldNumber
     *            the number of subsamples to divide the data (k)
     * @param qlMeasures
     *            the set of qualitative measures
     * @param qnMeasures
     *            the set of quantitative measures
     * @return List - contains list of multiple runs evaluation results
     *         corresponding to the algorithms, its implementation and used dataset
     *
     * @author Tommaso Soru (tsoru@informatik.uni-leipzig.de)
     * @version 2016-02-26
     */
    /* Table<String, String, Map<EvaluatorType, Double>> */
    public List<EvaluationRun> crossValidate(AMLAlgorithm algorithm, List<LearningParameter> parameter,
                                             Set<TaskData> datasets, int foldNumber, Set<EvaluatorType> qlMeasures,
                                             Set<IQuantitativeMeasure> qnMeasures) {

        // select a dataset-pair to evaluate each ML algorithm on
        for (TaskData dataset : datasets) {
            // Adjust if you dont need negative examples
            List<FoldData> folds = generateFolds(dataset.evalData, foldNumber, true);

            FoldData trainData = new FoldData();
            FoldData testData = folds.get(foldNumber - 1);
            // perform union on test folds
            for (int i = 0; i < foldNumber; i++) {
                if (i != 9) {
                    trainData.map = MappingOperations.union(trainData.map, folds.get(i).map);
                    trainData.sourceCache = cacheUnion(trainData.sourceCache, folds.get(i).sourceCache);
                    trainData.targetCache = cacheUnion(trainData.targetCache, folds.get(i).targetCache);
                }
            }
            // fix caches if necessary
            for (String s : trainData.map.getMap().keySet()) {
                for (String t : trainData.map.getMap().get(s).keySet()) {
                    if (!trainData.targetCache.containsUri(t)) {
                        // logger.info("target: " + t);
                        trainData.targetCache.addInstance(dataset.target.getInstance(t));
                    }
                }
                if (!trainData.sourceCache.containsUri(s)) {
                    // logger.info("source: " + s);
                    trainData.sourceCache.addInstance(dataset.source.getInstance(s));
                }
            }
            AMapping trainingData = trainData.map;
            ACache trainSourceCache = trainData.sourceCache;
            ACache trainTargetCache = trainData.targetCache;
            ACache testSourceCache = testData.sourceCache;
            ACache testTargetCache = testData.targetCache;
            GoldStandard goldStandard = new GoldStandard(testData.map, testSourceCache.getAllUris(),
                    testTargetCache.getAllUris());

            // train
            MLResults model = trainModel(algorithm, parameter, trainingData, dataset.evalData.getConfigReader().read(),
                    trainSourceCache, trainTargetCache);
            EvaluationRun er = new EvaluationRun(algorithm.getName(), dataset.dataName, eval
                    .evaluate(algorithm.predict(testSourceCache, testTargetCache, model), goldStandard, qlMeasures));
            er.display();
            runsList.add(er);
        }
        return runsList;
    }

    public Summary crossValidateWithTuningAndStatisticalTest(List<TaskAlgorithm> TaskAlgorithms,
                                                             Set<TaskData> datasets, Set<EvaluatorType> qlMeasures, int foldNumber) {
        for (TaskData dataset : datasets) {
            successesAndFailures = new HashMap<>();
            // Adjust if you need negative examples
            List<FoldData> folds = generateFolds(dataset.evalData, foldNumber, false);
            for (int k = 0; k < foldNumber; k++) {
                Map<String, AMapping> algoMappings = new HashMap<>();
                FoldData testData = folds.get(k);
                FoldData trainData = getTrainingFold(folds, k, foldNumber);
                trainData = fixCachesIfNecessary(trainData, dataset);
                AMapping trainingData = trainData.map;
                ACache trainSourceCache = trainData.sourceCache;
                ACache trainTargetCache = trainData.targetCache;
                ACache testSourceCache = testData.sourceCache;
                ACache testTargetCache = testData.targetCache;
                GoldStandard goldStandard = new GoldStandard(testData.map, testSourceCache.getAllUris(),
                        testTargetCache.getAllUris());
                List<FoldData> tuneFolds = createTuneFolds(trainData, 5d);
                GoldStandard tuneGold = new GoldStandard(tuneFolds.get(1).map,
                        tuneFolds.get(1).sourceCache.getAllUris(), tuneFolds.get(1).targetCache.getAllUris());
                for (TaskAlgorithm tAlgo : TaskAlgorithms) {
                    AMLAlgorithm algorithm = tAlgo.getMlAlgorithm();
                    // tune parameters
                    List<LearningParameter> params = null;
                    if (tAlgo.getMlParameterValues() != null) {
                        Set<List<LearningParameter>> parameterGrid = createParameterGrid(tAlgo.getMlParameterValues());
                        double bestFM = 0.0;
                        for (List<LearningParameter> lps : parameterGrid) {
                            MLResults tuneModel = trainModel(algorithm, lps, tuneFolds.get(0).map,
                                    dataset.evalData.getConfigReader().read(), tuneFolds.get(0).sourceCache,
                                    tuneFolds.get(0).targetCache);
                            double current = eval
                                    .evaluate(
                                            algorithm.predict(tuneFolds.get(1).sourceCache,
                                                    tuneFolds.get(1).targetCache, tuneModel),
                                            tuneGold, ImmutableSet.of(EvaluatorType.F_MEASURE))
                                    .get(EvaluatorType.F_MEASURE);
                            if (current > bestFM) {
                                bestFM = current;
                                params = lps;
                            }
                        }
                    } else {
                        params = tAlgo.getMlParameter();
                    }
                    long begin = System.currentTimeMillis();
                    // train
                    MLResults model = trainModel(algorithm, params, trainingData,
                            dataset.evalData.getConfigReader().read(), trainSourceCache, trainTargetCache);
                    AMapping prediction = algorithm.predict(testSourceCache, testTargetCache, model);
                    double runTime = ((double) (System.currentTimeMillis() - begin)) / 1000.0;
                    algoMappings.put(tAlgo.getName(), prediction);
                    EvaluationRun er = new EvaluationRun(tAlgo.getName(), tAlgo.getMlType().toString(),
                            dataset.dataName, eval.evaluate(prediction, goldStandard, qlMeasures), k,
                            model.getLinkSpecification());
                    er.setQuanititativeRecord(new RunRecord(k, runTime, 0.0, model.getLinkSpecification().size()));
                    er.display();
                    runsList.add(er);
                }
                // Calculate successes and failures for this fold
                updateSuccessesAndFailures(algoMappings, testData);
            }
            // Perform test for this dataset
            for (String a : successesAndFailures.keySet()) {
                for (String b : successesAndFailures.get(a).keySet()) {
                    double pValue = McNemarsTest.calculate(successesAndFailures.get(a).get(b));
                    addToMapMapMap(statisticalTestResults, dataset.dataName, a, b, pValue);
                }
            }
        }
        System.out.println(statisticalTestResults);
        Summary summary = new Summary(runsList, foldNumber);
        summary.setStatisticalTestResults(statisticalTestResults);
        return summary;
    }

    private void updateSuccessesAndFailures(Map<String, AMapping> algoMappings, FoldData testData) {
        for (String a : algoMappings.keySet()) {
            for (String b : algoMappings.keySet()) {
                if (!a.equals(b)) {
                    if ((successesAndFailures.get(a) == null || successesAndFailures.get(a).get(b) == null)
                            && successesAndFailures.get(b) == null) {
                        int successes = McNemarsTest.getSuccesses(algoMappings.get(a), algoMappings.get(b),
                                testData.map);
                        int failures = McNemarsTest.getSuccesses(algoMappings.get(b), algoMappings.get(a),
                                testData.map);
                        addToMapMap(successesAndFailures, a, b, new int[] { successes, failures });
                    } else if (successesAndFailures.get(a) != null && successesAndFailures.get(a).get(b) != null) {
                        int successes = McNemarsTest.getSuccesses(algoMappings.get(a), algoMappings.get(b),
                                testData.map);
                        int failures = McNemarsTest.getSuccesses(algoMappings.get(b), algoMappings.get(a),
                                testData.map);
                        int[] previous = successesAndFailures.get(a).get(b);
                        previous[0] += successes;
                        previous[1] += failures;
                    }
                }
            }
        }
    }

    private <T, S, U> void addToMapMap(Map<T, Map<S, U>> mapmap, T key, S subMapKey, U item) {
        if (!mapmap.containsKey(key)) {
            Map<S, U> subMap = new HashMap<>();
            subMap.put(subMapKey, item);
            mapmap.put(key, subMap);
        } else {
            Map<S, U> subMap;
            subMap = ((subMap = mapmap.get(key)) != null) ? subMap : new HashMap<>();
            subMap.put(subMapKey, item);
            mapmap.put(key, subMap);
        }
    }

    private <V, T, S, U> void addToMapMapMap(Map<V, Map<T, Map<S, U>>> mapmapmap, V key, T subMapKey, S subsubMapKey,
                                             U item) {
        if (!mapmapmap.containsKey(key)) {
            Map<T, Map<S, U>> subMap = new HashMap<>();
            Map<S, U> subsubMap = new HashMap<>();
            subsubMap.put(subsubMapKey, item);
            subMap.put(subMapKey, subsubMap);
            mapmapmap.put(key, subMap);
        } else {
            Map<T, Map<S, U>> subMap = mapmapmap.get(key);
            Map<S, U> subsubMap = null;
            if (!subMap.containsKey(subMapKey)) {
                subsubMap = new HashMap<>();
            } else {
                subsubMap = subMap.get(subMapKey);
            }
            subsubMap.put(subsubMapKey, item);
            subMap.put(subMapKey, subsubMap);
            mapmapmap.put(key, subMap);
        }
    }

    private MLResults trainModel(AMLAlgorithm algorithm, List<LearningParameter> params, AMapping trainingData,
                                 Configuration config, ACache trainSourceCache, ACache trainTargetCache) {
        algorithm.init(params, trainSourceCache, trainTargetCache);
        algorithm.getMl().setConfiguration(config);
        MLResults model = null;
        try {
            if (algorithm instanceof SupervisedMLAlgorithm)
                model = algorithm.asSupervised().learn(trainingData);
            else if (algorithm instanceof ActiveMLAlgorithm)
                model = algorithm.asActive().activeLearn(trainingData);
        } catch (UnsupportedMLImplementationException e) {
            e.printStackTrace();
        }
        return model;
    }

    private List<FoldData> createTuneFolds(FoldData trainData, double factor) {
        AMapping tuneTraining = MappingFactory.createDefaultMapping();
        AMapping tuneTest = MappingFactory.createDefaultMapping();
        int tuneTrainingSize = (int) Math.ceil(trainData.map.size() / 5d);
        for (String key : trainData.map.getMap().keySet()) {
            if (tuneTraining.size() < tuneTrainingSize) {
                tuneTraining.add(key, trainData.map.getMap().get(key));
            } else {
                tuneTest.add(key, trainData.map.getMap().get(key));
            }
        }
        List<AMapping> mappings = new ArrayList<>();
        mappings.add(tuneTraining);
        mappings.add(tuneTest);
        return createFoldDataFromCaches(mappings, trainData.sourceCache, trainData.targetCache);
    }

    public Set<List<LearningParameter>> createParameterGrid(Map<LearningParameter, List<Object>> parameters) {
        List<Set<LearningParameter>> grid = new ArrayList<>();
        for (LearningParameter lp : parameters.keySet()) {
            Set<LearningParameter> parameterPossibilites = new HashSet<>();
            for (Object value : parameters.get(lp)) {
                parameterPossibilites.add(new LearningParameter(lp.getName(), value));
            }
            grid.add(parameterPossibilites);
        }
        return Sets.cartesianProduct(grid);
    }

    public static void main(String[] args) throws UnsupportedMLImplementationException {
        TaskAlgorithm w = new TaskAlgorithm(MLImplementationType.SUPERVISED_BATCH,
                MLAlgorithmFactory.createMLAlgorithm(WombatSimple.class, MLImplementationType.SUPERVISED_BATCH), null);
        TaskAlgorithm w2 = new TaskAlgorithm(MLImplementationType.SUPERVISED_BATCH,
                MLAlgorithmFactory.createMLAlgorithm(WombatSimple.class, MLImplementationType.SUPERVISED_BATCH), null);
        w.setMlParameterValues(wombatDefaultParams(0.95, 0.99, 0.9, 0.95));
        w2.setMlParameterValues(wombatDefaultParams(0.35, 0.4, 0.85, 0.9));
        w.setName("WOMBAT HIGH");
        w2.setName("WOMBAT LOW");
        EvaluationData eval = DataSetChooser.getData("restaurants");
        TaskData td = new TaskData(new GoldStandard(eval.getReferenceMapping(), eval.getSourceCache().getAllUris(),
                eval.getTargetCache().getAllUris()), eval.getSourceCache(), eval.getTargetCache(), eval);
        td.dataName = eval.getName();
        List<TaskAlgorithm> algos = new ArrayList<>();
        Set<TaskData> data = new HashSet<>();
        Set<EvaluatorType> measures = new HashSet<>();
        algos.add(w);
        algos.add(w2);
        data.add(td);
        measures.add(EvaluatorType.F_MEASURE);
        System.out.println(new Evaluator().crossValidateWithTuningAndStatisticalTest(algos, data, measures, 3));
    }

    // DELETE ME
    private static Map<LearningParameter, List<Object>> wombatDefaultParams(double... p) {
        // default parameters
        long maxRefineTreeSize = 2000;
        int maxIterationNumber = 3;
        int maxIterationTimeInMin = 20;
        int maxExecutionTimeInMin = 600;
        double maxFitnessThreshold = 1;
        double childrenPenaltyWeight = 1;
        double complexityPenaltyWeight = 1;
        boolean saveMapping = true;
        double minPropertyCoverage = 0.4;
        double propertyLearningRate = 0.9;
        double overallPenaltyWeight = 0.5d;
        boolean verbose = false;
        Set<String> simMeasures = new HashSet<>(Arrays.asList("jaccard", "trigrams", "cosine", "qgrams"));

        LearningParameter lp1 = new LearningParameter(AWombat.PARAMETER_MAX_REFINEMENT_TREE_SIZE, maxRefineTreeSize,
                Long.class, 10d, Long.MAX_VALUE, 10d, AWombat.PARAMETER_MAX_REFINEMENT_TREE_SIZE);
        LearningParameter lp2 = new LearningParameter(AWombat.PARAMETER_MAX_ITERATIONS_NUMBER, maxIterationNumber,
                Integer.class, 1d, Integer.MAX_VALUE, 10d, AWombat.PARAMETER_MAX_ITERATIONS_NUMBER);
        LearningParameter lp3 = new LearningParameter(AWombat.PARAMETER_MAX_ITERATION_TIME_IN_MINUTES,
                maxIterationTimeInMin, Integer.class, 1d, Integer.MAX_VALUE, 1,
                AWombat.PARAMETER_MAX_ITERATION_TIME_IN_MINUTES);
        LearningParameter lp4 = new LearningParameter(AWombat.PARAMETER_EXECUTION_TIME_IN_MINUTES,
                maxExecutionTimeInMin, Integer.class, 1d, Integer.MAX_VALUE, 1,
                AWombat.PARAMETER_EXECUTION_TIME_IN_MINUTES);
        LearningParameter lp5 = new LearningParameter(AWombat.PARAMETER_MAX_FITNESS_THRESHOLD, maxFitnessThreshold,
                Double.class, 0d, 1d, 0.01d, AWombat.PARAMETER_MAX_FITNESS_THRESHOLD);
        LearningParameter lpMinPropC = new LearningParameter(AWombat.PARAMETER_MIN_PROPERTY_COVERAGE,
                minPropertyCoverage, Double.class, 0d, 1d, 0.01d, AWombat.PARAMETER_MIN_PROPERTY_COVERAGE);
        LearningParameter lpMinPropLR = new LearningParameter(AWombat.PARAMETER_PROPERTY_LEARNING_RATE,
                propertyLearningRate, Double.class, 0d, 1d, 0.01d, AWombat.PARAMETER_PROPERTY_LEARNING_RATE);
        LearningParameter lp8 = new LearningParameter(AWombat.PARAMETER_OVERALL_PENALTY_WEIGHT, overallPenaltyWeight,
                Double.class, 0d, 1d, 0.01d, AWombat.PARAMETER_OVERALL_PENALTY_WEIGHT);
        LearningParameter lp9 = new LearningParameter(AWombat.PARAMETER_CHILDREN_PENALTY_WEIGHT, childrenPenaltyWeight,
                Double.class, 0d, 1d, 0.01d, AWombat.PARAMETER_CHILDREN_PENALTY_WEIGHT);
        LearningParameter lp10 = new LearningParameter(AWombat.PARAMETER_COMPLEXITY_PENALTY_WEIGHT,
                complexityPenaltyWeight, Double.class, 0d, 1d, 0.01d, AWombat.PARAMETER_COMPLEXITY_PENALTY_WEIGHT);
        LearningParameter lp11 = new LearningParameter(AWombat.PARAMETER_VERBOSE, verbose, Boolean.class, 0, 1, 0,
                AWombat.PARAMETER_VERBOSE);
        LearningParameter lp12 = new LearningParameter(AWombat.PARAMETER_ATOMIC_MEASURES, simMeasures,
                MeasureType.class, 0, 0, 0, AWombat.PARAMETER_ATOMIC_MEASURES);
        LearningParameter lp13 = new LearningParameter(AWombat.PARAMETER_SAVE_MAPPING, saveMapping, Boolean.class, 0, 1,
                0, AWombat.PARAMETER_SAVE_MAPPING);

        List<Object> lp1Values = ImmutableList.of(maxRefineTreeSize);
        List<Object> lp2Values = ImmutableList.of(maxIterationNumber);
        List<Object> lp3Values = ImmutableList.of(maxIterationTimeInMin);
        List<Object> lp4Values = ImmutableList.of(maxExecutionTimeInMin);
        List<Object> lp5Values = ImmutableList.of(maxFitnessThreshold);
        List<Object> lpMinPropCValues = ImmutableList.of(p[0], p[1]);
        List<Object> lpMinPropLRValues = ImmutableList.of(p[2], p[3]);
        List<Object> lp8Values = ImmutableList.of(overallPenaltyWeight);
        List<Object> lp9Values = ImmutableList.of(childrenPenaltyWeight);
        List<Object> lp10Values = ImmutableList.of(complexityPenaltyWeight);
        List<Object> lp11Values = ImmutableList.of(verbose);
        List<Object> lp12Values = ImmutableList.of(simMeasures);
        List<Object> lp13Values = ImmutableList.of(saveMapping);
        Map<LearningParameter, List<Object>> params = new HashMap<>();
        params.put(lp1, lp1Values);
        params.put(lp2, lp2Values);
        params.put(lp3, lp3Values);
        params.put(lp4, lp4Values);
        params.put(lp5, lp5Values);
        params.put(lpMinPropC, lpMinPropCValues);
        params.put(lpMinPropLR, lpMinPropLRValues);
        params.put(lp8, lp8Values);
        params.put(lp9, lp9Values);
        params.put(lp10, lp10Values);
        params.put(lp11, lp11Values);
        params.put(lp12, lp12Values);
        params.put(lp13, lp13Values);
        return params;
    }

    private FoldData getTrainingFold(List<FoldData> folds, int k, int foldNumber) {
        FoldData trainData = new FoldData();
        // perform union on test folds
        for (int i = 0; i < foldNumber; i++) {
            if (i != k) {
                trainData.map = MappingOperations.union(trainData.map, folds.get(i).map);
                trainData.sourceCache = cacheUnion(trainData.sourceCache, folds.get(i).sourceCache);
                trainData.targetCache = cacheUnion(trainData.targetCache, folds.get(i).targetCache);
            }
        }
        return trainData;
    }

    private FoldData fixCachesIfNecessary(FoldData trainData, TaskData dataset) {
        // fix caches if necessary
        for (String s : trainData.map.getMap().keySet()) {
            for (String t : trainData.map.getMap().get(s).keySet()) {
                if (!trainData.targetCache.containsUri(t)) {
                    // logger.info("target: " + t);
                    trainData.targetCache.addInstance(dataset.target.getInstance(t));
                }
            }
            if (!trainData.sourceCache.containsUri(s)) {
                // logger.info("source: " + s);
                trainData.sourceCache.addInstance(dataset.source.getInstance(s));
            }
        }
        return trainData;
    }

    public ACache cacheUnion(ACache a, ACache b) {
        ACache result = new HybridCache();
        for (Instance i : a.getAllInstances()) {
            result.addInstance(i);
        }
        for (Instance i : b.getAllInstances()) {
            result.addInstance(i);
        }
        return result;
    }

    public List<FoldData> generateFolds(EvaluationData data, int foldNumber, boolean withNegativeExamples) {

        // Fill caches
        ACache source = data.getSourceCache();
        ACache target = data.getTargetCache();
        AMapping refMap = data.getReferenceMapping();

        // remove error mappings (if any)
        refMap = removeLinksWithNoInstances(refMap, source, target);

        // generate AMapping folds
        List<AMapping> foldMaps = generateMappingFolds(refMap, source, target, foldNumber, withNegativeExamples);

        // fill fold caches
        return createFoldDataFromCaches(foldMaps, source, target);
    }

    private List<FoldData> createFoldDataFromCaches(List<AMapping> foldMaps, ACache source, ACache target) {
        List<FoldData> folds = new ArrayList<>();
        for (AMapping foldMap : foldMaps) {
            ACache sourceFoldCache = new HybridCache();
            ACache targetFoldCache = new HybridCache();
            for (String s : foldMap.getMap().keySet()) {
                if (source.containsUri(s)) {
                    sourceFoldCache.addInstance(source.getInstance(s));
                    for (String t : foldMap.getMap().get(s).keySet()) {
                        if (target.containsUri(t)) {
                            targetFoldCache.addInstance(target.getInstance(t));
                        } else {
                            // logger.warn("Instance " + t +
                            // " not exist in the target dataset");
                        }
                    }
                } else {
                    // logger.warn("Instance " + s +
                    // " not exist in the source dataset");
                }
            }
            folds.add(new FoldData(foldMap, sourceFoldCache, targetFoldCache));
        }
        return folds;
    }

    public List<AMapping> generateMappingFolds(AMapping refMap, ACache source, ACache target, int foldNumber,
                                               boolean withNegativeExamples) {
        Random rand = new Random();
        List<AMapping> foldMaps = new ArrayList<>();
        int mapSize = refMap.getMap().keySet().size();
        int foldSize = (int) (mapSize / foldNumber);

        Iterator<HashMap<String, Double>> it = refMap.getMap().values().iterator();
        ArrayList<String> values = new ArrayList<String>();
        while (it.hasNext()) {
            for (String t : it.next().keySet()) {
                values.add(t);
            }
        }
        for (int foldIndex = 0; foldIndex < foldNumber; foldIndex++) {
            Set<Integer> index = new HashSet<>();
            // get random indexes
            while (index.size() < foldSize) {
                int number;
                do {
                    number = (int) (mapSize * Math.random());
                } while (index.contains(number));
                index.add(number);
            }
            // get data
            AMapping foldMap = MappingFactory.createDefaultMapping();
            int count = 0;
            for (String key : refMap.getMap().keySet()) {
                if (foldIndex != foldNumber - 1) {
                    if (index.contains(count) && count % 2 == 0) {
                        HashMap<String, Double> help = new HashMap<String, Double>();
                        for (String k : refMap.getMap().get(key).keySet()) {
                            help.put(k, 1.0);
                        }
                        foldMap.getMap().put(key, help);
                    } else if (withNegativeExamples && index.contains(count)) {
                        HashMap<String, Double> help = new HashMap<String, Double>();
                        help.put(getRandomTargetInstance(source, target, values, rand, refMap.getMap(), key, -1), 0.0);
                        foldMap.getMap().put(key, help);
                    }
                } else {
                    if (index.contains(count)) {
                        HashMap<String, Double> help = new HashMap<String, Double>();
                        for (String k : refMap.getMap().get(key).keySet()) {
                            help.put(k, 1.0);
                        }
                        foldMap.getMap().put(key, help);
                    }
                }
                count++;
            }

            foldMaps.add(foldMap);
            refMap = removeSubMap(refMap, foldMap);
        }
        int i = 0;
        int odd = 0;
        // if any remaining links in the refMap, then distribute them to all
        // folds
        for (String key : refMap.getMap().keySet()) {
            if (i != foldNumber - 1) {
                if (odd % 2 == 0) {
                    HashMap<String, Double> help = new HashMap<String, Double>();
                    for (String k : refMap.getMap().get(key).keySet()) {
                        help.put(k, 1.0);
                    }
                    foldMaps.get(i).add(key, help);
                } else {

                    HashMap<String, Double> help = new HashMap<String, Double>();
                    help.put(getRandomTargetInstance(source, target, values, rand, refMap.getMap(), key, -1), 0.0);
                    foldMaps.get(i).add(key, help);
                }
            } else {
                HashMap<String, Double> help = new HashMap<String, Double>();
                for (String k : refMap.getMap().get(key).keySet()) {
                    help.put(k, 1.0);
                }
                foldMaps.get(i).add(key, help);

            }
            odd++;
            i = (i + 1) % foldNumber;
        }
        return foldMaps;
    }

    public static String getRandomTargetInstance(ACache source, ACache target, List<String> values, Random random,
                                                 HashMap<String, HashMap<String, Double>> refMap, String sourceInstance, int previousRandom) {
        int randomInt;
        do {
            randomInt = random.nextInt(values.size());
        } while (randomInt == previousRandom);

        String tmpTarget = values.get(randomInt);
        if (refMap.get(sourceInstance).get(tmpTarget) == null && target.getInstance(tmpTarget) != null) {
            return tmpTarget;
        }
        return getRandomTargetInstance(source, target, values, random, refMap, sourceInstance, randomInt);
    }

    public AMapping removeSubMap(AMapping mainMap, AMapping subMap) {
        AMapping result = MappingFactory.createDefaultMapping();
        double value = 0;
        for (String mainMapSourceUri : mainMap.getMap().keySet()) {
            for (String mainMapTargetUri : mainMap.getMap().get(mainMapSourceUri).keySet()) {
                if (!subMap.contains(mainMapSourceUri, mainMapTargetUri)) {
                    result.add(mainMapSourceUri, mainMapTargetUri, value);
                }
            }
        }
        return result;
    }

    private AMapping removeLinksWithNoInstances(AMapping map, ACache source, ACache target) {
        AMapping result = MappingFactory.createDefaultMapping();
        for (String s : map.getMap().keySet()) {
            for (String t : map.getMap().get(s).keySet()) {
                if (source.containsUri(s) && target.containsUri(t)) {
                    result.add(s, t, map.getMap().get(s).get(t));
                }
            }
        }
        return result;
    }

    /**
     * It provides the feedback of the oracle by comparing the prediction to the
     * reference mapping.It is used by evaluator in the Supervised_Active.
     *
     * @param predictionMapping
     *            The predictions created by a machine learning
     * @param referenceMapping
     *            The gold standard to evaluate the prediction
     * @return Mapping - The mappings from the predictions that exist in the gold
     *         standard
     */
    private AMapping oracleFeedback(AMapping predictionMapping, AMapping referenceMapping) {
        AMapping result = MappingFactory.createDefaultMapping();

        for (String s : predictionMapping.getMap().keySet()) {
            for (String t : predictionMapping.getMap().get(s).keySet()) {
                if (referenceMapping.contains(s, t)) {
                    // result.add(s, t, predictionMapping.getMap().get(s).get(t));
                    result.add(s, t, 1.0);
                } else {
                    result.add(s, t, 0.0);
                }
            }
        }
        return result;
    }
    /*
     * It displays the overall results of evaluating machine learning algorithm
     * regarding qualitative measures
     *
     * @param results The evaluation results
     * algorith:datast:evaluation_measure:score
     */
    /*
     * public static void dsiplayOverallResults(Table<String, String,
     * Map<EvaluatorType, Double>> results) { for (String mlAlgorithm :
     * results.rowKeySet()) { for (String dataset : results.columnKeySet()) { for
     * (EvaluatorType measure : results.get(mlAlgorithm, dataset).keySet()) {
     * System.out.println(mlAlgorithm+"\t"+dataset+"\t"+measure+"\t"+results.get(
     * mlAlgorithm, dataset).get(measure)); } } } }
     */

    /*
     * It displays the results of evaluating a machine learning algorithm regarding
     * qualitative measures
     *
     * @param results The evaluation evaluation_measure:score
     */
    /*
     * public static void dsiplayResults(Map<EvaluatorType, Double> results) { for
     * (EvaluatorType measure : results.keySet()) {
     * System.out.println(measure+"\t"+results.get(measure)); }
     *
     * }
     */

}
