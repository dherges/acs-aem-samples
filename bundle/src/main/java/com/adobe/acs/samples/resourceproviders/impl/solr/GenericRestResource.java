package com.adobe.acs.samples.resourceproviders.impl.solr;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.resource.ValueMap;


/**
 * A Resource that was obtained from your super duper remote REST API
 */
public class GenericRestResource extends SyntheticResource {

    protected ValueMap myValueMap;
    protected String remotePath;

    public GenericRestResource(ResourceResolver resourceResolver, String resourceType, String slingPath,
                               String remotePath, ValueMap valueMap) {
        super(resourceResolver, slingPath, resourceType);

        this.remotePath = remotePath;
        this.myValueMap = valueMap;
    }

    /** @return Sling resource path */
    public String getSlingPath() {
        return super.getPath();
    }

    /** @return Resource path used by remote REST API */
    public String getRemotePath() {
        return super.getPath();
    }

    @Override
    public ValueMap getValueMap() {
        // TODO resourceType is not present in that value map .. issue?

        return myValueMap;
    }

    @Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        if (type.isAssignableFrom(ValueMap.class)) {
            return (AdapterType) getValueMap();
        }

        return super.adaptTo(type);
    }

}
