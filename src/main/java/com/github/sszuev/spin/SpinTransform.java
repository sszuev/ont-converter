package com.github.sszuev.spin;

import com.github.sszuev.ontapi.OWLStreamManager;
import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.*;
import org.topbraid.spin.model.*;
import ru.avicomp.ontapi.config.OntConfig;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.utils.Graphs;
import ru.avicomp.ontapi.jena.utils.Models;
import ru.avicomp.ontapi.jena.vocabulary.RDF;
import ru.avicomp.ontapi.transforms.Transform;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Copy-paste from ONT-API tests: see ru.avicomp.ontapi.utils.SpinTransform.
 * To replace spin queries with its string representations (it is alternative way to describe spin-sparql-query).
 * By default a spin query is represented in the bulky form which consists of several rdf:List ([]-List).
 * The short (string, sp:text) form allows to present the query as an axiom also.
 * <p>
 * Example of a query:
 * <pre> {@code
 * spin:body [
 *    rdf:type sp:Select ;
 *    sp:resultVariables (
 *        [
 *          sp:expression [
 *              rdf:type sp:Count ;
 *              sp:expression [
 *                  sp:varName \"subject\"^^xsd:string ;
 *                ] ;
 *            ] ;
 *          sp:varName \"result\"^^xsd:string ;
 *        ]
 *      ) ;
 *    sp:where (
 *        [
 *          sp:object spin:_arg2 ;
 *          sp:predicate spin:_arg1 ;
 *          sp:subject [
 *              sp:varName \"subject\"^^xsd:string ;
 *            ] ;
 *        ]
 *      ) ;
 *  ] ;
 * } </pre>
 * And it will be replaced with:
 * <pre> {@code
 * spin:body [ a        sp:Select ;
 *             sp:text  "SELECT ((COUNT(?subject)) AS ?result)\nWHERE {\n    ?subject spin:_arg1 spin:_arg2 .\n}"
 *           ] ;
 * }</pre>
 * <p>
 * <p>
 * <p>
 * Note: spin-api would try to go to the internet if there are no ontology imports mappings in the jena-system.
 * Please configure it before.
 *
 * @see OWLStreamManager
 * @see OntConfig#disableWebAccess()
 * @see <a href='https://github.com/owlcs/ont-api/blob/2.x.x/src/test/java/com/github/owlcs/ontapi/utils/SpinTransform.java'>com.github.owlcs.ontapi.utils.SpinTransform</a>
 * <p>
 * Created by @szuev on 10.01.2018.
 */
public class SpinTransform extends Transform {

    public SpinTransform(Graph graph) {
        super(graph);
    }

    @Override
    public void perform() {
        List<Query> queries = queries().collect(Collectors.toList());
        String name = Graphs.getName(getQueryModel().getGraph());
        if (!queries.isEmpty() && LOGGER.isDebugEnabled()) {
            LOGGER.debug("[{}] queries count: {}", name, queries.size());
        }
        queries.forEach(query -> {
            Literal literal = ResourceFactory.createTypedLiteral(String.valueOf(query));
            Resource type = statements(query, RDF.type, null)
                    .map(Statement::getObject)
                    .filter(RDFNode::isURIResource)
                    .map(RDFNode::asResource)
                    .findFirst().orElseThrow(() -> new OntJenaException("No type for " + literal));
            Set<Statement> remove = Models.getAssociatedStatements(query);
            remove.stream()
                    .filter(s -> !(RDF.type.equals(s.getPredicate()) && type.equals(s.getObject())))
                    .forEach(statement -> getWorkModel().remove(statement));
            getWorkModel().add(query, SP.text, literal);
        });
    }

    @Override
    public boolean test() {
        return Stream.concat(getGraph().getPrefixMapping().getNsPrefixMap().values().stream(),
                Graphs.getImports(getGraph()).stream()).anyMatch(u -> u.startsWith(SP.SPIN_URI));
    }

    @Override
    public Model createModel(Graph graph) {
        return SP.createModel(graph);
    }

    public Stream<Query> queries() {
        return Stream.of(QueryType.values()).flatMap(this::queries);
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

