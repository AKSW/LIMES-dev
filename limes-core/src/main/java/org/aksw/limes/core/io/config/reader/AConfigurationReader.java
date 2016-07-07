package org.aksw.limes.core.io.config.reader;

import org.aksw.limes.core.io.config.Configuration;

/**
 * @author Mohamed Sherif (sherif@informatik.uni-leipzig.de)
 * @version Nov 12, 2015
 */
public abstract class AConfigurationReader {

    protected String fileNameOrUri = new String();

    protected Configuration configuration = new Configuration();

    public AConfigurationReader(String fileNameOrUri) {
        this.fileNameOrUri = fileNameOrUri;
    }


    /**
     * @param filePath
     * @return filled configuration object from the input file
     */
    abstract public Configuration read();


    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

}
