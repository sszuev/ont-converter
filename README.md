# Ontology Converter v1.0

## A simple command-line utility to convert any RDF graph to OWL2-DL ontology.
This is a kind of [ONT-API](https://github.com/avicomp/ont-api) extension with intentionally straightforward realisation.
Can work both with single ontology file source and with directory containing dependent or independent sources. 
The utility automatically transforms the source RDF Graph to the OWL2 DL syntax according to the internal rules and command-line options. 
For example, if there is no `owl:Ontology` section inside rdf-graph, which is required by OWL, an anonymous ontology header will be generated. 
This is a tool, not a library, and, therefore, it is available only in the form of code and prebuilt jar (see [/releases](https://github.com/sszuev/ont-converter/releases)).

### Usage: `java -jar ont-converter.jar [-f] [-h] -i <path> [-if <format>] -o <path> -of <format> [-p <0|1|2>] [-r] [-s] [-v] [-w]`

### Options:

 * -f,--force                     `Ignore any exceptions while loading/saving
                                and processing imports`
 * -h,--help                      `Print usage.`
 * -i,--input <path>              `The file path or not-empty directory to
                                load ontology/ontologies.
                                See --input-format for list of supported
                                syntaxes.
                                - Required.`
 * -if,--input-format <format>    `The input format. If not specified the
                                program will choose the most suitable one
                                to load ontology from a file.
                                Must be one of the following:
                                TURTLE, RDF_XML, RDF_JSON, JSON_LD,
                                NTRIPLES, NQUADS, TRIG, TRIX, RDF_THRIFT,
                                OWL_XML, MANCHESTER_SYNTAX,
                                FUNCTIONAL_SYNTAX, BINARY_RDF, RDFA, OBO,
                                KRSS
                                - Optional.`
 * -o,--output <path>             `The file or directory path to store result
                                ontology/ontologies.
                                If the --input is a file then this
                                parameter must also be a file.
                                See --output-format for list of supported
                                syntaxes.
                                - Required.`
 * -of,--output-format <format>   `The format of output ontology/ontologies.
                                Must be one of the following:
                                TURTLE, RDF_XML, RDF_JSON, JSON_LD,
                                NTRIPLES, NQUADS, TRIG, TRIX, RDF_THRIFT,
                                OWL_XML, MANCHESTER_SYNTAX,
                                FUNCTIONAL_SYNTAX, OBO, KRSS2, DL,
                                DL_HTML, LATEX
                                - Required.`
 * -p,--punnings <0|1|2>          `The punning mode. Could be used in
                                conjunction with --refine option. Must be
                                one of the following:
                                0 - Lax mode. Default. Allow any punnings,
                                i.e. ontology is allowed to contain
                                multiple entity declarations
                                1 - Middle mode. Two forbidden
                                intersections: Datatype <-> Class &
                                NamedObjectProperty <-> DatatypeProperty
                                2 - Strict mode: All punnings are
                                forbidden, i.e. Datatype <-> Class and
                                rdf:Property intersections (any pairs of
                                NamedObjectProperty, DatatypeProperty,
                                AnnotationProperty).
                                - Optional.`
 * -r,--refine                    `Refine output: if specified the resulting
                                ontologies will consist only of the
                                OWL2-DL components (annotations and
                                axioms), otherwise there could be some
                                rdf-stuff (in case the output format is
                                provided by jena)
                                - Optional.`
 * -s,--spin                      `Use spin transformation to replace
                                rdf:List based spin-constructs (e.g
                                sp:Select) with their text-literal
                                representation to produce compact axioms
                                list.
                                - Optional.`
 * -v,--verbose                   `To print progress messages to console.`
 * -w,--web                       `Allow web/ftp diving to retrieve dependent
                                ontologies from imports (owl:imports),
                                otherwise the specified directory (see
                                --input) will be used as the only source.`

### Example (RDF/XML -> Manchester Syntax):

`$ java -jar ont-converter.jar -i /tmp/pizza.owl.xml -if 1 -o /tmp/pizza.omn -of 12 -v`

where 
* `-i /tmp/pizza.owl.xml` - the path to existing source file, required. In the example above it is [pizza.owl](https://protege.stanford.edu/ontologies/pizza/pizza.owl).
* `-if 1` - the explicit input format (could be also `-if rdf`, `-if rdf/xml` or `-if rdf_xml`), optional.
* `-o /tmp/pizza.omn` - the path to target file, required.
* `-of 12` - the output format (could be also `-of omn`, `-of manchestersyntax`, `-of manchester_syntax`), required.
* `-v` - to print progress info to console, optional.
                                
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
| CSV | Apache Jena | yes / no | 9, csv |
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
 
 ### Requirements:
* java1.8
* [maven](https://maven.apache.org/guides/index.html)

### Build:
Simply run `mvn package` in the project root. All dependencies should be resolved automatically by maven.

### Issues:
In case of any issue (for example the program hangs), please report it into the [/ont-converter/issues](https://github.com/sszuev/ont-converter/issues) page, but only if you really sure that the problem is in the program, not in the underlying API. Otherwise please refer to the [/ont-api/issues](https://github.com/avicomp/ont-api/issues) page.

 ### Dependencies:
 [ONT-API](https://github.com/avicomp/ont-api) (__version 1.4.0-SNAPSHOT__)