package com.adobe.acs.samples.resourceproviders.impl.solr;


import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ModifyingResourceProvider;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.QueriableResourceProvider;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit.RestAdapter;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component(metatype = true, label = "ACS AEM Samples - Solr Resource Provider",
        description = "Resource Provider that integrates your super duper remote Solr REST API with Sling's resource tree")
@Service
@Properties({
        @Property(name = ResourceProvider.ROOTS,
                value = SolrResourceProvider.ROOT_PATH_DEFAULT),
        @Property(name = ResourceProvider.OWNS_ROOTS,
                boolValue = true,
                propertyPrivate = true),
        @Property(name = QueriableResourceProvider.LANGUAGES,
                value = {SolrResourceProvider.QUERY_LANGUAGE_SOLR},
                propertyPrivate = true),
        @Property(name = SolrResourceProvider.RESOURCE_TYPE,
                value = {SolrResourceProvider.RESOURCE_TYPE_DEFAULT}),
        @Property(name = SolrResourceProvider.SOLR_URL,
                value = {SolrResourceProvider.SOLR_URL_DEFAULT}),
})
public class SolrResourceProvider implements ResourceProvider, QueriableResourceProvider, ModifyingResourceProvider {
    private static final Logger LOG = LoggerFactory.getLogger(SolrResourceProvider.class);

    protected static final String ROOT_PATH_DEFAULT = "/mnt/solr";

    protected static final String RESOURCE_TYPE = "solrResourceProvider.resourceType";
    protected static final String RESOURCE_TYPE_DEFAULT = "com/adobe/acs/samples/solr-resource";

    protected static final String QUERY_LANGUAGE_SOLR = "solrQuery";

    protected static final String SOLR_URL = "solrResourceProvider.solrUrl";
    protected static final String SOLR_URL_DEFAULT = "http://192.168.0.214:8984/solr/procato_products";


    protected String rootPath;
    protected String resourceType;
    protected String solrUrl;
    protected SolrApi solrApi;

    @Activate
    protected void activate(final Map<String, Object> props) {
        modified(props);
    }

    @Modified
    protected void modified(final Map<String, Object> props) {
        LOG.debug("SolrResourceProvider={}", props.toString());

        rootPath = PropertiesUtil.toString(props.get(ResourceProvider.ROOTS), ROOT_PATH_DEFAULT);
        resourceType = PropertiesUtil.toString(props.get(RESOURCE_TYPE), RESOURCE_TYPE_DEFAULT);
        solrUrl = PropertiesUtil.toString(props.get(SOLR_URL), SOLR_URL_DEFAULT);

        // init retrofit adapter
        if (solrApi == null) {
            RestAdapter restAdapter = new RestAdapter.Builder()
                    .setEndpoint(solrUrl)
//                    .setLog(Slf4jLog.INSTANCE)
                    .setLogLevel(RestAdapter.LogLevel.FULL)
                    .build();

            solrApi = restAdapter.create(SolrApi.class);
        }
    }

    /**
     * Check for special cases where the root resource is requested or path contains .json extension or some other
     * selectors and that stuff
     *
     * @param path Input
     * @return <code>True</code>, if the sanity check is passed
     */
    private boolean sanityCheck(String path) {
        return !(StringUtils.isBlank(path) || StringUtils.equals(path, rootPath) ||
                StringUtils.endsWith(path, ".json") || StringUtils.contains(path, ".tidy") ||
                StringUtils.contains(".infinity", path));
    }

    /**
     * Map the input path to the "real" Solr path
     *
     * @param path Example: "/mnt/rest-resources/my-shit"
     * @return Example: "/my-shit"
     */
    private String toSolrPath(String path) {
        return StringUtils.substring(path, rootPath.length());
    }

    /**
     * Map the input path to the "virtual" resource path
     *
     * @param path Example: "/my-shit"
     * @return Example: "/mnt/rest-resources/my-shit"
     */
    private String toResourcePath(String path) {
        return rootPath + (StringUtils.startsWith(path, "/") ? path : "/" + path);
    }

    @Override
    public Resource getResource(ResourceResolver resourceResolver, HttpServletRequest httpServletRequest, String path) {
        return getResource(resourceResolver, path);
    }

    @Override
    public Resource getResource(ResourceResolver resourceResolver, String path) {
        LOG.debug("getResource() ... path={}", path);

        if (sanityCheck(path)) {
            LOG.debug("getting resource for path={}", path);
            String solrPath = toSolrPath(path);

            Map<String, Object> thing = solrApi.get(solrPath);

            return new GenericRestResource(resourceResolver, RESOURCE_TYPE, path, solrPath,
                    new ValueMapDecorator((Map<String, Object>) thing.get("doc")));
        }

        return null;
    }

    @Override
    public Iterator<Resource> findResources(ResourceResolver resourceResolver, String query, String language) {
        LOG.debug("findResources() ... query={}", query);

        if (QUERY_LANGUAGE_SOLR.equals(language)) {

            Map<String, Object> response = solrApi.select(query);
            List<Map<String, Object>> documents = (List<Map<String, Object>>) ((Map<String, Object>) response.get("response")).get("docs");

            List<Resource> resourceList = new ArrayList<Resource>(documents.size());
            for (Map<String, Object> document : documents) {
                String path = (String) document.get("id");

                Resource solrResource = new GenericRestResource(resourceResolver, resourceType, path,
                        toResourcePath(path), new ValueMapDecorator(document));

                resourceList.add(solrResource);
            }

            return resourceList.iterator();
        }

        return null;
    }

    @Override
    public Iterator<ValueMap> queryResources(ResourceResolver resourceResolver, String query, String language) {
        LOG.debug("queryResources() ... not yet implemented ... the cool shit of solr ... if there's love ion this LIVE and it is..");

        return null;
    }


    @Override
    public Iterator<Resource> listChildren(Resource resource) {
        LOG.debug("listChildren() ... not yet implemented ... monday left me broken");

        return null;
    }

    @Override
    public Resource create(ResourceResolver resourceResolver, String path, Map<String, Object> stringObjectMap) throws PersistenceException {
        LOG.debug("create() ... not yet implemented ... tuesday i was thru with hopin");

        return null;
    }

    @Override
    public void delete(ResourceResolver resourceResolver, String path) throws PersistenceException {
        LOG.debug("delete() ... not yet implemented ... wednesday my empty arms were open");

    }

    @Override
    public void revert(ResourceResolver resourceResolver) {
        LOG.debug("revert() ... not yet implemented ... thursday waiting for love, waiting for love");

    }

    @Override
    public void commit(ResourceResolver resourceResolver) throws PersistenceException {
        LOG.debug("commit() ... not yet implemented ... thank the stars it's friday");

    }

    @Override
    public boolean hasChanges(ResourceResolver resourceResolver) {
        LOG.debug("hasChanges() ... not yet implemented ... WE'RE BURNING LIKE A FIRE FONE WILD ON SATURDAY");

        return false;
    }

}
