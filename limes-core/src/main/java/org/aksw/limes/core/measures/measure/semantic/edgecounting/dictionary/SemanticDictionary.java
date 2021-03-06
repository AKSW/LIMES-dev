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
package org.aksw.limes.core.measures.measure.semantic.edgecounting.dictionary;

import edu.mit.jwi.IRAMDictionary;
import edu.mit.jwi.RAMDictionary;
import edu.mit.jwi.data.ILoadPolicy;
import edu.mit.jwi.item.*;
import org.aksw.limes.core.exceptions.semanticDictionary.ExportedSemanticDictionaryFileNotFound;
import org.aksw.limes.core.exceptions.semanticDictionary.SemanticDictionaryNotFound;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Implements the semantic dictionary (wordnet) class. Responsible for loading,
 * exporting and closing the semantic dictionary.
 *
 * @author Kleanthi Georgala (georgala@informatik.uni-leipzig.de)
 * @version 1.0
 *
 */
public class SemanticDictionary {
    private IRAMDictionary dictionary = null;
    private String wordNetFolder = System.getProperty("user.dir") + "/src/main/resources/wordnet/dict/";
    private String exFile = wordNetFolder + "JWI_Export_.wn";
    private static final Logger logger = LoggerFactory.getLogger(SemanticDictionary.class);

    /**
     * Exports the wordnet database files into one file.
     *
     */
    public void exportDictionaryToFile() {
        File dictionaryFolder = new File(wordNetFolder);

        if (!dictionaryFolder.exists()) {
            logger.error("Wordnet dictionary folder doesn't exist. Can't do anything");
            throw new SemanticDictionaryNotFound("Wordnet dictionary could not be found.\n "
                    + "Please read the instructions in the README.md file on how to download the worndet database files.\n");
        } else {
            File dicFile = new File(exFile);
            if (!dicFile.exists()) {
                logger.info("No exported wordnet file is found. Creating one..");
                dictionary = new RAMDictionary(dictionaryFolder);
                dictionary.setLoadPolicy(ILoadPolicy.IMMEDIATE_LOAD);
                logger.info("Loaded dictionary into memory. Now exporting it to file.");
                try {
                    dictionary.open();
                    dictionary.export(new FileOutputStream(dicFile));
                } catch (IOException e1) {
                    logger.error("Couldn't open wordnet dictionary. Exiting..");
                    e1.printStackTrace();
                    throw new RuntimeException();
                } finally {
                    removeDictionary();
                }
            }
        }

    }

    /**
     * Closes and removes the semantic dictionary from memory
     *
     */
    public void removeDictionary() {
        if (dictionary != null) {
            dictionary.close();
            dictionary = null;
        }
    }

    /**
     * Opens the exported wordnet file and loads it to memory
     *
     */
    public void openDictionaryFromFile() {
        File dicFile = new File(exFile);

        if (!dicFile.exists()) {
            throw new ExportedSemanticDictionaryFileNotFound("No exported wordnet file is found. Exiting..\n"
                    + "Please execute the exportDictionaryToFile() function first.");
        } else {
            if (dictionary == null) {
                dictionary = new RAMDictionary(dicFile);
                try {
                    dictionary.open();
                } catch (IOException e2) {
                    logger.error("Couldn't open wordnet dictionary. Exiting..");
                    e2.printStackTrace();
                    throw new RuntimeException();
                }
            }
        }

    }

    /**
     * Retrieves the corresponding IWord of an IWordID in wordnet
     *
     * @param wordID,
     *            the input IWordID
     *
     * @return the resulting IWord
     */
    public IWord getWord(IWordID wordID) {
        return dictionary.getWord(wordID);
    }

    /**
     * Retrieves the corresponding IIndexWord of a string in wordnet
     *
     * @param str,
     *            the input string
     * @param pos,
     *            the input string's POS
     *
     * @return the resulting IIndexWord
     */
    public IIndexWord getIndexWord(String str, POS pos) {
        return dictionary.getIndexWord(str, pos);
    }

    /**
     * Retrieves the corresponding ISynset of ISynsetID in wordnet
     *
     * @param hypernymId,
     *            the input ISynsetID
     *
     * @return the resulting ISynset
     */
    public ISynset getSynset(ISynsetID hypernymId) {
        return dictionary.getSynset(hypernymId);
    }

    /**
     * Retrieves the semantic wordnet dictionary
     *
     * @return the dictionary
     */
    public IRAMDictionary getDictionary() {
        return dictionary;
    }
}
