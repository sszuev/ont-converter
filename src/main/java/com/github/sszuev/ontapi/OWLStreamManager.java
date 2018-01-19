package com.github.sszuev.ontapi;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.riot.system.stream.LocationMapper;
import org.apache.jena.riot.system.stream.Locator;
import org.apache.jena.riot.system.stream.StreamManager;
import org.semanticweb.owlapi.model.IRI;

import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.config.OntConfig;

/**
 * Jena API StreamManager with restriction on web-access.
 *
 * Created by @szuev on 12.01.2018.
 */
@SuppressWarnings("WeakerAccess")
public class OWLStreamManager extends StreamManager {

    private final IRIMap map;
    private final StreamManager delegate;
    private final List<OntConfig.Scheme> schemes;

    /**
     * Main constructor
     *
     * @param map      {@link IRIMap}
     * @param schemes  List of allowed {@link OntConfig.Scheme}s
     * @param delegate {@link StreamManager}
     */
    public OWLStreamManager(IRIMap map, List<OntConfig.Scheme> schemes, StreamManager delegate) {
        this.map = map;
        this.delegate = delegate;
        this.schemes = schemes;
    }

    public OWLStreamManager(IRIMap map, List<OntConfig.Scheme> schemes) {
        this(map, schemes, StreamManager.makeDefaultStreamManager());
    }

    public OWLStreamManager(List<OntConfig.Scheme> schemes) {
        this(new IRIMap(), schemes);
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public StreamManager clone() {
        return new OWLStreamManager(this.map, this.schemes, delegate.clone());
    }

    @Override
    public TypedInputStream open(String filenameOrURI) {
        return delegate.open(filenameOrURI);
    }

    /**
     *
     * @param url String, the ontology IRI
     * @return String, the document IRI
     * @throws OntApiException in case the url-scheme is not allowed
     */
    @Override
    public String mapURI(String url) throws OntApiException {
        IRI iri = IRI.create(Objects.requireNonNull(url, "Null url specified."));
        Optional<String> doc = map.documentIRI(iri).map(IRI::toString);
        if (doc.isPresent()) {
            return doc.get();
        }
        if (iri.getScheme() == null || schemes.stream().anyMatch(s -> s.same(iri))) {
            return delegate.mapURI(url);
        }
        throw new OntApiException("Not allowed scheme: " + iri);
    }

    @Override
    public TypedInputStream openNoMap(String filenameOrURI) {
        return delegate.openNoMap(filenameOrURI);
    }

    @Override
    public TypedInputStream openNoMapOrNull(String filenameOrURI) {
        return delegate.openNoMapOrNull(filenameOrURI);
    }

    @Override
    public void setLocationMapper(LocationMapper _mapper) {
        throw new UnsupportedOperationException();
    }

    @Override
    public LocationMapper getLocationMapper() {
        return delegate.getLocationMapper();
    }

    @Override
    public List<Locator> locators() {
        return delegate.locators();
    }

    @Override
    public void remove(Locator loc) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clearLocators() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addLocator(Locator loc) {
        throw new UnsupportedOperationException();
    }
}
