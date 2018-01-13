package com.github.sszuev.spin;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.*;
import org.topbraid.spin.model.*;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.utils.Graphs;
import ru.avicomp.ontapi.jena.utils.Models;
import ru.avicomp.ontapi.jena.vocabulary.RDF;
import ru.avicomp.ontapi.transforms.Transform;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Copy-paste from ONT-API tests: see ru.avicomp.ontapi.utils.SpinTransform.
 * <p>
 * Note: spin-api would try to go to the internet if there are no ontology imports mappings in the jena-system.
 * Please configure it before.
 * @see com.github.sszuev.ontapi.NoWebStreamManager
 * <p>
 * Created by @szuev on 10.01.2018.
 */
public class SpinTransform extends Transform {

    private Model model;
    private Model base;

    public SpinTransform(Graph graph) {
        super(graph);
    }

    @Override
    public void perform() {
        List<Query> queries = queries().collect(Collectors.toList());
        String name = Graphs.getName(getBaseGraph());
        if (!queries.isEmpty() && LOGGER.isDebugEnabled()) {
            LOGGER.debug("[{}] queries count: {}", name, queries.size());
        }
        queries.forEach(query -> {
            Literal literal = ResourceFactory.createTypedLiteral(String.valueOf(query));
            Resource type = statements(query, RDF.type, null)
                    .map(Statement::getObject)
                    .filter(RDFNode::isURIResource)
                    .map(RDFNode::asResource)
                    .findFirst().orElseThrow(OntJenaException.supplier("No type for " + literal));
            Set<Statement> remove = Models.getAssociatedStatements(query);
            remove.stream()
                    .filter(s -> !(RDF.type.equals(s.getPredicate()) && type.equals(s.getObject())))
                    .forEach(statement -> getBaseModel().remove(statement));
            getBaseModel().add(query, SP.text, literal);
        });
    }

    @Override
    public Model getModel() {
        return this.model == null ? (this.model = SP.createModel(this.getGraph())) : this.model;
    }

    @Override
    public Model getBaseModel() {
        return this.base == null ? (this.base = SP.createModel(this.getBaseGraph())) : this.base;
    }

    public Stream<Query> queries() {
        return Stream.of(QueryType.values()).map(this::queries).flatMap(Function.identity());
    }

    protected Stream<Query> queries(QueryType type) {
        return statements(null, RDF.type, type.getType()).map(Statement::getSubject)
                .filter(s -> s.canAs(type.getView())).map(s -> s.as(type.getView()));
    }

    public enum QueryType {
        SELECT(SP.Select, Select.class),
        CONSTRUCT(SP.Construct, Construct.class),
        ASK(SP.Ask, Ask.class),
        DESCRIBE(SP.Describe, Describe.class);

        private final Resource type;
        private final Class<? extends Query> view;

        QueryType(Resource type, Class<? extends Query> view) {
            this.type = type;
            this.view = view;
        }

        public Resource getType() {
            return type;
        }

        public Class<? extends Query> getView() {
            return view;
        }
    }
}

