package com.github.sszuev.ontapi;

import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.riot.system.stream.LocationMapper;
import org.apache.jena.riot.system.stream.Locator;
import org.apache.jena.riot.system.stream.StreamManager;
import org.semanticweb.owlapi.model.IRI;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Created by @szuev on 12.01.2018.
 */
public class NoWebStreamManager extends StreamManager {

    private final IRIMap map;
    private final StreamManager delegate;

    public NoWebStreamManager(IRIMap map, StreamManager delegate) {
        this.map = map;
        this.delegate = delegate;
    }

    public NoWebStreamManager(IRIMap map) {
        this(map, StreamManager.makeDefaultStreamManager());
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public StreamManager clone() {
        return new NoWebStreamManager(this.map, delegate.clone());
    }

    @Override
    public TypedInputStream open(String filenameOrURI) {
        return delegate.open(filenameOrURI);
    }

    @Override
    public String mapURI(String url) {
        Optional<String> res = map.documentIRI(IRI.create(Objects.requireNonNull(url, "Null uri"))).map(IRI::toString);
        if (res.isPresent()) {
            return res.get();
        }
        if (url.startsWith("http://") || url.startsWith("https://")) {
            throw new UnsupportedOperationException("Web access is prohibited.");
        }
        return delegate.mapURI(url);
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
