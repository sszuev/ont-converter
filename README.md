# Ontology Converter v1.0

## A simple command-line utility to convert any rdf graph to OWL2-DL ontology.

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

                                
### Full list of supported formats:
| Name | Provider | Aliases (case insensitive) |
| :------------- | :-------------| :----- |
| TURTLE | Apache Jena | 0, turtle, ttl |
| RDF_XML | Apache Jena | 1, rdf_xml, rdf/xml, rdf |
| RDF_JSON | Apache Jena | 2, rdf_json, rdf/json, rj |
| JSON_LD | Apache Jena | 3, json_ld, json-ld, jsonld |
| NTRIPLES | Apache Jena | 4, ntriples, n-triples, nt |
| NQUADS | Apache Jena | 5, nquads, n-quads, nq |
| TRIG | Apache Jena | 6, trig |
| TRIX | Apache Jena | 7, trix |
| RDF_THRIFT | Apache Jena | 8, rdf_thrift, rdf-thrift, trdf |
| CSV | Apache Jena | 9, csv |
| OWL_XML | OWL-API | 11, owl_xml, owl/xml, owl |
| MANCHESTER_SYNTAX | OWL-API | 12, manchester_syntax, manchestersyntax, omn |
| FUNCTIONAL_SYNTAX | OWL-API | 13, functional_syntax, functionalsyntax, fss |
| BINARY_RDF | OWL-API | 14, binary_rdf, binaryrdf, brf |
| RDFA | OWL-API | 15, rdfa, xhtml |
| OBO | OWL-API | 16, obo |
| KRSS | OWL-API | 17, krss |
| KRSS2 | OWL-API | 18, krss2 |
| DL | OWL-API | 19, dl |
| DL_HTML | OWL-API | 20, dl_html, dl/html, html |
| LATEX | OWL-API | 21, latex, tex |
 
 ### Requirements:
* java1.8
* maven to build (`mvn package`)

 ### Dependencies:
 [ONT-API](https://github.com/avicomp/ont-api) (__version 1.1.0-SNAPSHOT__)