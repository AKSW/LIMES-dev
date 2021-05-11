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
package org.aksw.limes.core.ml.matching;

import org.aksw.limes.core.io.config.KBInfo;
import org.aksw.limes.core.io.mapping.AMapping;
import org.aksw.limes.core.io.mapping.MappingFactory;
import org.aksw.limes.core.io.query.ModelRegistry;
import org.aksw.limes.core.io.query.QueryModuleFactory;
import org.aksw.limes.core.ml.algorithm.matching.DefaultPropertyMapper;
import org.apache.jena.rdf.model.Model;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DefaultPropertyMapperTest {

    KBInfo sKB = new KBInfo();
    KBInfo tKB = new KBInfo();
    Model sModel;
    Model tModel;

    @Before
    public void init() {
        String base = "/datasets/";
        sKB.setEndpoint(LabelBasedClassMapperTest.class.getResource(base + "Persons1/person11.nt").getPath());
        sKB.setGraph(null);
        sKB.setPageSize(1000);
        sKB.setId("person11");
        tKB.setEndpoint(LabelBasedClassMapperTest.class.getResource(base + "Persons1/person12.nt").getPath());
        tKB.setGraph(null);
        tKB.setPageSize(1000);
        tKB.setId("person12");
        QueryModuleFactory.getQueryModule("nt", sKB);
        QueryModuleFactory.getQueryModule("nt", tKB);
        sModel = ModelRegistry.getInstance().getMap().get(sKB.getEndpoint());
        tModel = ModelRegistry.getInstance().getMap().get(tKB.getEndpoint());
    }

    @Test
    public void testGetPropertyMapping() {
        DefaultPropertyMapper mapper = new DefaultPropertyMapper(sModel, tModel);
        AMapping m = mapper.getPropertyMapping(sKB.getEndpoint(), tKB.getEndpoint(),
                "http://www.okkam.org/ontology_person1.owl#Person", "http://www.okkam.org/ontology_person2.owl#Person");
        AMapping correctMapping = MappingFactory.createDefaultMapping();
        correctMapping.add("http://www.okkam.org/ontology_person1.owl#surname",
                "http://www.okkam.org/ontology_person2.owl#surname", 61.0);
        correctMapping.add("http://www.okkam.org/ontology_person1.owl#date_of_birth",
                "http://www.okkam.org/ontology_person2.owl#phone_numer", 5.0);
        correctMapping.add("http://www.okkam.org/ontology_person1.owl#given_name",
                "http://www.okkam.org/ontology_person2.owl#given_name", 141.0);
        correctMapping.add("http://www.okkam.org/ontology_person1.owl#age",
                "http://www.okkam.org/ontology_person2.owl#age", 24.0);
        correctMapping.add("http://www.okkam.org/ontology_person1.owl#phone_numer",
                "http://www.okkam.org/ontology_person2.owl#date_of_birth", 15.0);
        assertEquals(correctMapping, m);
    }
}
