/**
 *
 */
package org.aksw.limes.core.measures.measure.string;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.aksw.limes.core.datastrutures.GoldStandard;
import org.aksw.limes.core.evaluation.evaluator.EvaluatorType;
import org.aksw.limes.core.evaluation.qualititativeMeasures.QualitativeMeasuresEvaluator;
import org.aksw.limes.core.io.mapping.AMapping;
import org.aksw.limes.core.io.mapping.MappingFactory;
import org.junit.Before;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;

/**
 * A gold standard test for the doc2vec measure. I use a prepared dataset of normal English and simple English Wikipedia abstracts
 * of about 90 common topics. Clearly the two versions on the same topic should tend to be more similar to each other than to most
 * other articles. Unfortunately, my current doc2vec measure does shows rather small correlations.
 *
 * @author Swante Scholz
 */
public class Doc2VecMeasuresGoldStandardTest {
    
    public static final double epsilon = 0.00001;
    static String basePath = "src/test/resources/";
    
    GoldStandard goldStandard;
    AMapping predictions = MappingFactory.createDefaultMapping();
    
    private String vec2String(INDArray vector) {
        return Arrays.stream(vector.data().asDouble()).mapToObj(it -> ""+it ).collect( Collectors.joining( " " ) );
    }
    
    @Before
    public void setupData() throws IOException {
        Doc2VecMeasure measure = new Doc2VecMeasure(
            Doc2VecMeasure.DEFAULT_PRECOMPUTED_VECTORS_FILE_PATH);
        ArrayList<String> names = new ArrayList<String>();
        ArrayList<String> sourceUris = new ArrayList<String>();
        ArrayList<String> targetUris = new ArrayList<String>();
        AMapping goldMapping = MappingFactory.createDefaultMapping();
        ArrayList<String> simpleAbstracts = new ArrayList<String>();
        ArrayList<String> normalAbstracts = new ArrayList<String>();
        
        Files.readAllLines(
            new File(basePath, "simple-and-normal-english-wiki-abstracts.csv").toPath()).
            forEach(line -> {
                String[] parts = line.split("\t");
                names.add(parts[0]);
                String sourceUri = "http://a.de/" + parts[0];
                String targetUri = "http://b.de/" + parts[0];
                sourceUris.add(sourceUri);
                targetUris.add(targetUri);
                String simpleAbstract = parts[1];
                String normalAbstract = parts[2];
                simpleAbstracts.add(simpleAbstract);
                normalAbstracts.add(normalAbstract);
                goldMapping.add(sourceUri, targetUri, 1);
            });
        List<INDArray> simpleVectors = simpleAbstracts.stream().map(it ->  measure.inferVector(it)).collect(
            Collectors.toList());
        List<INDArray> normalVectors = normalAbstracts.stream().map(it -> {return measure.inferVector(it);}).collect(
            Collectors.toList());
//        for (int i = 0; i < sourceUris.size(); i++) {
//            System.out.println(names.get(i));
//            System.out.println(Arrays.stream(simpleVectors.get(i).data().asDouble()).mapToObj(it -> ""+it ).collect( Collectors.joining( " " ) ));
//            System.out.println(Arrays.stream(normalVectors.get(i).data().asDouble()).mapToObj(it -> ""+it ).collect( Collectors.joining( " " ) ));
//        }
//        System.exit(0);
        for (int i = 0; i < sourceUris.size(); i++) {
            double bestSim = Double.MAX_VALUE;
            int bestJ = -1;
            INDArray simpleVector = simpleVectors.get(i);
            for (int j = 0; j < targetUris.size(); j++) {
                INDArray normalVector = normalVectors.get(j);
                double sim = Doc2VecMeasure.getSimilarityForInferredVectors(simpleVector, normalVector);
//                System.out.println(vec2String(simpleVector));
//                System.out.println(vec2String(normalVector));
//                System.out.println(sim);
//                System.exit(0);
                if (sim < bestSim) {
                    bestSim = sim;
                    bestJ = j;
                }
                if (sim > 0.944644824763) {
                    predictions.add(sourceUris.get(i), targetUris.get(j), 1);
//                    System.out.println(names.get(i) + " " + names.get(j) + " " + sim);
                }
            }
//			predictions.add(sourceUris.get(i), targetUris.get(bestJ), 1);
        }
        goldStandard = new GoldStandard(goldMapping, sourceUris, targetUris);
    }
	
	/*
with correct threshold (with lower epsilon)
precision: 0.06666666666666667
recall: 0.07777777777777778
fmeasure: 0.0717948717948718
accuracy: 0.9776543209876544
pprecision: 0.3761904761904762
precall: 0.4388888888888889
pfmeasure: 0.4051282051282051
	
	with 9.55:
	precision: 0.011111111111111112
recall: 0.05555555555555555
fmeasure: 0.018518518518518517
accuracy: 0.9345679012345679
pprecision: 0.10555555555555556
precall: 0.5277777777777778
pfmeasure: 0.17592592592592593
	
	with threshold 0.7
	precision: 0.015741270749856897
	recall: 0.6111111111111112
	fmeasure: 0.030691964285714284
	accuracy: 0.5711111111111111
	pprecision: 0.024899828277046364
	precall: 0.9666666666666667
	pfmeasure: 0.04854910714285714
	
	with threshold 0.154033065905:
	precision: 0.01111934766493699
	recall: 1.0
	fmeasure: 0.021994134897360705
	accuracy: 0.011851851851851851
	pprecision: 0.01111934766493699
	precall: 1.0
	pfmeasure: 0.021994134897360705
	 */
    
    @Test
    public void testGoldenStandard() {
        Set<EvaluatorType> measures = initEvalMeasures();
        
        Map<EvaluatorType, Double> calculations = testQualitativeEvaluator(predictions,
            goldStandard, measures);
        
        double precision = calculations.get(EvaluatorType.PRECISION);
        System.out.println("precision: " + precision);
//		assertEquals(0.7, precision, epsilon);
        
        double recall = calculations.get(EvaluatorType.RECALL);
        System.out.println("recall: " + recall);
//		assertEquals(0.7, recall, epsilon);
        
        double fmeasure = calculations.get(EvaluatorType.F_MEASURE);
        System.out.println("fmeasure: " + fmeasure);
//		assertEquals(0.7, fmeasure, epsilon);
        
        double accuracy = calculations.get(EvaluatorType.ACCURACY);
        System.out.println("accuracy: " + accuracy);
//		assertEquals(0.94, accuracy, epsilon);
        
        double pprecision = calculations.get(EvaluatorType.P_PRECISION);
        System.out.println("pprecision: " + pprecision);
//		assertEquals(0.8, pprecision, epsilon);
        
        double precall = calculations.get(EvaluatorType.P_RECALL);
        System.out.println("precall: " + precall);
//		assertEquals(0.8, precall, epsilon);
        
        double pfmeasure = calculations.get(EvaluatorType.PF_MEASURE);
        System.out.println("pfmeasure: " + pfmeasure);
//		assertTrue(pfmeasure > 0.7 && pfmeasure < 0.9);
    }
    
    private Map<EvaluatorType, Double> testQualitativeEvaluator(AMapping predictions,
        GoldStandard gs, Set<EvaluatorType> evaluationMeasures) {
        return new QualitativeMeasuresEvaluator().evaluate(predictions, gs, evaluationMeasures);
    }
    
    private Set<EvaluatorType> initEvalMeasures() {
        Set<EvaluatorType> measures = new HashSet<EvaluatorType>();
        measures.add(EvaluatorType.PRECISION);
        measures.add(EvaluatorType.RECALL);
        measures.add(EvaluatorType.F_MEASURE);
        measures.add(EvaluatorType.ACCURACY);
        measures.add(EvaluatorType.P_PRECISION);
        measures.add(EvaluatorType.P_RECALL);
        measures.add(EvaluatorType.PF_MEASURE);
        return measures;
    }
    
}
