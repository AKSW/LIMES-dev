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
package org.aksw.limes.core.evaluation.qualititativeMeasures;

import org.aksw.limes.core.datastrutures.GoldStandard;
import org.aksw.limes.core.io.mapping.AMapping;
import org.aksw.limes.core.io.mapping.MemoryMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * It measures how far the algorithm retrieved correct results out of the all existed correct results.<br>
 * It is defined to be the ratio between the true positive to the total number of correct results whether retrieved or not
 *
 * @author Mofeed Hassan (mounir@informatik.uni-leipzig.de)
 * @author Tommaso Soru (tsoru@informatik.uni-leipzig.de)
 * @version 1.0
 * @since 1.0
 */
public class Recall extends APRF implements IQualitativeMeasure {
    static Logger logger = LoggerFactory.getLogger(Recall.class);

    /**
     * The method calculates the recall of the machine learning predictions compared to a gold standard
     * @param predictions The predictions provided by a machine learning algorithm
     * @param goldStandard It contains the gold standard (reference mapping) combined with the source and target URIs
     * @return double - This returns the calculated recall
     */
    @Override
    public double calculate(AMapping predictions, GoldStandard goldStandard) {
        if (predictions.size() == 0)
            return 0;
        return trueFalsePositive(predictions, goldStandard.referenceMappings, true)
                / (double) ((MemoryMapping) goldStandard.referenceMappings).getNumberofPositiveMappings();
    }

}
