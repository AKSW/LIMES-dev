package org.aksw.limes.core.measures.measure.customGraphs.subgraphs;

import com.google.common.collect.Multiset;
import org.aksw.limes.core.measures.measure.customGraphs.description.IDescriptionGraphView;
import org.aksw.limes.core.measures.measure.customGraphs.description.INode;

/**
 * @author Cedric Richter
 */
public class WLSubgraphZeroStore extends WLSubgraphStore {

    public static final String DEFAUL_NODE_LABEL = "_NODE_";

    private boolean ignoreNodeURIs = true;

    public WLSubgraphZeroStore(IDescriptionGraphView view) {
        super(view);
    }

    @Override
    public String map(INode node_uri) {
        if(ignoreNodeURIs && node_uri.getType() == INode.NodeType.URL){
            return DEFAUL_NODE_LABEL;
        }
        return node_uri.getLabel();
    }

}