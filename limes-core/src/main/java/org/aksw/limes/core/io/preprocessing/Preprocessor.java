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
package org.aksw.limes.core.io.preprocessing;

import org.aksw.limes.core.io.cache.ACache;
import org.aksw.limes.core.io.cache.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * This class is used to apply preprocessing functions to caches
 *
 * @author Daniel Obraczka
 */
public class Preprocessor {
    static Logger logger = LoggerFactory.getLogger(Preprocessor.class.getName());


    /**
     * Calls {@link #applyFunctionsToCache(ACache, Map, boolean)} with <code>cloneCache=false</code>
     * @param cache cache that should be processed
     * @param functions preprocessing functions that will be applied
     * @return cache with instances on which the preprocessing functions where applied
     */
    public static ACache applyFunctionsToCache(ACache cache, Map<String, Map<String, String>> functions) {
        return applyFunctionsToCache(cache, functions, false);
    }

    /**
     *
     * @param cache cache that should be processed
     * @param functions preprocessing functions that will be applied
     * @param cloneCache if true, the cache will be cloned and the given cache is left as is
     * @return cache with instances on which the preprocessing functions where applied, if cloneCache was set to true this a different cache than the provided
     */
    public static ACache applyFunctionsToCache(ACache cache, Map<String, Map<String, String>> functions, boolean cloneCache) {
        ACache cacheClone;
        if(cloneCache){
            cacheClone = cache.clone();
        }else{
            cacheClone = cache;
        }
        functions.forEach((property, innerMap) -> {
            innerMap.forEach((propertyDub, functionChain) -> {
                for(Instance inst : cacheClone.getAllInstances()){
                    logger.debug("Function chain = " + functionChain);
                    applyRenameIfNecessary(inst, property, propertyDub);
                    if (functionChain != null) {
                        if (!functionChain.equals("")) {
                            String split[] = functionChain.split("->");
                            for (int i = 0; i < split.length; i++) {
                                String functionId = getFunctionId(split[i]);
                                PreprocessingFunctionType type = PreprocessingFunctionFactory
                                        .getPreprocessingType(functionId);
                                APreprocessingFunction func = PreprocessingFunctionFactory.getPreprocessingFunction(type);
                                String[] arguments = func.retrieveArguments(split[i]);
                                if (arguments.length > 0) {
                                    func.applyFunction(inst, propertyDub, arguments);
                                } else {
                                    func.applyFunction(inst, propertyDub);
                                }
                            }
                        }
                    }
                }
            });
        });
        return cacheClone;
    }

    public static void applyRenameIfNecessary(Instance inst, String property, String propertyDub) {
        if (property != null && !property.equals("") && propertyDub != null && !propertyDub.equals("")
                && !property.equals(propertyDub)) {
            PreprocessingFunctionType type = PreprocessingFunctionFactory
                    .getPreprocessingType(PreprocessingFunctionFactory.RENAME_PROPERTY);
            APreprocessingFunction func = PreprocessingFunctionFactory.getPreprocessingFunction(type);
            func.applyFunction(inst, property, propertyDub);
        }
    }

    public static String getFunctionId(String argString){
        return argString.split("\\(")[0];
    }

}
