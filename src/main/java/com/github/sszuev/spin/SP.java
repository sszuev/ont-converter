package com.github.sszuev.spin;

import org.apache.jena.enhanced.BuiltinPersonalities;
import org.apache.jena.enhanced.Personality;
import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.ModelCom;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.topbraid.spin.model.*;
import org.topbraid.spin.model.impl.*;
import org.topbraid.spin.model.update.*;
import org.topbraid.spin.model.update.impl.*;
import org.topbraid.spin.util.SimpleImplementation;
import org.topbraid.spin.util.SimpleImplementation2;
import org.topbraid.spin.vocabulary.SPIN;
import org.topbraid.spin.vocabulary.SPL;

/**
 * Copy-paste from {@link org.topbraid.spin.vocabulary.SP}.
 * The difference: it does not modifies standard global jena personalities.
 *
 * Created by @szuev on 10.01.2018.
 */
@SuppressWarnings("WeakerAccess")
public class SP {
    public static final String SPIN_URI = "http://spinrdf.org";
    public static final String BASE_URI = SPIN_URI + "/sp";
    public static final String NS = BASE_URI + "#";
    public static final String PREFIX = "sp";

    public static final Resource Ask = ResourceFactory.createResource("http://spinrdf.org/sp#Ask");
    public static final Resource Bind = ResourceFactory.createResource("http://spinrdf.org/sp#Bind");
    public static final Resource Clear = ResourceFactory.createResource("http://spinrdf.org/sp#Clear");
    public static final Resource Construct = ResourceFactory.createResource("http://spinrdf.org/sp#Construct");
    public static final Resource Create = ResourceFactory.createResource("http://spinrdf.org/sp#Create");
    public static final Resource Delete = ResourceFactory.createResource("http://spinrdf.org/sp#Delete");
    public static final Resource DeleteData = ResourceFactory.createResource("http://spinrdf.org/sp#DeleteData");
    public static final Resource DeleteWhere = ResourceFactory.createResource("http://spinrdf.org/sp#DeleteWhere");
    public static final Resource Describe = ResourceFactory.createResource("http://spinrdf.org/sp#Describe");
    public static final Resource Drop = ResourceFactory.createResource("http://spinrdf.org/sp#Drop");
    public static final Resource Exists = ResourceFactory.createResource("http://spinrdf.org/sp#Exists");
    public static final Resource Filter = ResourceFactory.createResource("http://spinrdf.org/sp#Filter");

    public static final Resource Insert = ResourceFactory.createResource("http://spinrdf.org/sp#Insert");
    public static final Resource InsertData = ResourceFactory.createResource("http://spinrdf.org/sp#InsertData");

    public static final Resource Let = ResourceFactory.createResource("http://spinrdf.org/sp#Let");
    public static final Resource Load = ResourceFactory.createResource("http://spinrdf.org/sp#Load");
    public static final Resource Modify = ResourceFactory.createResource("http://spinrdf.org/sp#Modify");
    public static final Resource Minus = ResourceFactory.createResource("http://spinrdf.org/sp#Minus");
    public static final Resource NamedGraph = ResourceFactory.createResource("http://spinrdf.org/sp#NamedGraph");
    public static final Resource NotExists = ResourceFactory.createResource("http://spinrdf.org/sp#NotExists");
    public static final Resource Optional = ResourceFactory.createResource("http://spinrdf.org/sp#Optional");
    public static final Resource Select = ResourceFactory.createResource("http://spinrdf.org/sp#Select");
    public static final Resource Service = ResourceFactory.createResource("http://spinrdf.org/sp#Service");
    public static final Resource SubQuery = ResourceFactory.createResource("http://spinrdf.org/sp#SubQuery");
    public static final Resource TriplePath = ResourceFactory.createResource("http://spinrdf.org/sp#TriplePath");
    public static final Resource TriplePattern = ResourceFactory.createResource("http://spinrdf.org/sp#TriplePattern");
    public static final Resource TripleTemplate = ResourceFactory.createResource("http://spinrdf.org/sp#TripleTemplate");
    public static final Resource Union = ResourceFactory.createResource("http://spinrdf.org/sp#Union");
    public static final Resource Values = ResourceFactory.createResource("http://spinrdf.org/sp#Values");
    public static final Resource Variable = ResourceFactory.createResource("http://spinrdf.org/sp#Variable");
    public static final Property text = ResourceFactory.createProperty("http://spinrdf.org/sp#text");

    public static Personality<RDFNode> SPIN_PERSONALITY = init(BuiltinPersonalities.model.copy());

    public static Model createModel(Graph graph) {
        return new ModelCom(graph, SPIN_PERSONALITY);
    }

    public static Personality<RDFNode> init(Personality<RDFNode> p) {
        p.add(org.topbraid.spin.model.Aggregation.class, new SimpleImplementation(SPL.Argument.asNode(), AggregationImpl.class));
        p.add(Argument.class, new SimpleImplementation(SPL.Argument.asNode(), ArgumentImpl.class));
        p.add(Attribute.class, new SimpleImplementation(SPL.Attribute.asNode(), AttributeImpl.class));
        p.add(org.topbraid.spin.model.Ask.class, new SimpleImplementation(Ask.asNode(), AskImpl.class));
        p.add(org.topbraid.spin.model.Bind.class, new SimpleImplementation2(Bind.asNode(), Let.asNode(), BindImpl.class));
        p.add(org.topbraid.spin.model.update.Clear.class, new SimpleImplementation(Clear.asNode(), ClearImpl.class));
        p.add(Construct.class, new SimpleImplementation(Construct.asNode(), ConstructImpl.class));
        p.add(org.topbraid.spin.model.update.Create.class, new SimpleImplementation(Create.asNode(), CreateImpl.class));
        p.add(org.topbraid.spin.model.update.Delete.class, new SimpleImplementation(Delete.asNode(), DeleteImpl.class));
        p.add(org.topbraid.spin.model.update.DeleteData.class, new SimpleImplementation(DeleteData.asNode(), DeleteDataImpl.class));
        p.add(org.topbraid.spin.model.update.DeleteWhere.class, new SimpleImplementation(DeleteWhere.asNode(), DeleteWhereImpl.class));
        p.add(Describe.class, new SimpleImplementation(Describe.asNode(), DescribeImpl.class));
        p.add(Drop.class, new SimpleImplementation(Drop.asNode(), DropImpl.class));
        p.add(ElementList.class, new SimpleImplementation(RDF.List.asNode(), ElementListImpl.class));
        p.add(Exists.class, new SimpleImplementation(Exists.asNode(), ExistsImpl.class));
        p.add(Function.class, new SimpleImplementation(SPIN.Function.asNode(), FunctionImpl.class));
        p.add(FunctionCall.class, new SimpleImplementation(SPIN.Function.asNode(), FunctionCallImpl.class));
        p.add(Filter.class, new SimpleImplementation(Filter.asNode(), FilterImpl.class));
        p.add(Insert.class, new SimpleImplementation(Insert.asNode(), InsertImpl.class));
        p.add(InsertData.class, new SimpleImplementation(InsertData.asNode(), InsertDataImpl.class));
        p.add(Load.class, new SimpleImplementation(Load.asNode(), LoadImpl.class));
        p.add(Minus.class, new SimpleImplementation(Minus.asNode(), MinusImpl.class));
        p.add(Modify.class, new SimpleImplementation(Modify.asNode(), ModifyImpl.class));
        p.add(Module.class, new SimpleImplementation(SPIN.Module.asNode(), ModuleImpl.class));
        p.add(NamedGraph.class, new SimpleImplementation(NamedGraph.asNode(), NamedGraphImpl.class));
        p.add(NotExists.class, new SimpleImplementation(NotExists.asNode(), NotExistsImpl.class));
        p.add(Optional.class, new SimpleImplementation(Optional.asNode(), OptionalImpl.class));
        p.add(Service.class, new SimpleImplementation(Service.asNode(), ServiceImpl.class));
        p.add(Select.class, new SimpleImplementation(Select.asNode(), SelectImpl.class));
        p.add(SubQuery.class, new SimpleImplementation(SubQuery.asNode(), SubQueryImpl.class));
        p.add(SPINInstance.class, new SimpleImplementation(RDFS.Resource.asNode(), SPINInstanceImpl.class));
        p.add(Template.class, new SimpleImplementation(SPIN.Template.asNode(), TemplateImpl.class));
        p.add(TemplateCall.class, new SimpleImplementation(RDFS.Resource.asNode(), TemplateCallImpl.class));
        p.add(TriplePath.class, new SimpleImplementation(TriplePath.asNode(), TriplePathImpl.class));
        p.add(TriplePattern.class, new SimpleImplementation(TriplePattern.asNode(), TriplePatternImpl.class));
        p.add(TripleTemplate.class, new SimpleImplementation(TripleTemplate.asNode(), TripleTemplateImpl.class));
        p.add(Union.class, new SimpleImplementation(Union.asNode(), UnionImpl.class));
        p.add(Values.class, new SimpleImplementation(Values.asNode(), ValuesImpl.class));
        p.add(Variable.class, new SimpleImplementation(Variable.asNode(), VariableImpl.class));
        return p;
    }


}
