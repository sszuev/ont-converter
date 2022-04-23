# Ontology Converter v2.0

## A simple command-line utility to convert any RDF graph to OWL2-DL ontology.

This is a kind of [ONT-API](https://github.com/owlcs/ont-api) extension.
Can work both with single ontology file source and with directory containing dependent or independent ontology document sources.
The utility automatically transforms the source RDF Graph to the OWL2 DL syntax according to the internal rules and command-line options.
For example, if there is no `owl:Ontology` section inside rdf-graph (which is required by OWL), an anonymous ontology header will be generated.
The tool is available in the form of code and prebuilt jar (see [/releases](https://github.com/sszuev/ont-converter/releases)).

### Usage: `java -jar ont-converter.jar [-f] [-h] -i <path> [-if <format>] -o <path> -of <format> [-p <0|1|2>] [-r] [-v] [-w]`

### Options:

* **-f,--force**                     `Ignore any exceptions while loading/saving and processing imports.`
* **-i,--input <path>**              `The file path or not-empty directory to load ontology/ontologies. (always required) { String }`
* **-if,--input-format <format>**    `The input format. If not specified the program will choose the most suitable one to load ontology from a file. { Value should be one of [{0, ttl, TURTLE}, {1, rdf, RDF_XML, RDF/XML}, {2, rj, RDF_JSON, RDF/JSON}, {3, jsonld, JSON_LD, JSON-LD}, {4, nt, NTRIPLES, N-Triples}, {5, nq, NQUADS, N-Quads}, {6, trig}, {7, trix}, {8, trdf, RDF_THRIFT, RDF-THRIFT}, {11, owl, OWL_XML, OWL/XML}, {12, omn, MANCHESTER_SYNTAX, ManchesterSyntax}, {13, fss, FUNCTIONAL_SYNTAX, FunctionalSyntax}, {18, krss2}, {19, dl}] }`
* **-o,--output <path>**             `The file or directory path to store result ontology/ontologies.If the --input is a file then this parameter must also be a file. (always required) { String }`
* **-of,--output-format <format>**   `The format of output ontology/ontologies. (always required) { Value should be one of [{0, ttl, TURTLE}, {1, rdf, RDF_XML, RDF/XML}, {2, rj, RDF_JSON, RDF/JSON}, {3, jsonld, JSON_LD, JSON-LD}, {4, nt, NTRIPLES, N-Triples}, {5, nq, NQUADS, N-Quads}, {6, trig}, {7, trix}, {8, trdf, RDF_THRIFT, RDF-THRIFT}, {11, owl, OWL_XML, OWL/XML}, {12, omn, MANCHESTER_SYNTAX, ManchesterSyntax}, {13, fss, FUNCTIONAL_SYNTAX, FunctionalSyntax}, {18, krss2}, {19, dl}, {20, html, DL_HTML, DL/HTML}, {21, tex, LATEX}] }`
* **-p,--punnings <mode>**           `The punning mode. Could be used in conjunction with --refine option. Must be one of the following:Lax mode: allows any punnings, i.e. ontology is allowed to contain multiple entity declarationsMiddle mode: two forbidden intersections: Datatype <-> Class & NamedObjectProperty <-> DatatypePropertyStrict mode: all punnings are forbidden, i.e. Datatype <-> Class and rdf:Property intersections(any pairs of NamedObjectProperty, DatatypeProperty, AnnotationProperty). { Value should be one of [{0, STRICT}, {1, MEDIUM}, {2, LAX}] }`
* **-r,--refine**                    `Refine output: if specified the resulting ontologies will consist only of the OWL2-DL components (annotations and axioms), otherwise there could be some rdf-stuff (in case the output format is provided by jena)`
* **-v,--verbose**                   `To print progress messages to console.`
* **-w,--web**                       `Allow web/ftp diving to retrieve dependent ontologies from imports (owl:imports),otherwise the specified directory (see --input) will be used as the only source.`

### Example (RDF/XML -> Manchester Syntax):

`$ java -jar ont-converter.jar -i /tmp/pizza.owl.xml -if 1 -o /tmp/pizza.omn -of 12 -v`

where
* `-i /tmp/pizza.owl.xml` - the path to existing source file, required. In the example above it is [pizza.owl](https://protege.stanford.edu/ontologies/pizza/pizza.owl).
* `-if 1` - the explicit input format (could be also `-if rdf`, `-if rdf/xml` or `-if rdf_xml`), optional.
* `-o /tmp/pizza.omn` - the path to target file, required.
* `-of 12` - the output format (could be also `-of omn`, `-of manchestersyntax`, `-of manchester_syntax`), required.
* `-v` - to print progress info to console, optional.

### Load API
Some facilities from the tool internals can be used as a library to simplify bulk loading. 
This can be helpful to properly handle all `owl:imports` dependencies. See examples:
```java
List<OntologyMap> maps = LoadersKt.loadDirectory(Path.of("/path-to-dir-with-ontologies"), null, false, ManagersKt::createSoftManager);
maps.forEach(map -> map.getIds().forEach((iri, id) -> System.out.println("document-iri = " + iri + " => id=" + id)));
maps.forEach(map -> map.getGraphs().forEach((iri, g) -> System.out.println("document-iri = " + iri + " => triples=" + g.size())));
maps.forEach(map -> map.sources().iterator().forEachRemaining(s -> System.out.println("document-source = " + s)));

OntologyMap map = LoadersKt.loadFile(Path.of("/path-to-file-ttl"), OntFormat.TURTLE, false, ManagersKt.createSoftManager());
System.out.println(map);
```
### Requirements:
* **java-11+** tp tun
* **[gradle-7+](https://gradle.org/)** to build

### Build:
- To build use `gradle clean build` running in the project root.
- To install into local maven repository (`.m2`) use `gradle publishToMavenLocal`
- To run from the project root use command `java -jar build/libs/ont-converter.jar`

### Issues:
Please use [/ont-converter/issues](https://github.com/sszuev/ont-converter/issues) page

### Dependencies:
[ONT-API](https://github.com/owlcs/ont-api) (__version 3.0.0__)

### Full list of supported formats:
| Name | Provider | Read / Write | Aliases (case insensitive) |
| :-------------  | :------------- | :-------------| :----- |
| TURTLE | Apache Jena | yes / yes | 0, turtle, ttl |
| RDF_XML | Apache Jena | yes / yes | 1, rdf_xml, rdf/xml, rdf |
| RDF_JSON | Apache Jena | yes / yes | 2, rdf_json, rdf/json, rj |
| JSON_LD | Apache Jena | yes / yes | 3, json_ld, json-ld, jsonld |
| NTRIPLES | Apache Jena | yes / yes | 4, ntriples, n-triples, nt |
| NQUADS | Apache Jena | yes / yes | 5, nquads, n-quads, nq |
| TRIG | Apache Jena | yes / yes | 6, trig |
| TRIX | Apache Jena | yes / yes | 7, trix |
| RDF_THRIFT | Apache Jena | yes / yes | 8, rdf_thrift, rdf-thrift, trdf |
| OWL_XML | OWL-API | yes / yes | 11, owl_xml, owl/xml, owl |
| MANCHESTER_SYNTAX | OWL-API | yes / yes | 12, manchester_syntax, manchestersyntax, omn |
| FUNCTIONAL_SYNTAX | OWL-API | yes / yes | 13, functional_syntax, functionalsyntax, fss |
| BINARY_RDF | OWL-API | yes / yes | 14, binary_rdf, binaryrdf, brf |
| RDFA | OWL-API | yes / no | 15, rdfa, xhtml |
| OBO | OWL-API | yes / yes | 16, obo |
| KRSS2 | OWL-API | yes / yes | 18, krss2 |
| DL | OWL-API | yes / yes | 19, dl |
| DL_HTML | OWL-API | no / yes | 20, dl_html, dl/html, html |
| LATEX | OWL-API | no / yes | 21, latex, tex |
